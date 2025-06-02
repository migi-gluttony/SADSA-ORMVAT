package ormvat.sadsa.service.agent_guc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ormvat.sadsa.dto.common.DossierCommonDTOs.*;
import ormvat.sadsa.dto.agent_guc.DossierGUCActionDTOs.*;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;
import ormvat.sadsa.service.common.DossierCommonService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DossierGUCService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final HistoriqueWorkflowRepository historiqueWorkflowRepository;
    private final DossierCommonService dossierCommonService;

    /**
     * Send dossier to Commission (Agent GUC action) with team-based routing
     */
    @Transactional
    public DossierActionResponse sendDossierToCommission(SendToCommissionRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_GUC)) {
                throw new RuntimeException("Seul un agent GUC peut envoyer un dossier à la Commission de Vérification Terrain");
            }

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Determine appropriate commission team based on project type
            Long rubriqueId = dossier.getSousRubrique().getRubrique().getId();
            Utilisateur.EquipeCommission equipeRequise = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);
            
            // Find available commission agents for this project type
            List<Utilisateur> agentsCommission = utilisateurRepository.findByRole(Utilisateur.UserRole.AGENT_COMMISSION_TERRAIN)
                    .stream()
                    .filter(agent -> agent.getEquipeCommission() == equipeRequise)
                    .filter(agent -> agent.getActif())
                    .collect(Collectors.toList());

            if (agentsCommission.isEmpty()) {
                throw new RuntimeException("Aucun agent de la " + equipeRequise.getDisplayName() + 
                        " n'est disponible pour traiter ce type de projet");
            }

            // For now, assign to first available agent (could implement load balancing later)
            Utilisateur agentAssigne = agentsCommission.get(0);

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.IN_REVIEW);
            dossierRepository.save(dossier);

            // Update workflow to Commission with team info
            updateWorkflowForCommission(dossier, utilisateur, request, agentAssigne, equipeRequise);

            // Create audit trail with team information
            String commentaireDetaille = String.format("Dossier envoyé à la Commission de Vérification Terrain - %s. Agent assigné: %s %s. Commentaire: %s",
                    equipeRequise.getDisplayName(),
                    agentAssigne.getPrenom(), 
                    agentAssigne.getNom(),
                    request.getCommentaire());
            
            dossierCommonService.createAuditTrail("ENVOI_COMMISSION_TERRAIN", dossier, utilisateur, commentaireDetaille);

            return DossierActionResponse.builder()
                    .success(true)
                    .message(String.format("Dossier envoyé à la Commission de Vérification Terrain (%s) avec succès", 
                            equipeRequise.getDisplayName()))
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "equipeAssignee", equipeRequise.getDisplayName(),
                            "agentAssigne", agentAssigne.getPrenom() + " " + agentAssigne.getNom(),
                            "typeProjet", dossier.getSousRubrique().getRubrique().getDesignation()
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du dossier à la commission", e);
            throw new RuntimeException("Erreur lors de l'envoi: " + e.getMessage());
        }
    }

    /**
     * Return dossier to Antenne (Agent GUC action)
     */
    @Transactional
    public DossierActionResponse returnDossierToAntenne(ReturnToAntenneRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_GUC)) {
                throw new RuntimeException("Seul un agent GUC peut retourner un dossier à l'antenne");
            }

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Update dossier status to allow editing at antenne
            dossier.setStatus(Dossier.DossierStatus.RETURNED_FOR_COMPLETION);
            dossierRepository.save(dossier);

            // Update workflow back to Antenne
            updateWorkflowBackToAntenne(dossier, utilisateur, request);

            // Create audit trail
            dossierCommonService.createAuditTrail("RETOUR_ANTENNE", dossier, utilisateur, request.getCommentaire());

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier retourné à l'antenne avec succès")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du retour du dossier à l'antenne", e);
            throw new RuntimeException("Erreur lors du retour: " + e.getMessage());
        }
    }

    /**
     * Reject dossier (Agent GUC action)
     */
    @Transactional
    public DossierActionResponse rejectDossier(RejectDossierRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_GUC)) {
                throw new RuntimeException("Seul un agent GUC peut rejeter un dossier");
            }

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.REJECTED);
            dossierRepository.save(dossier);

            // Update workflow
            updateWorkflowForRejection(dossier, utilisateur, request);

            // Create audit trail
            dossierCommonService.createAuditTrail("REJET_DOSSIER", dossier, utilisateur, request.getCommentaire());

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier rejeté avec succès")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du rejet du dossier", e);
            throw new RuntimeException("Erreur lors du rejet: " + e.getMessage());
        }
    }

    /**
     * Get available actions for Agent GUC
     */
    public List<AvailableActionDTO> getAvailableActionsForGUC() {
        List<AvailableActionDTO> actions = new ArrayList<>();

        actions.add(AvailableActionDTO.builder()
                .action("send_to_commission")
                .label("Envoyer à la Commission")
                .icon("pi-forward")
                .severity("info")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Envoyer le dossier à la Commission de Vérification Terrain")
                .build());

        actions.add(AvailableActionDTO.builder()
                .action("return_to_antenne")
                .label("Retourner à l'Antenne")
                .icon("pi-undo")
                .severity("warning")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Retourner le dossier à l'antenne pour complétion")
                .build());

        actions.add(AvailableActionDTO.builder()
                .action("reject")
                .label("Rejeter")
                .icon("pi-times-circle")
                .severity("danger")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Rejeter définitivement le dossier")
                .build());

        return actions;
    }

    /**
     * Get available actions for specific dossier and GUC user
     */
    public List<AvailableActionDTO> getAvailableActionsForDossier(Dossier dossier, Utilisateur utilisateur) {
        List<AvailableActionDTO> allActions = getAvailableActionsForGUC();
        DossierPermissionsDTO permissions = dossierCommonService.calculatePermissions(dossier, utilisateur);

        return allActions.stream()
                .filter(action -> {
                    switch (action.getAction()) {
                        case "send_to_commission":
                            return permissions.getPeutEnvoyerCommission();
                        case "return_to_antenne":
                            return permissions.getPeutRetournerAntenne();
                        case "reject":
                            return permissions.getPeutRejeter();
                        default:
                            return true;
                    }
                })
                .collect(Collectors.toList());
    }

    // Private helper methods

    private void updateWorkflowForCommission(Dossier dossier, Utilisateur utilisateur,
            SendToCommissionRequest request, Utilisateur agentAssigne, Utilisateur.EquipeCommission equipe) {
        List<WorkflowInstance> workflows = workflowInstanceRepository.findByDossierId(dossier.getId());

        if (!workflows.isEmpty()) {
            WorkflowInstance current = workflows.get(0);
            current.setEtapeDesignation("Commission Vérification Terrain - " + equipe.getDisplayName());
            current.setEmplacementActuel(WorkflowInstance.EmplacementType.COMMISSION_AHA_AF);
            current.setDateEntree(LocalDateTime.now());
            workflowInstanceRepository.save(current);
        }

        HistoriqueWorkflow history = new HistoriqueWorkflow();
        history.setDossier(dossier);
        history.setEtapeDesignation("Envoi Commission Vérification Terrain - " + equipe.getDisplayName());
        history.setEmplacementType(WorkflowInstance.EmplacementType.GUC);
        history.setDateEntree(LocalDateTime.now());
        history.setUtilisateur(utilisateur);
        history.setCommentaire(String.format("%s (Assigné à: %s %s)", 
                request.getCommentaire(), agentAssigne.getPrenom(), agentAssigne.getNom()));
        historiqueWorkflowRepository.save(history);
    }

    private void updateWorkflowBackToAntenne(Dossier dossier, Utilisateur utilisateur, ReturnToAntenneRequest request) {
        List<WorkflowInstance> workflows = workflowInstanceRepository.findByDossierId(dossier.getId());

        if (!workflows.isEmpty()) {
            WorkflowInstance current = workflows.get(0);
            current.setEtapeDesignation("Phase Antenne - Complétion");
            current.setEmplacementActuel(WorkflowInstance.EmplacementType.ANTENNE);
            current.setDateEntree(LocalDateTime.now());
            workflowInstanceRepository.save(current);
        }

        HistoriqueWorkflow history = new HistoriqueWorkflow();
        history.setDossier(dossier);
        history.setEtapeDesignation("Retour à l'Antenne");
        history.setEmplacementType(WorkflowInstance.EmplacementType.GUC);
        history.setDateEntree(LocalDateTime.now());
        history.setUtilisateur(utilisateur);
        history.setCommentaire(request.getCommentaire());
        historiqueWorkflowRepository.save(history);
    }

    private void updateWorkflowForRejection(Dossier dossier, Utilisateur utilisateur, RejectDossierRequest request) {
        List<WorkflowInstance> workflows = workflowInstanceRepository.findByDossierId(dossier.getId());

        if (!workflows.isEmpty()) {
            WorkflowInstance current = workflows.get(0);
            current.setEtapeDesignation("Rejeté");
            current.setEmplacementActuel(WorkflowInstance.EmplacementType.GUC);
            current.setDateEntree(LocalDateTime.now());
            workflowInstanceRepository.save(current);
        }

        HistoriqueWorkflow history = new HistoriqueWorkflow();
        history.setDossier(dossier);
        history.setEtapeDesignation("Rejet du dossier");
        history.setEmplacementType(WorkflowInstance.EmplacementType.GUC);
        history.setDateEntree(LocalDateTime.now());
        history.setUtilisateur(utilisateur);
        history.setCommentaire(request.getCommentaire());
        historiqueWorkflowRepository.save(history);
    }
}