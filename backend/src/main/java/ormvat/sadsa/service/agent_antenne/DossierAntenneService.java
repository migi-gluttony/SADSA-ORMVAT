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

@Service
@RequiredArgsConstructor
@Slf4j
public class DossierAntenneService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final WorkflowService workflowService;
    private final DossierCommonService dossierCommonService;

    /**
     * Send dossier to GUC (Agent Antenne action) - Phase 1 only
     */
    @Transactional
    public DossierActionResponse sendDossierToGUC(SendToGUCRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Verify permission - only Phase 1 (AP - Phase Antenne)
            if (!canSendToGUC(dossier, utilisateur)) {
                throw new RuntimeException("Vous n'avez pas l'autorisation d'envoyer ce dossier");
            }

            // Check if user can act on current etape (must be at Phase 1)
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                throw new RuntimeException("Cette action n'est pas autorisée à l'étape actuelle");
            }

            // TODO: Add validation that forms and documents are complete
            // validateDocumentCompletion(dossier);

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.SUBMITTED);
            dossier.setDateSubmission(LocalDateTime.now());
            dossierRepository.save(dossier);

            // Move to next etape using WorkflowService (Phase 2)
            WorkflowService.EtapeInfo currentEtapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            workflowService.moveToNextEtape(dossier, utilisateur, 
                request.getCommentaire() != null ? request.getCommentaire() : "Envoi au GUC");

            // Create audit trail
            dossierCommonService.createAuditTrail("ENVOI_GUC", dossier, utilisateur, 
                "Envoi du dossier au GUC depuis l'étape: " + currentEtapeInfo.getDesignation());

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier envoyé au GUC avec succès")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du dossier au GUC", e);
            throw new RuntimeException("Erreur lors de l'envoi: " + e.getMessage());
        }
    }

    /**
     * Handle dossier creation workflow initialization
     */
    @Transactional
    public void initializeDossierWorkflow(Dossier dossier, Utilisateur utilisateur) {
        try {
            // Initialize workflow at AP - Phase Antenne (Phase 1)
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
     * Handle returned dossier from GUC
     */
    @Transactional
    public DossierActionResponse handleReturnedDossier(Long dossierId, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Update dossier status to allow editing
            dossier.setStatus(Dossier.DossierStatus.RETURNED_FOR_COMPLETION);
            dossierRepository.save(dossier);

            // Move back to Phase 1 (AP - Phase Antenne)
            workflowService.moveToEtape(dossier, "AP - Phase Antenne", utilisateur, 
                "Retour du dossier pour complétion");

            // Create audit trail
            dossierCommonService.createAuditTrail("RECEPTION_RETOUR", dossier, utilisateur, 
                "Réception du dossier retourné par le GUC");

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier retourné reçu - modifications possibles")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du traitement du dossier retourné", e);
            throw new RuntimeException("Erreur lors du traitement: " + e.getMessage());
        }
    }

    /**
     * Delete dossier (Agent Antenne action) - Phase 1 only
     */
    @Transactional
    public DossierActionResponse deleteDossier(DeleteDossierRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Verify permission - only Phase 1 (AP - Phase Antenne)
            if (!canDeleteDossier(dossier, utilisateur)) {
                throw new RuntimeException("Vous n'avez pas l'autorisation de supprimer ce dossier");
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
     * REIMPLEMENTED: Initialize realization phase - Direct transition to Phase 6
     */
    @Transactional
    public DossierActionResponse startRealizationPhase(Long dossierId, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Validate dossier is approved and waiting for farmer
            if (!canInitializeRealization(dossier, utilisateur)) {
                throw new RuntimeException("Impossible d'initialiser la réalisation pour ce dossier");
            }

            // Validate dossier status
            if (dossier.getStatus() != Dossier.DossierStatus.APPROVED_AWAITING_FARMER) {
                throw new RuntimeException("Le dossier doit être approuvé et en attente du fermier pour initialiser la réalisation");
            }

            // Direct transition to Phase 6 (RP - Phase GUC) - Skip Phase 5
            workflowService.moveToEtape(dossier, "RP - Phase GUC", utilisateur, 
                "Initialisation de la phase de réalisation - Transition directe vers GUC");

            // Update status to realization in progress
            dossier.setStatus(Dossier.DossierStatus.REALIZATION_IN_PROGRESS);
            dossierRepository.save(dossier);

            // Create audit trail
            dossierCommonService.createAuditTrail("INITIALISATION_REALISATION", dossier, utilisateur, 
                "Initialisation de la phase de réalisation - Dossier transféré directement au GUC (Phase 6)");

            log.info("Realization phase initialized for dossier {} - moved directly to Phase 6 (RP - Phase GUC)", dossierId);

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Phase de réalisation initialisée avec succès - Dossier transféré au GUC")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation de la phase de réalisation", e);
            throw new RuntimeException("Erreur lors de l'initialisation: " + e.getMessage());
        }
    }

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
     * UPDATED: Get available actions for specific dossier based on current phase
     */
    public List<AvailableActionDTO> getAvailableActionsForDossier(Dossier dossier, Utilisateur utilisateur) {
        List<AvailableActionDTO> availableActions = new ArrayList<>();
        
        try {
            // Get current etape information
            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            String currentEtape = etapeInfo.getDesignation();
            
            log.debug("Determining actions for dossier {} at etape: {} with status: {}", 
                     dossier.getId(), currentEtape, dossier.getStatus());

            // Must be Agent Antenne from correct antenne
            if (!isCorrectAntenneAgent(dossier, utilisateur)) {
                return availableActions;
            }

            // PHASE 1: AP - Phase Antenne (Creation & Document Filling)
            if ("AP - Phase Antenne".equals(currentEtape)) {
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
            }

            // PHASES 2-3: Read-only (AP - Phase GUC, AP - Phase AHA-AF)
            else if (currentEtape.startsWith("AP - Phase GUC") || currentEtape.startsWith("AP - Phase AHA-AF")) {
                // No actions available - read-only
                log.debug("Dossier {} in read-only phase: {}", dossier.getId(), currentEtape);
            }

            // PHASE 4 END: Approved and waiting for farmer
            else if (dossier.getStatus() == Dossier.DossierStatus.APPROVED_AWAITING_FARMER) {
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

            // PHASES 6-8: Read-only (RP phases)
            else if (currentEtape.startsWith("RP")) {
                // No actions available - read-only
                log.debug("Dossier {} in RP read-only phase: {}", dossier.getId(), currentEtape);
            }

            log.debug("Found {} available actions for dossier {} at etape {}", 
                     availableActions.size(), dossier.getId(), currentEtape);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des actions disponibles pour le dossier {}", dossier.getId(), e);
        }

        return availableActions;
    }

    // Private helper methods

    private boolean canSendToGUC(Dossier dossier, Utilisateur utilisateur) {
        // Must be at Phase 1 with correct status
        boolean correctStatus = dossier.getStatus() == Dossier.DossierStatus.DRAFT ||
                               dossier.getStatus() == Dossier.DossierStatus.RETURNED_FOR_COMPLETION;
        
        // Must be Agent Antenne from correct antenne
        boolean correctRole = utilisateur.getRole() == Utilisateur.UserRole.AGENT_ANTENNE;
        boolean correctAntenne = isCorrectAntenneAgent(dossier, utilisateur);
        
        return correctStatus && correctRole && correctAntenne;
    }

    private boolean canDeleteDossier(Dossier dossier, Utilisateur utilisateur) {
        // Can only delete DRAFT dossiers at Phase 1
        boolean isDraft = dossier.getStatus() == Dossier.DossierStatus.DRAFT;
        boolean correctRole = utilisateur.getRole() == Utilisateur.UserRole.AGENT_ANTENNE;
        boolean correctAntenne = isCorrectAntenneAgent(dossier, utilisateur);
        
        return isDraft && correctRole && correctAntenne;
    }

    private boolean canInitializeRealization(Dossier dossier, Utilisateur utilisateur) {
        // Must be approved and waiting for farmer
        boolean correctStatus = dossier.getStatus() == Dossier.DossierStatus.APPROVED_AWAITING_FARMER;
        
        // Must be Agent Antenne from correct antenne
        boolean correctRole = utilisateur.getRole() == Utilisateur.UserRole.AGENT_ANTENNE;
        boolean correctAntenne = isCorrectAntenneAgent(dossier, utilisateur);
        
        return correctStatus && correctRole && correctAntenne;
    }

    private boolean isCorrectAntenneAgent(Dossier dossier, Utilisateur utilisateur) {
        return utilisateur.getAntenne() != null &&
               dossier.getAntenne().getId().equals(utilisateur.getAntenne().getId());
    }

    // TODO: Implement document completion validation
    // private void validateDocumentCompletion(Dossier dossier) {
    //     // Check if all required documents are uploaded and forms are filled
    //     // Throw exception if not complete
    // }
}