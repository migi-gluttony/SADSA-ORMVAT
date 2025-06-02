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
     */
    @Transactional
    public WorkflowInstance initializeWorkflow(Dossier dossier, Utilisateur utilisateur) {
        // Get the first etape: AP - Phase Antenne
        Etape firstEtape = etapeRepository.findByDesignation("AP - Phase Antenne");
        if (firstEtape == null) {
            throw new RuntimeException("Etape 'AP - Phase Antenne' non trouvée");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateLimite = calculateDeadline(now, firstEtape.getDureeJours());

        WorkflowInstance workflow = new WorkflowInstance();
        workflow.setDossier(dossier);
        workflow.setEtape(firstEtape);
        workflow.setEtapeDesignation(firstEtape.getDesignation());
        workflow.setEmplacementActuel(getEmplacementForEtape(firstEtape));
        workflow.setDateEntree(now);
        workflow.setDateLimite(dateLimite);
        workflow.setJoursRestants(calculateRemainingDays(dateLimite));
        workflow.setEnRetard(false);

        workflowInstanceRepository.save(workflow);

        // Create history entry
        createHistoryEntry(dossier, firstEtape, now, null, utilisateur, "Initialisation du workflow");

        log.info("Workflow initialisé pour le dossier {} à l'étape {}", dossier.getId(), firstEtape.getDesignation());
        return workflow;
    }

    /**
     * Move to next etape in sequence
     */
    @Transactional
    public WorkflowInstance moveToNextEtape(Dossier dossier, Utilisateur utilisateur, String commentaire) {
        WorkflowInstance currentWorkflow = getCurrentWorkflowInstance(dossier);
        Etape currentEtape = currentWorkflow.getEtape();
        
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
     */
    public WorkflowInstance getCurrentWorkflowInstance(Dossier dossier) {
        List<WorkflowInstance> workflows = workflowInstanceRepository.findByDossierId(dossier.getId());
        if (workflows.isEmpty()) {
            throw new RuntimeException("Aucun workflow trouvé pour le dossier " + dossier.getId());
        }
        return workflows.get(0); // Latest workflow
    }

    /**
     * Get current etape info with calculated remaining time
     */
    public EtapeInfo getCurrentEtapeInfo(Dossier dossier) {
        WorkflowInstance workflow = getCurrentWorkflowInstance(dossier);
        Etape etape = workflow.getEtape();
        
        int joursRestants = calculateRemainingDays(workflow.getDateLimite());
        boolean enRetard = joursRestants < 0;

        return EtapeInfo.builder()
                .etape(etape)
                .designation(etape.getDesignation())
                .phase(etape.getPhase())
                .dureeJours(etape.getDureeJours())
                .ordre(etape.getOrdre())
                .emplacementActuel(workflow.getEmplacementActuel())
                .dateEntree(workflow.getDateEntree())
                .dateLimite(workflow.getDateLimite())
                .joursRestants(Math.max(0, joursRestants))
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
        currentWorkflow.setEnRetard(false);

        workflowInstanceRepository.save(currentWorkflow);

        // Create new history entry
        createHistoryEntry(dossier, targetEtape, now, null, utilisateur, commentaire);

        log.info("Dossier {} transitionné vers l'étape {}", dossier.getId(), targetEtape.getDesignation());
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

    private LocalDateTime calculateDeadline(LocalDateTime startDate, int durationInWorkingDays) {
        LocalDate currentDate = startDate.toLocalDate();
        int addedDays = 0;
        
        // Get holidays for the period
        LocalDate endSearchDate = currentDate.plusDays(durationInWorkingDays * 2); // Buffer for weekends/holidays
        Set<LocalDate> holidays = joursFerieRepository.findByDateBetween(currentDate, endSearchDate)
                .stream()
                .map(JoursFerie::getDate)
                .collect(Collectors.toSet());
        
        while (addedDays < durationInWorkingDays) {
            currentDate = currentDate.plusDays(1);
            
            // Skip weekends and holidays
            if (currentDate.getDayOfWeek() != DayOfWeek.SATURDAY && 
                currentDate.getDayOfWeek() != DayOfWeek.SUNDAY && 
                !holidays.contains(currentDate)) {
                addedDays++;
            }
        }
        
        return currentDate.atTime(17, 0); // End of business day
    }

    private int calculateRemainingDays(LocalDateTime deadline) {
        LocalDate deadlineDate = deadline.toLocalDate();
        LocalDate today = LocalDate.now();
        
        if (deadlineDate.isBefore(today)) {
            return (int) today.toEpochDay() - (int) deadlineDate.toEpochDay(); // Negative for overdue
        }
        
        int remainingDays = 0;
        LocalDate currentDate = today;
        
        // Get holidays
        Set<LocalDate> holidays = joursFerieRepository.findByDateBetween(today, deadlineDate)
                .stream()
                .map(JoursFerie::getDate)
                .collect(Collectors.toSet());
        
        while (currentDate.isBefore(deadlineDate)) {
            currentDate = currentDate.plusDays(1);
            
            // Count only working days
            if (currentDate.getDayOfWeek() != DayOfWeek.SATURDAY && 
                currentDate.getDayOfWeek() != DayOfWeek.SUNDAY && 
                !holidays.contains(currentDate)) {
                remainingDays++;
            }
        }
        
        return remainingDays;
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
    }

    private void closeCurrentHistoryEntry(Dossier dossier, WorkflowInstance workflow, 
                                         LocalDateTime dateSortie, Utilisateur utilisateur, String commentaire) {
        List<HistoriqueWorkflow> openEntries = historiqueWorkflowRepository.findByDossierId(dossier.getId())
                .stream()
                .filter(h -> h.getDateSortie() == null)
                .collect(Collectors.toList());
        
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
        private Integer joursRestants;
        private Boolean enRetard;
    }
}