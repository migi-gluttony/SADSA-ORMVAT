package ormvat.sadsa.service.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowService {

    private final EtapeRepository etapeRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final HistoriqueWorkflowRepository historiqueWorkflowRepository;
    private final JoursFerieRepository joursFerieRepository;

    /**
     * Initialize workflow for new dossier - starts at AP Phase Antenne
     * ✅ FIXED: Better logging and validation
     */
    @Transactional
    public WorkflowInstance initializeWorkflow(Dossier dossier, Utilisateur utilisateur) {
        log.info("Initializing workflow for dossier ID: {} with reference: {}", 
                 dossier.getId(), dossier.getReference());
        
        // ✅ FIXED: Check if workflow already exists for this dossier
        List<WorkflowInstance> existingWorkflows = workflowInstanceRepository.findByDossierId(dossier.getId());
        if (!existingWorkflows.isEmpty()) {
            log.warn("Workflow already exists for dossier ID: {}. Returning existing workflow.", dossier.getId());
            return existingWorkflows.get(0);
        }
        
        // Get the first etape: AP - Phase Antenne
        Etape firstEtape = etapeRepository.findByDesignation("AP - Phase Antenne");
        if (firstEtape == null) {
            throw new RuntimeException("Etape 'AP - Phase Antenne' non trouvée");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateLimite = calculateDeadline(now, firstEtape.getDureeJours());
        
        log.info("Creating workflow for dossier ID: {} starting at: {} with deadline: {}", 
                 dossier.getId(), now, dateLimite);

        WorkflowInstance workflow = new WorkflowInstance();
        workflow.setDossier(dossier);
        workflow.setEtape(firstEtape);
        workflow.setEtapeDesignation(firstEtape.getDesignation());
        workflow.setEmplacementActuel(getEmplacementForEtape(firstEtape));
        workflow.setDateEntree(now);
        workflow.setDateLimite(dateLimite);
        workflow.setJoursRestants(calculateRemainingDays(dateLimite));
        workflow.setEnRetard(calculateRemainingDays(dateLimite) < 0);

        workflow = workflowInstanceRepository.save(workflow);
        
        log.info("Workflow created successfully for dossier ID: {} with workflow ID: {}", 
                 dossier.getId(), workflow.getId());

        // Create history entry
        createHistoryEntry(dossier, firstEtape, now, null, utilisateur, "Initialisation du workflow");

        return workflow;
    }

    /**
     * Move to next etape in sequence
     */
    @Transactional
    public WorkflowInstance moveToNextEtape(Dossier dossier, Utilisateur utilisateur, String commentaire) {
        WorkflowInstance currentWorkflow = getCurrentWorkflowInstance(dossier);
        Etape currentEtape = currentWorkflow.getEtape();
        
        log.info("Moving dossier ID: {} from etape: {} to next etape", 
                 dossier.getId(), currentEtape.getDesignation());
        
        // Find next etape
        Etape nextEtape = getNextEtape(currentEtape);
        if (nextEtape == null) {
            throw new RuntimeException("Aucune étape suivante trouvée après " + currentEtape.getDesignation());
        }

        return transitionToEtape(dossier, nextEtape, utilisateur, commentaire);
    }

    /**
     * Return to previous etape
     */
    @Transactional
    public WorkflowInstance returnToPreviousEtape(Dossier dossier, Utilisateur utilisateur, String commentaire) {
        WorkflowInstance currentWorkflow = getCurrentWorkflowInstance(dossier);
        Etape currentEtape = currentWorkflow.getEtape();
        
        // Find previous etape
        Etape previousEtape = getPreviousEtape(currentEtape);
        if (previousEtape == null) {
            throw new RuntimeException("Aucune étape précédente trouvée avant " + currentEtape.getDesignation());
        }

        return transitionToEtape(dossier, previousEtape, utilisateur, commentaire);
    }

    /**
     * Move to specific etape (for special cases)
     */
    @Transactional
    public WorkflowInstance moveToEtape(Dossier dossier, String etapeDesignation, Utilisateur utilisateur, String commentaire) {
        Etape targetEtape = etapeRepository.findByDesignation(etapeDesignation);
        if (targetEtape == null) {
            throw new RuntimeException("Etape '" + etapeDesignation + "' non trouvée");
        }

        return transitionToEtape(dossier, targetEtape, utilisateur, commentaire);
    }

    /**
     * Get current workflow instance for dossier
     * ✅ FIXED: Better error handling and logging
     */
    public WorkflowInstance getCurrentWorkflowInstance(Dossier dossier) {
        log.debug("Getting current workflow for dossier ID: {}", dossier.getId());
        
        List<WorkflowInstance> workflows = workflowInstanceRepository.findByDossierId(dossier.getId());
        if (workflows.isEmpty()) {
            log.error("No workflow found for dossier ID: {} with reference: {}", 
                     dossier.getId(), dossier.getReference());
            throw new RuntimeException("Aucun workflow trouvé pour le dossier " + dossier.getId() + 
                                     " (Référence: " + dossier.getReference() + ")");
        }
        
        // ✅ FIXED: Get the most recent workflow (in case there are multiple)
        WorkflowInstance latestWorkflow = workflows.stream()
                .max((w1, w2) -> w1.getDateEntree().compareTo(w2.getDateEntree()))
                .orElse(workflows.get(0));
                
        log.debug("Found workflow ID: {} for dossier ID: {} at etape: {}", 
                 latestWorkflow.getId(), dossier.getId(), latestWorkflow.getEtapeDesignation());
                 
        return latestWorkflow;
    }

    /**
     * Get current etape info with calculated remaining time
     * ✅ FIXED: Recalculate remaining days on each call
     */
    public EtapeInfo getCurrentEtapeInfo(Dossier dossier) {
        WorkflowInstance workflow = getCurrentWorkflowInstance(dossier);
        Etape etape = workflow.getEtape();
        
        // ✅ FIXED: Always recalculate remaining days based on current time
        int joursRestants = calculateRemainingDays(workflow.getDateLimite());
        boolean enRetard = joursRestants < 0;
        
        // ✅ FIXED: Update workflow instance with current calculations
        workflow.setJoursRestants(Math.abs(joursRestants)); // Store absolute value
        workflow.setEnRetard(enRetard);
        workflowInstanceRepository.save(workflow);

        log.debug("Etape info for dossier ID: {} - Etape: {}, Jours restants: {}, En retard: {}", 
                 dossier.getId(), etape.getDesignation(), joursRestants, enRetard);

        return EtapeInfo.builder()
                .etape(etape)
                .designation(etape.getDesignation())
                .phase(etape.getPhase())
                .dureeJours(etape.getDureeJours())
                .ordre(etape.getOrdre())
                .emplacementActuel(workflow.getEmplacementActuel())
                .dateEntree(workflow.getDateEntree())
                .dateLimite(workflow.getDateLimite())
                .joursRestants(Math.max(0, joursRestants)) // Return 0 if overdue
                .joursDeRetard(Math.max(0, -joursRestants)) // Positive number of overdue days
                .enRetard(enRetard)
                .build();
    }

    /**
     * Check if user can act on current etape
     */
    public boolean canUserActOnCurrentEtape(Dossier dossier, Utilisateur utilisateur) {
        WorkflowInstance workflow = getCurrentWorkflowInstance(dossier);
        Etape currentEtape = workflow.getEtape();
        
        return canUserActOnEtape(currentEtape, utilisateur);
    }

    /**
     * Get workflow history for dossier
     */
    public List<HistoriqueWorkflow> getWorkflowHistory(Dossier dossier) {
        return historiqueWorkflowRepository.findByDossierId(dossier.getId());
    }

    // Private helper methods

    private WorkflowInstance transitionToEtape(Dossier dossier, Etape targetEtape, Utilisateur utilisateur, String commentaire) {
        WorkflowInstance currentWorkflow = getCurrentWorkflowInstance(dossier);
        LocalDateTime now = LocalDateTime.now();
        
        log.info("Transitioning dossier ID: {} from etape: {} to etape: {}", 
                 dossier.getId(), currentWorkflow.getEtapeDesignation(), targetEtape.getDesignation());

        // Close current workflow history
        closeCurrentHistoryEntry(dossier, currentWorkflow, now, utilisateur, commentaire);

        // Update workflow instance
        currentWorkflow.setEtape(targetEtape);
        currentWorkflow.setEtapeDesignation(targetEtape.getDesignation());
        currentWorkflow.setEmplacementActuel(getEmplacementForEtape(targetEtape));
        currentWorkflow.setDateEntree(now);
        
        LocalDateTime newDeadline = calculateDeadline(now, targetEtape.getDureeJours());
        currentWorkflow.setDateLimite(newDeadline);
        currentWorkflow.setJoursRestants(calculateRemainingDays(newDeadline));
        currentWorkflow.setEnRetard(false); // Reset delay status for new etape

        workflowInstanceRepository.save(currentWorkflow);

        // Create new history entry
        createHistoryEntry(dossier, targetEtape, now, null, utilisateur, commentaire);

        log.info("Dossier ID: {} transitioned successfully to etape: {} with new deadline: {}", 
                 dossier.getId(), targetEtape.getDesignation(), newDeadline);
        return currentWorkflow;
    }

    private Etape getNextEtape(Etape currentEtape) {
        String currentPhase = currentEtape.getPhase();
        int currentOrdre = currentEtape.getOrdre();
        
        // Try to find next etape in same phase
        List<Etape> etapesInPhase = etapeRepository.findAll().stream()
                .filter(e -> e.getPhase().equals(currentPhase))
                .filter(e -> e.getOrdre() == currentOrdre + 1)
                .collect(Collectors.toList());
        
        if (!etapesInPhase.isEmpty()) {
            return etapesInPhase.get(0);
        }
        
        // If at end of AP phase, move to first RP etape
        if ("AP".equals(currentPhase) && currentOrdre == 4) {
            return etapeRepository.findAll().stream()
                    .filter(e -> "RP".equals(e.getPhase()))
                    .filter(e -> e.getOrdre() == 1)
                    .findFirst()
                    .orElse(null);
        }
        
        return null;
    }

    private Etape getPreviousEtape(Etape currentEtape) {
        String currentPhase = currentEtape.getPhase();
        int currentOrdre = currentEtape.getOrdre();
        
        // Try to find previous etape in same phase
        List<Etape> etapesInPhase = etapeRepository.findAll().stream()
                .filter(e -> e.getPhase().equals(currentPhase))
                .filter(e -> e.getOrdre() == currentOrdre - 1)
                .collect(Collectors.toList());
        
        if (!etapesInPhase.isEmpty()) {
            return etapesInPhase.get(0);
        }
        
        // If at beginning of RP phase, can go back to last AP etape
        if ("RP".equals(currentPhase) && currentOrdre == 1) {
            return etapeRepository.findAll().stream()
                    .filter(e -> "AP".equals(e.getPhase()))
                    .filter(e -> e.getOrdre() == 4)
                    .findFirst()
                    .orElse(null);
        }
        
        return null;
    }

    private WorkflowInstance.EmplacementType getEmplacementForEtape(Etape etape) {
        String designation = etape.getDesignation().toLowerCase();
        
        if (designation.contains("antenne")) {
            return WorkflowInstance.EmplacementType.ANTENNE;
        } else if (designation.contains("guc") || designation.contains("gu")) {
            return WorkflowInstance.EmplacementType.GUC;
        } else if (designation.contains("aha-af") || designation.contains("commission")) {
            return WorkflowInstance.EmplacementType.COMMISSION_AHA_AF;
        } else if (designation.contains("service technique")) {
            return WorkflowInstance.EmplacementType.SERVICE_TECHNIQUE;
        }
        
        return WorkflowInstance.EmplacementType.GUC; // Default
    }

    private boolean canUserActOnEtape(Etape etape, Utilisateur utilisateur) {
        WorkflowInstance.EmplacementType emplacement = getEmplacementForEtape(etape);
        
        switch (utilisateur.getRole()) {
            case AGENT_ANTENNE:
                return emplacement == WorkflowInstance.EmplacementType.ANTENNE;
            case AGENT_GUC:
                return emplacement == WorkflowInstance.EmplacementType.GUC;
            case AGENT_COMMISSION_TERRAIN:
                return emplacement == WorkflowInstance.EmplacementType.COMMISSION_AHA_AF;
            case SERVICE_TECHNIQUE:
                return emplacement == WorkflowInstance.EmplacementType.SERVICE_TECHNIQUE;
            case ADMIN:
                return true;
            default:
                return false;
        }
    }

    /**
     * ✅ FIXED: Calculate deadline from start date, considering working days and holidays
     */
    private LocalDateTime calculateDeadline(LocalDateTime startDate, int durationInWorkingDays) {
        LocalDate currentDate = startDate.toLocalDate();
        int addedDays = 0;
        
        log.debug("Calculating deadline from: {} with duration: {} working days", startDate, durationInWorkingDays);
        
        // Get holidays for the period (look ahead more days to account for weekends)
        LocalDate endSearchDate = currentDate.plusDays(durationInWorkingDays * 2 + 30); // Buffer for weekends/holidays
        Set<LocalDate> holidays = joursFerieRepository.findByDateBetween(currentDate, endSearchDate)
                .stream()
                .map(JoursFerie::getDate)
                .collect(Collectors.toSet());
        
        log.debug("Found {} holidays in the period", holidays.size());
        
        while (addedDays < durationInWorkingDays) {
            currentDate = currentDate.plusDays(1);
            
            // Skip weekends and holidays
            if (currentDate.getDayOfWeek() != DayOfWeek.SATURDAY && 
                currentDate.getDayOfWeek() != DayOfWeek.SUNDAY && 
                !holidays.contains(currentDate)) {
                addedDays++;
                log.trace("Added working day {}: {} (total: {})", addedDays, currentDate, addedDays);
            } else {
                log.trace("Skipped non-working day: {} (weekend: {}, holiday: {})", 
                         currentDate, 
                         currentDate.getDayOfWeek() == DayOfWeek.SATURDAY || currentDate.getDayOfWeek() == DayOfWeek.SUNDAY,
                         holidays.contains(currentDate));
            }
        }
        
        LocalDateTime deadline = currentDate.atTime(17, 0); // End of business day
        log.debug("Calculated deadline: {} (added {} working days)", deadline, durationInWorkingDays);
        
        return deadline;
    }

    /**
     * ✅ FIXED: Calculate remaining working days until deadline
     */
    private int calculateRemainingDays(LocalDateTime deadline) {
        LocalDate deadlineDate = deadline.toLocalDate();
        LocalDate today = LocalDate.now();
        
        log.trace("Calculating remaining days from today: {} to deadline: {}", today, deadlineDate);
        
        // ✅ FIXED: If deadline has passed, return negative number of overdue days
        if (deadlineDate.isBefore(today)) {
            int overdueDays = calculateWorkingDaysBetween(deadlineDate, today);
            log.trace("Deadline passed. Overdue by {} working days", overdueDays);
            return -overdueDays; // Negative for overdue
        }
        
        // ✅ FIXED: If deadline is today, return 0
        if (deadlineDate.equals(today)) {
            log.trace("Deadline is today");
            return 0;
        }
        
        // Calculate remaining working days
        int remainingDays = calculateWorkingDaysBetween(today, deadlineDate);
        log.trace("Remaining working days: {}", remainingDays);
        
        return remainingDays;
    }
    
    /**
     * ✅ NEW: Helper method to calculate working days between two dates
     */
    private int calculateWorkingDaysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate.equals(endDate)) {
            return 0;
        }
        
        // Get holidays for the period
        Set<LocalDate> holidays = joursFerieRepository.findByDateBetween(startDate, endDate)
                .stream()
                .map(JoursFerie::getDate)
                .collect(Collectors.toSet());
        
        int workingDays = 0;
        LocalDate currentDate = startDate;
        
        while (currentDate.isBefore(endDate)) {
            currentDate = currentDate.plusDays(1);
            
            // Count only working days (not weekends or holidays)
            if (currentDate.getDayOfWeek() != DayOfWeek.SATURDAY && 
                currentDate.getDayOfWeek() != DayOfWeek.SUNDAY && 
                !holidays.contains(currentDate)) {
                workingDays++;
            }
        }
        
        return workingDays;
    }

    private void createHistoryEntry(Dossier dossier, Etape etape, LocalDateTime dateEntree, 
                                   LocalDateTime dateSortie, Utilisateur utilisateur, String commentaire) {
        HistoriqueWorkflow history = new HistoriqueWorkflow();
        history.setDossier(dossier);
        history.setEtape(etape);
        history.setEtapeDesignation(etape.getDesignation());
        history.setEmplacementType(getEmplacementForEtape(etape));
        history.setDateEntree(dateEntree);
        history.setDateSortie(dateSortie);
        history.setUtilisateur(utilisateur);
        history.setCommentaire(commentaire);
        
        if (dateSortie != null) {
            long durationDays = java.time.Duration.between(dateEntree, dateSortie).toDays();
            history.setDureeJours((int) durationDays);
            history.setEnRetard(durationDays > etape.getDureeJours());
        }
        
        historiqueWorkflowRepository.save(history);
        
        log.debug("Created history entry for dossier ID: {} at etape: {}", 
                 dossier.getId(), etape.getDesignation());
    }

    private void closeCurrentHistoryEntry(Dossier dossier, WorkflowInstance workflow, 
                                         LocalDateTime dateSortie, Utilisateur utilisateur, String commentaire) {
        List<HistoriqueWorkflow> openEntries = historiqueWorkflowRepository.findByDossierId(dossier.getId())
                .stream()
                .filter(h -> h.getDateSortie() == null)
                .collect(Collectors.toList());
        
        log.debug("Closing {} open history entries for dossier ID: {}", openEntries.size(), dossier.getId());
        
        for (HistoriqueWorkflow entry : openEntries) {
            entry.setDateSortie(dateSortie);
            long durationDays = java.time.Duration.between(entry.getDateEntree(), dateSortie).toDays();
            entry.setDureeJours((int) durationDays);
            entry.setEnRetard(durationDays > entry.getEtape().getDureeJours());
            historiqueWorkflowRepository.save(entry);
        }
    }

    /**
     * Helper class for etape information
     * ✅ ENHANCED: Added joursDeRetard field
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class EtapeInfo {
        private Etape etape;
        private String designation;
        private String phase;
        private Integer dureeJours;
        private Integer ordre;
        private WorkflowInstance.EmplacementType emplacementActuel;
        private LocalDateTime dateEntree;
        private LocalDateTime dateLimite;
        private Integer joursRestants;      // Positive number of remaining days (0 if overdue)
        private Integer joursDeRetard;      // Positive number of overdue days (0 if not overdue)
        private Boolean enRetard;
    }
}