package ormvat.sadsa.service.agent_antenne;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ormvat.sadsa.dto.common.DossierCommonDTOs.*;
import ormvat.sadsa.dto.agent_antenne.DossierAntenneActionDTOs.*;
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
public class DossierAntenneService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final WorkflowService workflowService;
    private final DossierCommonService dossierCommonService;

    // ============================================================================
    // PHASE-BASED ACTION METHODS - SIMPLIFIED
    // ============================================================================

    /**
     * Send dossier to GUC (Phase 1 → Phase 2)
     */
    @Transactional
    public DossierActionResponse sendDossierToGUC(SendToGUCRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = validateAntenneUser(userEmail);
            Dossier dossier = getDossier(request.getDossierId());
            
            // Verify we're in Phase 1 and user owns this dossier
            validateAntenneOwnership(dossier, utilisateur);
            Long currentPhase = getCurrentPhase(dossier);
            if (currentPhase != 1L) {
                throw new RuntimeException("Envoi au GUC autorisé uniquement depuis Phase 1 (actuelle: Phase " + currentPhase + ")");
            }

            // Verify dossier can be sent
            if (dossier.getStatus() != Dossier.DossierStatus.DRAFT && 
                dossier.getStatus() != Dossier.DossierStatus.RETURNED_FOR_COMPLETION) {
                throw new RuntimeException("Le dossier doit être en brouillon ou retourné pour être envoyé");
            }

            // Update dossier and move to Phase 2
            dossier.setStatus(Dossier.DossierStatus.SUBMITTED);
            dossier.setDateSubmission(LocalDateTime.now());
            dossierRepository.save(dossier);

            workflowService.moveToNextEtape(dossier, utilisateur, 
                request.getCommentaire() != null ? request.getCommentaire() : "Envoi au GUC");

            // Create audit trail
            dossierCommonService.createAuditTrail("ENVOI_GUC_PHASE_2", dossier, utilisateur, 
                "Envoi du dossier au GUC depuis Phase 1");

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier envoyé au GUC avec succès")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "fromPhase", 1,
                            "toPhase", 2,
                            "nextStep", "GUC"
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du dossier au GUC", e);
            throw new RuntimeException("Erreur lors de l'envoi: " + e.getMessage());
        }
    }

    /**
     * Initialize dossier workflow (at creation)
     */
    @Transactional
    public void initializeDossierWorkflow(Dossier dossier, Utilisateur utilisateur) {
        try {
            // Initialize workflow at Phase 1
            workflowService.initializeWorkflow(dossier, utilisateur);
            
            // Set initial status
            dossier.setStatus(Dossier.DossierStatus.DRAFT);
            dossierRepository.save(dossier);

            log.info("Workflow initialisé pour le dossier {} par l'utilisateur {}", 
                dossier.getId(), utilisateur.getEmail());

        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation du workflow", e);
            throw new RuntimeException("Erreur lors de l'initialisation du workflow: " + e.getMessage());
        }
    }

    /**
     * Handle returned dossier from GUC (back to Phase 1)
     */
    @Transactional
    public DossierActionResponse handleReturnedDossier(Long dossierId, String userEmail) {
        try {
            Utilisateur utilisateur = validateAntenneUser(userEmail);
            Dossier dossier = getDossier(dossierId);
            
            validateAntenneOwnership(dossier, utilisateur);

            // Update dossier status to allow editing
            dossier.setStatus(Dossier.DossierStatus.RETURNED_FOR_COMPLETION);
            dossierRepository.save(dossier);

            // Move back to Phase 1
            workflowService.moveToEtape(dossier, 1L, utilisateur, 
                "Réception du dossier retourné par le GUC");

            // Create audit trail
            dossierCommonService.createAuditTrail("RECEPTION_RETOUR_PHASE_1", dossier, utilisateur, 
                "Réception du dossier retourné par le GUC");

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier retourné reçu - modifications possibles")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "returnedToPhase", 1,
                            "canEdit", true
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du traitement du dossier retourné", e);
            throw new RuntimeException("Erreur lors du traitement: " + e.getMessage());
        }
    }

    /**
     * Delete dossier (Phase 1 only, DRAFT status)
     */
    @Transactional
    public DossierActionResponse deleteDossier(DeleteDossierRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = validateAntenneUser(userEmail);
            Dossier dossier = getDossier(request.getDossierId());
            
            validateAntenneOwnership(dossier, utilisateur);
            
            // Verify can delete (Phase 1 + DRAFT only)
            Long currentPhase = getCurrentPhase(dossier);
            if (currentPhase != 1L) {
                throw new RuntimeException("Suppression autorisée uniquement en Phase 1 (actuelle: Phase " + currentPhase + ")");
            }
            
            if (dossier.getStatus() != Dossier.DossierStatus.DRAFT) {
                throw new RuntimeException("Seuls les dossiers en brouillon peuvent être supprimés");
            }

            // Create audit trail before deletion
            dossierCommonService.createAuditTrail("SUPPRESSION_DOSSIER", dossier, utilisateur, request.getMotif());

            // Delete dossier (cascade will handle related entities)
            dossierRepository.delete(dossier);

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier supprimé avec succès")
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la suppression du dossier", e);
            throw new RuntimeException("Erreur lors de la suppression: " + e.getMessage());
        }
    }

    /**
     * Initialize realization phase - Direct transition to Phase 6
     */
    @Transactional
    public DossierActionResponse startRealizationPhase(Long dossierId, String userEmail) {
        try {
            Utilisateur utilisateur = validateAntenneUser(userEmail);
            Dossier dossier = getDossier(dossierId);
            
            validateAntenneOwnership(dossier, utilisateur);

            // Validate dossier is approved and waiting for farmer
            if (dossier.getStatus() != Dossier.DossierStatus.APPROVED_AWAITING_FARMER) {
                throw new RuntimeException("Le dossier doit être approuvé et en attente du fermier pour initialiser la réalisation");
            }

            // Direct transition to Phase 6 (RP - Phase GUC) - Skip Phase 5
            workflowService.moveToEtape(dossier, 6L, utilisateur, 
                "Initialisation de la phase de réalisation - Transition directe vers GUC (Phase 6)");

            // Update status to realization in progress
            dossier.setStatus(Dossier.DossierStatus.REALIZATION_IN_PROGRESS);
            dossierRepository.save(dossier);

            // Create audit trail
            dossierCommonService.createAuditTrail("INITIALISATION_REALISATION_PHASE_6", dossier, utilisateur, 
                "Initialisation de la phase de réalisation - Dossier transféré directement au GUC (Phase 6)");

            log.info("Realization phase initialized for dossier {} - moved directly to Phase 6", dossierId);

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Phase de réalisation initialisée avec succès - Dossier transféré au GUC")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "fromState", "APPROVED_AWAITING_FARMER",
                            "toPhase", 6,
                            "nextStep", "GUC Realization Review"
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation de la phase de réalisation", e);
            throw new RuntimeException("Erreur lors de l'initialisation: " + e.getMessage());
        }
    }

    // ============================================================================
    // HELPER METHODS - SIMPLIFIED
    // ============================================================================

    /**
     * Validate that user is Antenne agent
     */
    private Utilisateur validateAntenneUser(String userEmail) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_ANTENNE)) {
            throw new RuntimeException("Seul un agent antenne peut effectuer cette action");
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

    /**
     * Validate that user can act on this dossier (owns the antenne)
     */
    private void validateAntenneOwnership(Dossier dossier, Utilisateur utilisateur) {
        if (utilisateur.getAntenne() == null || 
            !dossier.getAntenne().getId().equals(utilisateur.getAntenne().getId())) {
            throw new RuntimeException("Vous ne pouvez agir que sur les dossiers de votre antenne");
        }
    }

    // ============================================================================
    // AVAILABLE ACTIONS - SIMPLIFIED
    // ============================================================================

    /**
     * Get available actions for Agent Antenne - All possible actions
     */
    public List<AvailableActionDTO> getAvailableActionsForAntenne() {
        List<AvailableActionDTO> actions = new ArrayList<>();

        actions.add(AvailableActionDTO.builder()
                .action("send_to_guc")
                .label("Envoyer au GUC")
                .icon("pi-send")
                .severity("success")
                .requiresComment(false)
                .requiresConfirmation(true)
                .description("Envoyer le dossier au Guichet Unique Central pour traitement")
                .build());

        actions.add(AvailableActionDTO.builder()
                .action("initialize_realization")
                .label("Initialiser Réalisation")
                .icon("pi-play")
                .severity("info")
                .requiresComment(false)
                .requiresConfirmation(true)
                .description("Initialiser la phase de réalisation du projet approuvé")
                .build());

        actions.add(AvailableActionDTO.builder()
                .action("edit_dossier")
                .label("Modifier Dossier")
                .icon("pi-pencil")
                .severity("warning")
                .requiresComment(false)
                .requiresConfirmation(false)
                .description("Modifier les informations du dossier")
                .build());

        actions.add(AvailableActionDTO.builder()
                .action("delete_dossier")
                .label("Supprimer Dossier")
                .icon("pi-trash")
                .severity("danger")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Supprimer définitivement le dossier")
                .build());

        return actions;
    }

    /**
     * Get available actions for specific dossier based on current phase
     */
    public List<AvailableActionDTO> getAvailableActionsForDossier(Dossier dossier, Utilisateur utilisateur) {
        List<AvailableActionDTO> availableActions = new ArrayList<>();
        
        try {
            // Must be Agent Antenne from correct antenne
            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_ANTENNE)) {
                return availableActions;
            }
            
            if (utilisateur.getAntenne() == null || 
                !dossier.getAntenne().getId().equals(utilisateur.getAntenne().getId())) {
                return availableActions;
            }

            // Can only act if user can act on current etape
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                return availableActions;
            }

            Long currentPhase = getCurrentPhase(dossier);
            
            log.debug("Determining actions for dossier {} at phase: {}, status: {}", 
                     dossier.getId(), currentPhase, dossier.getStatus());

            switch (currentPhase.intValue()) {
                case 1: // Phase 1: AP - Phase Antenne
                    // Can edit if DRAFT or RETURNED_FOR_COMPLETION
                    if (dossier.getStatus() == Dossier.DossierStatus.DRAFT || 
                        dossier.getStatus() == Dossier.DossierStatus.RETURNED_FOR_COMPLETION) {
                        
                        availableActions.add(AvailableActionDTO.builder()
                                .action("edit_dossier")
                                .label("Modifier Dossier")
                                .icon("pi-pencil")
                                .severity("warning")
                                .requiresComment(false)
                                .requiresConfirmation(false)
                                .description("Modifier les informations du dossier")
                                .build());

                        availableActions.add(AvailableActionDTO.builder()
                                .action("send_to_guc")
                                .label("Envoyer au GUC")
                                .icon("pi-send")
                                .severity("success")
                                .requiresComment(false)
                                .requiresConfirmation(true)
                                .description("Envoyer le dossier au GUC après completion des documents")
                                .build());
                    }

                    // Can delete if DRAFT only
                    if (dossier.getStatus() == Dossier.DossierStatus.DRAFT) {
                        availableActions.add(AvailableActionDTO.builder()
                                .action("delete_dossier")
                                .label("Supprimer Dossier")
                                .icon("pi-trash")
                                .severity("danger")
                                .requiresComment(true)
                                .requiresConfirmation(true)
                                .description("Supprimer définitivement le dossier")
                                .build());
                    }
                    break;

                case 5: // Phase 5: RP - Phase Antenne (if ever used)
                    availableActions.add(AvailableActionDTO.builder()
                            .action("send_to_guc")
                            .label("Envoyer au GUC")
                            .icon("pi-send")
                            .severity("success")
                            .requiresComment(false)
                            .requiresConfirmation(true)
                            .description("Envoyer le dossier au GUC pour traitement de réalisation")
                            .build());
                    break;
            }

            // Special case: APPROVED_AWAITING_FARMER status
            if (dossier.getStatus() == Dossier.DossierStatus.APPROVED_AWAITING_FARMER) {
                availableActions.add(AvailableActionDTO.builder()
                        .action("initialize_realization")
                        .label("Initialiser Réalisation")
                        .icon("pi-play")
                        .severity("info")
                        .requiresComment(false)
                        .requiresConfirmation(true)
                        .description("Vérifier la fiche d'approbation et initialiser la phase de réalisation")
                        .build());
            }

            log.debug("Found {} available actions for dossier {} at phase {}", 
                     availableActions.size(), dossier.getId(), currentPhase);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des actions disponibles pour le dossier {}", dossier.getId(), e);
        }

        return availableActions;
    }
}