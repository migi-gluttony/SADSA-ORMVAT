package ormvat.sadsa.service.service_technique;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ormvat.sadsa.dto.common.DossierCommonDTOs.*;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;
import ormvat.sadsa.service.common.DossierCommonService;
import ormvat.sadsa.service.common.WorkflowService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceTechniqueService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final WorkflowService workflowService;
    private final DossierCommonService dossierCommonService;

    // ============================================================================
    // PHASE-BASED ACTION METHODS - SIMPLIFIED
    // ============================================================================

    /**
     * Complete implementation and return to GUC (Phase 7 → Phase 8)
     */
    @Transactional
    public DossierActionResponse completeImplementation(Long dossierId, String userEmail, String commentaire) {
        try {
            Utilisateur utilisateur = validateServiceTechniqueUser(userEmail);
            Dossier dossier = getDossier(dossierId);
            
            // Verify we're in Phase 7
            Long currentPhase = getCurrentPhase(dossier);
            if (currentPhase != 7L) {
                throw new RuntimeException("Finalisation implémentation autorisée uniquement en Phase 7 (actuelle: Phase " + currentPhase + ")");
            }

            // Verify this is an infrastructure project (AMENAGEMENT HYDRO-AGRICOLE)
            Long rubriqueId = dossier.getSousRubrique().getRubrique().getId();
            if (rubriqueId != 3L) {
                log.warn("Service Technique called for non-infrastructure project (rubrique {})", rubriqueId);
            }

            // Move to next etape (Phase 8 - final RP - Phase GUC)
            workflowService.moveToNextEtape(dossier, utilisateur, 
                "Implémentation terminée par le Service Technique. " + commentaire);

            // Keep status as REALIZATION_IN_PROGRESS (GUC will finalize)
            dossier.setStatus(Dossier.DossierStatus.REALIZATION_IN_PROGRESS);
            dossierRepository.save(dossier);

            // Create audit trail
            dossierCommonService.createAuditTrail("IMPLEMENTATION_TERMINEE_PHASE_7", dossier, utilisateur, 
                String.format("Implémentation terminée par le Service Technique. Commentaire: %s", commentaire));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Implémentation terminée - Dossier transféré au GUC pour finalisation")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "fromPhase", 7,
                            "toPhase", 8,
                            "nextStep", "GUC Final Realization",
                            "implementationComplete", true
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la finalisation de l'implémentation", e);
            throw new RuntimeException("Erreur lors de la finalisation: " + e.getMessage());
        }
    }

    /**
     * Report implementation issues and return to GUC for review (Phase 7 → Phase 6)
     */
    @Transactional
    public DossierActionResponse reportImplementationIssues(Long dossierId, String userEmail, String motif, String commentaire) {
        try {
            Utilisateur utilisateur = validateServiceTechniqueUser(userEmail);
            Dossier dossier = getDossier(dossierId);
            
            // Verify we're in Phase 7
            Long currentPhase = getCurrentPhase(dossier);
            if (currentPhase != 7L) {
                throw new RuntimeException("Retour pour problèmes autorisé uniquement depuis Phase 7 (actuelle: Phase " + currentPhase + ")");
            }

            // Return to previous etape (Phase 6 - RP - Phase GUC)
            workflowService.returnToPreviousEtape(dossier, utilisateur, 
                String.format("Problèmes d'implémentation signalés. Motif: %s. Commentaire: %s", motif, commentaire));

            // Update status to indicate issues
            dossier.setStatus(Dossier.DossierStatus.IN_REVIEW);
            dossierRepository.save(dossier);

            // Create audit trail
            dossierCommonService.createAuditTrail("PROBLEMES_IMPLEMENTATION_PHASE_7", dossier, utilisateur, 
                String.format("Problèmes d'implémentation signalés. Motif: %s. Commentaire: %s", motif, commentaire));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Problèmes signalés - Dossier retourné au GUC pour révision")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "fromPhase", 7,
                            "toPhase", 6,
                            "motifRetour", motif,
                            "hasIssues", true
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du signalement des problèmes d'implémentation", e);
            throw new RuntimeException("Erreur lors du signalement: " + e.getMessage());
        }
    }

    /**
     * Update implementation progress (Phase 7 - status update only)
     */
    @Transactional
    public DossierActionResponse updateImplementationProgress(Long dossierId, String userEmail, String progressNote) {
        try {
            Utilisateur utilisateur = validateServiceTechniqueUser(userEmail);
            Dossier dossier = getDossier(dossierId);
            
            // Verify we're in Phase 7
            Long currentPhase = getCurrentPhase(dossier);
            if (currentPhase != 7L) {
                throw new RuntimeException("Mise à jour progrès autorisée uniquement en Phase 7 (actuelle: Phase " + currentPhase + ")");
            }

            // Create audit trail for progress update (no workflow change)
            dossierCommonService.createAuditTrail("PROGRES_IMPLEMENTATION_PHASE_7", dossier, utilisateur, 
                String.format("Mise à jour du progrès d'implémentation: %s", progressNote));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Progrès d'implémentation mis à jour")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "currentPhase", 7,
                            "progressUpdate", true,
                            "note", progressNote
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du progrès", e);
            throw new RuntimeException("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    // ============================================================================
    // HELPER METHODS - SIMPLIFIED
    // ============================================================================

    /**
     * Validate that user is Service Technique agent
     */
    private Utilisateur validateServiceTechniqueUser(String userEmail) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!utilisateur.getRole().equals(Utilisateur.UserRole.SERVICE_TECHNIQUE)) {
            throw new RuntimeException("Seul un agent du Service Technique peut effectuer cette action");
        }

        return utilisateur;
    }

    /**
     * Get dossier by ID
     */
    private Dossier getDossier(Long dossierId) {
        return dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));
    }

    /**
     * Get current phase number (1-8) for dossier
     */
    private Long getCurrentPhase(Dossier dossier) {
        try {
            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            return etapeInfo.getEtapeId();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de déterminer la phase actuelle du dossier");
        }
    }

    // ============================================================================
    // AVAILABLE ACTIONS - SIMPLIFIED
    // ============================================================================

    /**
     * Get available actions for Service Technique agents
     */
    public List<AvailableActionDTO> getAvailableActionsForServiceTechnique() {
        List<AvailableActionDTO> actions = new ArrayList<>();

        actions.add(AvailableActionDTO.builder()
                .action("complete_implementation")
                .label("Finaliser Implémentation")
                .icon("pi-check-circle")
                .severity("success")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Marquer l'implémentation comme terminée")
                .build());

        actions.add(AvailableActionDTO.builder()
                .action("report_issues")
                .label("Signaler Problèmes")
                .icon("pi-exclamation-triangle")
                .severity("warning")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Signaler des problèmes d'implémentation au GUC")
                .build());

        actions.add(AvailableActionDTO.builder()
                .action("update_progress")
                .label("Mettre à jour Progrès")
                .icon("pi-refresh")
                .severity("info")
                .requiresComment(true)
                .requiresConfirmation(false)
                .description("Mettre à jour le progrès de l'implémentation")
                .build());

        return actions;
    }

    /**
     * Get available actions for specific dossier and Service Technique user
     */
    public List<AvailableActionDTO> getAvailableActionsForDossier(Dossier dossier, Utilisateur utilisateur) {
        List<AvailableActionDTO> availableActions = new ArrayList<>();
        
        try {
            // Must be Service Technique agent
            if (!utilisateur.getRole().equals(Utilisateur.UserRole.SERVICE_TECHNIQUE)) {
                return availableActions;
            }

            // Can only act if user can act on current etape and it's Phase 7
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                return availableActions;
            }

            Long currentPhase = getCurrentPhase(dossier);
            if (currentPhase != 7L) {
                return availableActions;
            }

            // Verify this is an infrastructure project (should be for Phase 7)
            Long rubriqueId = dossier.getSousRubrique().getRubrique().getId();
            if (rubriqueId != 3L) {
                log.warn("Service Technique accessed non-infrastructure project (rubrique {})", rubriqueId);
                // Still allow actions but log warning
            }

            // All Service Technique actions are available when at Phase 7
            availableActions.add(AvailableActionDTO.builder()
                    .action("complete_implementation")
                    .label("Finaliser Implémentation")
                    .icon("pi-check-circle")
                    .severity("success")
                    .requiresComment(true)
                    .requiresConfirmation(true)
                    .description("Marquer l'implémentation comme terminée")
                    .build());

            availableActions.add(AvailableActionDTO.builder()
                    .action("report_issues")
                    .label("Signaler Problèmes")
                    .icon("pi-exclamation-triangle")
                    .severity("warning")
                    .requiresComment(true)
                    .requiresConfirmation(true)
                    .description("Signaler des problèmes au GUC")
                    .build());

            availableActions.add(AvailableActionDTO.builder()
                    .action("update_progress")
                    .label("Mettre à jour Progrès")
                    .icon("pi-refresh")
                    .severity("info")
                    .requiresComment(true)
                    .requiresConfirmation(false)
                    .description("Mettre à jour le progrès")
                    .build());

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des actions disponibles", e);
        }

        return availableActions;
    }
}