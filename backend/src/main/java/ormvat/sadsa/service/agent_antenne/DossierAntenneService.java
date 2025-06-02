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

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class DossierAntenneService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final HistoriqueWorkflowRepository historiqueWorkflowRepository;
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

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.SUBMITTED);
            dossier.setDateSubmission(LocalDateTime.now());
            dossierRepository.save(dossier);

            // Update workflow
            updateWorkflowForGUC(dossier, utilisateur, request.getCommentaire());

            // Create audit trail
            dossierCommonService.createAuditTrail("ENVOI_GUC", dossier, utilisateur, request.getCommentaire());

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

        return actions;
    }

    /**
     * Get available actions for specific dossier and antenne user
     */
    public List<AvailableActionDTO> getAvailableActionsForDossier(Dossier dossier, Utilisateur utilisateur) {
        List<AvailableActionDTO> allActions = getAvailableActionsForAntenne();
        DossierPermissionsDTO permissions = dossierCommonService.calculatePermissions(dossier, utilisateur);

        return allActions.stream()
                .filter(action -> {
                    switch (action.getAction()) {
                        case "send_to_guc":
                            return permissions.getPeutEtreEnvoye();
                        default:
                            return true;
                    }
                })
                .collect(java.util.stream.Collectors.toList());
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

    private void updateWorkflowForGUC(Dossier dossier, Utilisateur utilisateur, String commentaire) {
        List<WorkflowInstance> workflows = workflowInstanceRepository.findByDossierId(dossier.getId());

        if (!workflows.isEmpty()) {
            WorkflowInstance current = workflows.get(0);
            current.setEtapeDesignation("Phase GUC");
            current.setEmplacementActuel(WorkflowInstance.EmplacementType.GUC);
            current.setDateEntree(LocalDateTime.now());
            workflowInstanceRepository.save(current);
        }

        // Create history entry
        HistoriqueWorkflow history = new HistoriqueWorkflow();
        history.setDossier(dossier);
        history.setEtapeDesignation("Envoi au GUC");
        history.setEmplacementType(WorkflowInstance.EmplacementType.ANTENNE);
        history.setDateEntree(LocalDateTime.now());
        history.setUtilisateur(utilisateur);
        history.setCommentaire(commentaire);
        historiqueWorkflowRepository.save(history);
    }
}