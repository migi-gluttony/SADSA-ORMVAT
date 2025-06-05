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

    // ============================================================================
    // PHASE MAPPING CONSTANTS - Single Source of Truth
    // ============================================================================
    
    /**
     * Get phase number (1-8) from etape ID
     */
    public int getPhaseNumber(Long etapeId) {
        if (etapeId == null) return 0;
        return etapeId.intValue(); // Direct mapping: etapeId = phaseNumber
    }

    /**
     * Get emplacement type from etape ID
     */
    public WorkflowInstance.EmplacementType getEmplacementType(Long etapeId) {
        if (etapeId == null) return WorkflowInstance.EmplacementType.GUC;
        
        switch (etapeId.intValue()) {
            case 1: // AP - Phase Antenne
            case 5: // RP - Phase Antenne
                return WorkflowInstance.EmplacementType.ANTENNE;
                
            case 2: // AP - Phase GUC (Initial)
            case 4: // AP - Phase GUC (Final)
            case 6: // RP - Phase GUC (Realization Review)
            case 8: // RP - Phase GUC (Final)
                return WorkflowInstance.EmplacementType.GUC;
                
            case 3: // AP - Phase Commission
                return WorkflowInstance.EmplacementType.COMMISSION_AHA_AF;
                
            case 7: // RP - Phase Service Technique
                return WorkflowInstance.EmplacementType.SERVICE_TECHNIQUE;
                
            default:
                return WorkflowInstance.EmplacementType.GUC;
        }
    }

    /**
     * Get user role that can act on this etape
     */
    public Utilisateur.UserRole getAuthorizedRole(Long etapeId) {
        if (etapeId == null) return null;
        
        switch (etapeId.intValue()) {
            case 1: // AP - Phase Antenne
            case 5: // RP - Phase Antenne
                return Utilisateur.UserRole.AGENT_ANTENNE;
                
            case 2: // AP - Phase GUC (Initial)
            case 4: // AP - Phase GUC (Final)
            case 6: // RP - Phase GUC (Realization Review)
            case 8: // RP - Phase GUC (Final)
                return Utilisateur.UserRole.AGENT_GUC;
                
            case 3: // AP - Phase Commission
                return Utilisateur.UserRole.AGENT_COMMISSION_TERRAIN;
                
            case 7: // RP - Phase Service Technique
                return Utilisateur.UserRole.SERVICE_TECHNIQUE;
                
            default:
                return null;
        }
    }

    /**
     * Get display name for phase
     */
    public String getPhaseDisplayName(Long etapeId) {
        if (etapeId == null) return "Phase inconnue";
        
        switch (etapeId.intValue()) {
            case 1: return "Phase 1: Création & Documents (Antenne)";
            case 2: return "Phase 2: Validation GUC Initiale";
            case 3: return "Phase 3: Commission Terrain";
            case 4: return "Phase 4: Approbation Finale GUC";
            case 5: return "Phase 5: Réalisation - Antenne";
            case 6: return "Phase 6: Réalisation - GUC";
            case 7: return "Phase 7: Réalisation - Service Technique";
            case 8: return "Phase 8: Réalisation - GUC Final";
            default: return "Phase " + etapeId + " (inconnue)";
        }
    }

    /**
     * Check if phase is in AP (Approval Phase)
     */
    public boolean isApprovalPhase(Long etapeId) {
        return etapeId != null && etapeId >= 1 && etapeId <= 4;
    }

    /**
     * Check if phase is in RP (Realization Phase)
     */
    public boolean isRealizationPhase(Long etapeId) {
        return etapeId != null && etapeId >= 5 && etapeId <= 8;
    }

    // ============================================================================
    // WORKFLOW INITIALIZATION
    // ============================================================================

    /**
     * Initialize workflow for new dossier - starts at Phase 1
     */
    @Transactional
    public WorkflowInstance initializeWorkflow(Dossier dossier, Utilisateur utilisateur) {
        log.info("Initializing workflow for dossier ID: {} with reference: {}", 
                 dossier.getId(), dossier.getReference());
        
        // Check if workflow already exists
        List<WorkflowInstance> existingWorkflows = workflowInstanceRepository.findByDossierId(dossier.getId());
        if (!existingWorkflows.isEmpty()) {
            log.warn("Workflow already exists for dossier ID: {}. Returning existing workflow.", dossier.getId());
            return existingWorkflows.get(0);
        }
        
        // Get Phase 1 etape
        Etape firstEtape = etapeRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Etape Phase 1 (ID=1) non trouvée"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateLimite = calculateDeadline(now, firstEtape.getDureeJours());
        
        log.info("Creating workflow for dossier ID: {} starting at Phase 1 with deadline: {}", 
                 dossier.getId(), dateLimite);

        WorkflowInstance workflow = new WorkflowInstance();
        workflow.setDossier(dossier);
        workflow.setEtape(firstEtape);
        workflow.setEmplacementActuel(getEmplacementType(firstEtape.getId()));
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

    // ============================================================================
    // WORKFLOW TRANSITIONS
    // ============================================================================

    /**
     * Move to next sequential etape
     */
    @Transactional
    public WorkflowInstance moveToNextEtape(Dossier dossier, Utilisateur utilisateur, String commentaire) {
        WorkflowInstance currentWorkflow = getCurrentWorkflowInstance(dossier);
        Long currentEtapeId = currentWorkflow.getEtape().getId();
        
        log.info("Moving dossier ID: {} from phase {} to next phase", 
                 dossier.getId(), getPhaseNumber(currentEtapeId));
        
        // Get next etape ID (sequential)
        Long nextEtapeId = getNextEtapeId(currentEtapeId);
        if (nextEtapeId == null) {
            throw new RuntimeException("Aucune étape suivante après la phase " + getPhaseNumber(currentEtapeId));
        }

        return transitionToEtape(dossier, nextEtapeId, utilisateur, commentaire);
    }

    /**
     * Move to previous sequential etape
     */
    @Transactional
    public WorkflowInstance returnToPreviousEtape(Dossier dossier, Utilisateur utilisateur, String commentaire) {
        WorkflowInstance currentWorkflow = getCurrentWorkflowInstance(dossier);
        Long currentEtapeId = currentWorkflow.getEtape().getId();
        
        // Get previous etape ID (sequential)
        Long previousEtapeId = getPreviousEtapeId(currentEtapeId);
        if (previousEtapeId == null) {
            throw new RuntimeException("Aucune étape précédente avant la phase " + getPhaseNumber(currentEtapeId));
        }

        return transitionToEtape(dossier, previousEtapeId, utilisateur, commentaire);
    }

    /**
     * Move to specific etape by ID
     */
    @Transactional
    public WorkflowInstance moveToEtape(Dossier dossier, Long etapeId, Utilisateur utilisateur, String commentaire) {
        if (etapeId == null || etapeId < 1 || etapeId > 8) {
            throw new RuntimeException("Etape ID invalide: " + etapeId);
        }
        return transitionToEtape(dossier, etapeId, utilisateur, commentaire);
    }

    /**
     * Move to specific etape by designation (legacy support)
     */
    @Transactional
    public WorkflowInstance moveToEtape(Dossier dossier, String etapeDesignation, Utilisateur utilisateur, String commentaire) {
        Long etapeId = getEtapeIdByDesignation(etapeDesignation);
        if (etapeId == null) {
            throw new RuntimeException("Etape '" + etapeDesignation + "' non trouvée");
        }
        return moveToEtape(dossier, etapeId, utilisateur, commentaire);
    }

    // ============================================================================
    // WORKFLOW QUERIES
    // ============================================================================

    /**
     * Get current workflow instance for dossier
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
        
        // Get the most recent workflow
        WorkflowInstance latestWorkflow = workflows.stream()
                .max((w1, w2) -> w1.getDateEntree().compareTo(w2.getDateEntree()))
                .orElse(workflows.get(0));
                
        log.debug("Found workflow ID: {} for dossier ID: {} at phase: {}", 
                 latestWorkflow.getId(), dossier.getId(), getPhaseNumber(latestWorkflow.getEtape().getId()));
                 
        return latestWorkflow;
    }

    /**
     * Get current etape info with calculated remaining time
     */
    public EtapeInfo getCurrentEtapeInfo(Dossier dossier) {
        WorkflowInstance workflow = getCurrentWorkflowInstance(dossier);
        Etape etape = workflow.getEtape();
        Long etapeId = etape.getId();
        
        // Recalculate remaining days based on current time
        int joursRestants = calculateRemainingDays(workflow.getDateLimite());
        boolean enRetard = joursRestants < 0;
        
        // Update workflow instance with current calculations
        workflow.setJoursRestants(Math.abs(joursRestants));
        workflow.setEnRetard(enRetard);
        workflowInstanceRepository.save(workflow);

        log.debug("Etape info for dossier ID: {} - Phase: {}, Jours restants: {}, En retard: {}", 
                 dossier.getId(), getPhaseNumber(etapeId), joursRestants, enRetard);

        return EtapeInfo.builder()
                .etape(etape)
                .etapeId(etapeId)
                .phaseNumber(getPhaseNumber(etapeId))
                .designation(getPhaseDisplayName(etapeId))
                .phase(isApprovalPhase(etapeId) ? "AP" : "RP")
                .dureeJours(etape.getDureeJours())
                .ordre(etape.getOrdre())
                .emplacementActuel(workflow.getEmplacementActuel())
                .dateEntree(workflow.getDateEntree())
                .dateLimite(workflow.getDateLimite())
                .joursRestants(Math.max(0, joursRestants))
                .joursDeRetard(Math.max(0, -joursRestants))
                .enRetard(enRetard)
                .build();
    }

    /**
     * Check if user can act on current etape
     */
    public boolean canUserActOnCurrentEtape(Dossier dossier, Utilisateur utilisateur) {
        try {
            WorkflowInstance workflow = getCurrentWorkflowInstance(dossier);
            Long currentEtapeId = workflow.getEtape().getId();
            
            // Admin can always act
            if (utilisateur.getRole() == Utilisateur.UserRole.ADMIN) {
                return true;
            }
            
            // Check if user's role matches required role for this etape
            Utilisateur.UserRole requiredRole = getAuthorizedRole(currentEtapeId);
            return requiredRole != null && requiredRole.equals(utilisateur.getRole());
            
        } catch (Exception e) {
            log.warn("Cannot check user permissions for dossier {}: {}", dossier.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Get workflow history for dossier
     */
    public List<HistoriqueWorkflow> getWorkflowHistory(Dossier dossier) {
        return historiqueWorkflowRepository.findByDossierId(dossier.getId());
    }

    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================

    /**
     * Core transition method
     */
    private WorkflowInstance transitionToEtape(Dossier dossier, Long targetEtapeId, Utilisateur utilisateur, String commentaire) {
        WorkflowInstance currentWorkflow = getCurrentWorkflowInstance(dossier);
        LocalDateTime now = LocalDateTime.now();
        
        // Get target etape
        Etape targetEtape = etapeRepository.findById(targetEtapeId)
                .orElseThrow(() -> new RuntimeException("Etape ID " + targetEtapeId + " non trouvée"));
        
        log.info("Transitioning dossier ID: {} from phase {} to phase {}", 
                 dossier.getId(), getPhaseNumber(currentWorkflow.getEtape().getId()), getPhaseNumber(targetEtapeId));

        // Close current workflow history
        closeCurrentHistoryEntry(dossier, currentWorkflow, now, utilisateur, commentaire);

        // Update workflow instance
        currentWorkflow.setEtape(targetEtape);
        currentWorkflow.setEmplacementActuel(getEmplacementType(targetEtapeId));
        currentWorkflow.setDateEntree(now);
        
        LocalDateTime newDeadline = calculateDeadline(now, targetEtape.getDureeJours());
        currentWorkflow.setDateLimite(newDeadline);
        currentWorkflow.setJoursRestants(calculateRemainingDays(newDeadline));
        currentWorkflow.setEnRetard(false); // Reset delay status for new etape

        workflowInstanceRepository.save(currentWorkflow);

        // Create new history entry
        createHistoryEntry(dossier, targetEtape, now, null, utilisateur, commentaire);

        log.info("Dossier ID: {} transitioned successfully to phase {} with new deadline: {}", 
                 dossier.getId(), getPhaseNumber(targetEtapeId), newDeadline);
        return currentWorkflow;
    }

    /**
     * Get next etape ID in sequence
     */
    private Long getNextEtapeId(Long currentEtapeId) {
        if (currentEtapeId == null) return null;
        
        switch (currentEtapeId.intValue()) {
            case 1: return 2L; // Phase 1 → Phase 2
            case 2: return 3L; // Phase 2 → Phase 3 
            case 3: return 4L; // Phase 3 → Phase 4
            case 4: return 6L; // Phase 4 → Phase 6 (Skip Phase 5 - it's a halt state)
            case 5: return 6L; // Phase 5 → Phase 6
            case 6: return 7L; // Phase 6 → Phase 7
            case 7: return 8L; // Phase 7 → Phase 8
            case 8: return null; // Phase 8 is final
            default: return null;
        }
    }

    /**
     * Get previous etape ID in sequence
     */
    private Long getPreviousEtapeId(Long currentEtapeId) {
        if (currentEtapeId == null) return null;
        
        switch (currentEtapeId.intValue()) {
            case 1: return null; // Phase 1 is first
            case 2: return 1L; // Phase 2 → Phase 1
            case 3: return 2L; // Phase 3 → Phase 2
            case 4: return 3L; // Phase 4 → Phase 3
            case 5: return 1L; // Phase 5 → Phase 1 (special case for returned dossiers)
            case 6: return 2L; // Phase 6 → Phase 2 (for GUC returns)
            case 7: return 6L; // Phase 7 → Phase 6
            case 8: return 7L; // Phase 8 → Phase 7
            default: return null;
        }
    }

    /**
     * Legacy support: Map designation to etape ID
     */
    private Long getEtapeIdByDesignation(String designation) {
        if (designation == null) return null;
        
        if (designation.contains("AP - Phase Antenne")) return 1L;
        if (designation.contains("AP - Phase GUC")) return 2L; // Default to Phase 2
        if (designation.contains("AP - Phase AHA-AF")) return 3L;
        if (designation.contains("RP - Phase Antenne")) return 5L;
        if (designation.contains("RP - Phase GUC")) return 6L; // Default to Phase 6
        if (designation.contains("RP - Phase service technique")) return 7L;
        
        return null;
    }

    /**
     * Calculate deadline from start date, considering working days and holidays
     */
    private LocalDateTime calculateDeadline(LocalDateTime startDate, int durationInWorkingDays) {
        LocalDate currentDate = startDate.toLocalDate();
        int addedDays = 0;
        
        log.debug("Calculating deadline from: {} with duration: {} working days", startDate, durationInWorkingDays);
        
        // Get holidays for the period
        LocalDate endSearchDate = currentDate.plusDays(durationInWorkingDays * 2 + 30);
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
            }
        }
        
        LocalDateTime deadline = currentDate.atTime(17, 0); // End of business day
        log.debug("Calculated deadline: {} (added {} working days)", deadline, durationInWorkingDays);
        
        return deadline;
    }

    /**
     * Calculate remaining working days until deadline
     */
    private int calculateRemainingDays(LocalDateTime deadline) {
        LocalDate deadlineDate = deadline.toLocalDate();
        LocalDate today = LocalDate.now();
        
        // If deadline has passed, return negative number of overdue days
        if (deadlineDate.isBefore(today)) {
            int overdueDays = calculateWorkingDaysBetween(deadlineDate, today);
            return -overdueDays; // Negative for overdue
        }
        
        // If deadline is today, return 0
        if (deadlineDate.equals(today)) {
            return 0;
        }
        
        // Calculate remaining working days
        return calculateWorkingDaysBetween(today, deadlineDate);
    }
    
    /**
     * Helper method to calculate working days between two dates
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
        history.setEtapeDesignation(getPhaseDisplayName(etape.getId())); // Keep for display only
        history.setEmplacementType(getEmplacementType(etape.getId()));
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
        
        log.debug("Created history entry for dossier ID: {} at phase: {}", 
                 dossier.getId(), getPhaseNumber(etape.getId()));
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
     * Enhanced EtapeInfo class with ID-based fields
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class EtapeInfo {
        private Etape etape;
        private Long etapeId;                           // NEW: Direct etape ID
        private Integer phaseNumber;                    // NEW: Phase number (1-8)
        private String designation;                     // Display name
        private String phase;                          // AP or RP
        private Integer dureeJours;
        private Integer ordre;
        private WorkflowInstance.EmplacementType emplacementActuel;
        private LocalDateTime dateEntree;
        private LocalDateTime dateLimite;
        private Integer joursRestants;                 // Positive number of remaining days (0 if overdue)
        private Integer joursDeRetard;                 // Positive number of overdue days (0 if not overdue)
        private Boolean enRetard;
    }
}