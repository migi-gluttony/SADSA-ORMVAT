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
     * Send dossier to GUC (Agent Antenne action)
     */
    @Transactional
    public DossierActionResponse sendDossierToGUC(SendToGUCRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Verify permission
            if (!canSendToGUC(dossier, utilisateur)) {
                throw new RuntimeException("Vous n'avez pas l'autorisation d'envoyer ce dossier");
            }

            // Check if user can act on current etape
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                throw new RuntimeException("Cette action n'est pas autorisée à l'étape actuelle");
            }

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.SUBMITTED);
            dossier.setDateSubmission(LocalDateTime.now());
            dossierRepository.save(dossier);

            // Move to next etape using WorkflowService
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
            // Initialize workflow at AP - Phase Antenne
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
     * Delete dossier (Agent Antenne action)
     */
    @Transactional
    public DossierActionResponse deleteDossier(DeleteDossierRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Verify permission
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
     * Handle RP phase - when farmer returns with approval
     */
    @Transactional
    public DossierActionResponse startRealizationPhase(Long dossierId, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Move to RP - Phase Antenne - 1
            workflowService.moveToEtape(dossier, "RP - Phase Antenne - 1", utilisateur, 
                "Début de la phase de réalisation");

            // Update status
            dossier.setStatus(Dossier.DossierStatus.IN_REVIEW);
            dossierRepository.save(dossier);

            // Create audit trail
            dossierCommonService.createAuditTrail("DEBUT_REALISATION", dossier, utilisateur, 
                "Début de la phase de réalisation du projet");

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Phase de réalisation démarrée avec succès")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du démarrage de la phase de réalisation", e);
            throw new RuntimeException("Erreur lors du démarrage: " + e.getMessage());
        }
    }

    /**
     * Get available actions for Agent Antenne
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
                .action("start_realization")
                .label("Démarrer Réalisation")
                .icon("pi-play")
                .severity("info")
                .requiresComment(false)
                .requiresConfirmation(true)
                .description("Démarrer la phase de réalisation du projet approuvé")
                .build());

        return actions;
    }

    /**
     * Get available actions for specific dossier and antenne user
     */
    public List<AvailableActionDTO> getAvailableActionsForDossier(Dossier dossier, Utilisateur utilisateur) {
        List<AvailableActionDTO> availableActions = new ArrayList<>();
        
        try {
            // Check current etape
            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            String currentEtape = etapeInfo.getDesignation();
            
            // Can only act if user can act on current etape
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                return availableActions;
            }

            DossierPermissionsDTO permissions = dossierCommonService.calculatePermissions(dossier, utilisateur);

            // AP - Phase Antenne: Can send to GUC
            if ("AP - Phase Antenne".equals(currentEtape) && permissions.getPeutEtreEnvoye()) {
                availableActions.add(AvailableActionDTO.builder()
                        .action("send_to_guc")
                        .label("Envoyer au GUC")
                        .icon("pi-send")
                        .severity("success")
                        .requiresComment(false)
                        .requiresConfirmation(true)
                        .description("Envoyer le dossier au GUC pour validation")
                        .build());
            }

            // RP - Phase Antenne actions
            if (currentEtape.startsWith("RP - Phase Antenne")) {
                availableActions.add(AvailableActionDTO.builder()
                        .action("process_realization")
                        .label("Traiter Réalisation")
                        .icon("pi-forward")
                        .severity("info")
                        .requiresComment(false)
                        .requiresConfirmation(true)
                        .description("Avancer à l'étape suivante de réalisation")
                        .build());
            }

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des actions disponibles", e);
        }

        return availableActions;
    }

    // Private helper methods

    private boolean canSendToGUC(Dossier dossier, Utilisateur utilisateur) {
        boolean canEdit = dossier.getStatus() == Dossier.DossierStatus.DRAFT ||
                dossier.getStatus() == Dossier.DossierStatus.RETURNED_FOR_COMPLETION;
        return canEdit && utilisateur.getRole() == Utilisateur.UserRole.AGENT_ANTENNE;
    }

    private boolean canDeleteDossier(Dossier dossier, Utilisateur utilisateur) {
        return dossier.getStatus() == Dossier.DossierStatus.DRAFT &&
                utilisateur.getRole() == Utilisateur.UserRole.AGENT_ANTENNE;
    }
}