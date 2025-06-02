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
import ormvat.sadsa.service.common.WorkflowService;

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
    private final WorkflowService workflowService;
    private final DossierCommonService dossierCommonService;

    /**
     * Send dossier to Commission AHA-AF (Agent GUC action) with team-based routing
     */
    @Transactional
    public DossierActionResponse sendDossierToCommission(SendToCommissionRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_GUC)) {
                throw new RuntimeException("Seul un agent GUC peut envoyer un dossier à la Commission AHA-AF");
            }

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Check if user can act on current etape
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                throw new RuntimeException("Cette action n'est pas autorisée à l'étape actuelle");
            }

            // Determine appropriate commission team based on project type
            Long rubriqueId = dossier.getSousRubrique().getRubrique().getId();
            Utilisateur.EquipeCommission equipeRequise = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);
            
            // Find available commission agents for this project type
            List<Utilisateur> agentsCommission = utilisateurRepository.findByRoleAndEquipeCommissionAndActifTrue(
                    Utilisateur.UserRole.AGENT_COMMISSION_TERRAIN, equipeRequise);

            if (agentsCommission.isEmpty()) {
                throw new RuntimeException("Aucun agent de la " + equipeRequise.getDisplayName() + 
                        " n'est disponible pour traiter ce type de projet");
            }

            // For now, assign to first available agent (could implement load balancing later)
            Utilisateur agentAssigne = agentsCommission.get(0);

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.IN_REVIEW);
            dossierRepository.save(dossier);

            // Move to Commission AHA-AF etape using WorkflowService
            workflowService.moveToNextEtape(dossier, utilisateur, 
                String.format("Envoi à la Commission AHA-AF (%s) - Agent assigné: %s %s. %s",
                        equipeRequise.getDisplayName(), 
                        agentAssigne.getPrenom(), 
                        agentAssigne.getNom(),
                        request.getCommentaire()));

            // Create audit trail with team information
            String commentaireDetaille = String.format("Dossier envoyé à la Commission AHA-AF - %s. Agent assigné: %s %s. Commentaire: %s",
                    equipeRequise.getDisplayName(),
                    agentAssigne.getPrenom(), 
                    agentAssigne.getNom(),
                    request.getCommentaire());
            
            dossierCommonService.createAuditTrail("ENVOI_COMMISSION_AHA_AF", dossier, utilisateur, commentaireDetaille);

            return DossierActionResponse.builder()
                    .success(true)
                    .message(String.format("Dossier envoyé à la Commission AHA-AF (%s) avec succès", 
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

            // Check if user can act on current etape
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                throw new RuntimeException("Cette action n'est pas autorisée à l'étape actuelle");
            }

            // Update dossier status to allow editing at antenne
            dossier.setStatus(Dossier.DossierStatus.RETURNED_FOR_COMPLETION);
            dossierRepository.save(dossier);

            // Return to previous etape (should be AP - Phase Antenne)
            workflowService.returnToPreviousEtape(dossier, utilisateur, 
                String.format("Retour à l'antenne - Motif: %s. Commentaire: %s", 
                        request.getMotif(), request.getCommentaire()));

            // Create audit trail
            dossierCommonService.createAuditTrail("RETOUR_ANTENNE", dossier, utilisateur, 
                String.format("Motif: %s - %s", request.getMotif(), request.getCommentaire()));

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

            // Check if user can act on current etape
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                throw new RuntimeException("Cette action n'est pas autorisée à l'étape actuelle");
            }

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.REJECTED);
            dossierRepository.save(dossier);

            // Update workflow - keep current etape but mark as rejected
            WorkflowService.EtapeInfo currentEtapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            
            // Create audit trail
            dossierCommonService.createAuditTrail("REJET_DOSSIER", dossier, utilisateur, 
                String.format("Rejet à l'étape %s - Motif: %s. Commentaire: %s", 
                        currentEtapeInfo.getDesignation(), request.getMotif(), request.getCommentaire()));

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
     * Approve dossier and generate final approval (end of AP phase)
     */
    @Transactional
    public DossierActionResponse approveDossier(Long dossierId, String userEmail, String commentaire) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_GUC)) {
                throw new RuntimeException("Seul un agent GUC peut approuver un dossier");
            }

            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Check if at final AP etape
            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            if (!"AP - Phase GUC".equals(etapeInfo.getDesignation()) || etapeInfo.getOrdre() != 4) {
                throw new RuntimeException("Le dossier n'est pas à l'étape finale d'approbation");
            }

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.APPROVED);
            dossier.setDateApprobation(LocalDateTime.now());
            dossierRepository.save(dossier);

            // Create audit trail for approval
            dossierCommonService.createAuditTrail("APPROBATION_FINALE", dossier, utilisateur, 
                "Approbation finale du dossier - Phase AP terminée. " + commentaire);

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier approuvé avec succès - Phase d'approbation terminée")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "phaseTerminee", "AP",
                            "prochainEtape", "Attente de démarrage phase RP par l'antenne"
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'approbation du dossier", e);
            throw new RuntimeException("Erreur lors de l'approbation: " + e.getMessage());
        }
    }

    /**
     * Get available actions for Agent GUC
     */
    public List<AvailableActionDTO> getAvailableActionsForGUC() {
        List<AvailableActionDTO> actions = new ArrayList<>();

        actions.add(AvailableActionDTO.builder()
                .action("send_to_commission")
                .label("Envoyer à la Commission AHA-AF")
                .icon("pi-forward")
                .severity("info")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Envoyer le dossier à la Commission AHA-AF pour inspection terrain")
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

        actions.add(AvailableActionDTO.builder()
                .action("approve")
                .label("Approuver")
                .icon("pi-check-circle")
                .severity("success")
                .requiresComment(false)
                .requiresConfirmation(true)
                .description("Approuver le dossier (fin de phase AP)")
                .build());

        return actions;
    }

    /**
     * Get available actions for specific dossier and GUC user
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

            // Actions based on current etape
            if ("AP - Phase GUC".equals(currentEtape)) {
                if (etapeInfo.getOrdre() == 2) {
                    // First GUC phase - can send to commission or return
                    if (permissions.getPeutEnvoyerCommission()) {
                        availableActions.add(AvailableActionDTO.builder()
                                .action("send_to_commission")
                                .label("Envoyer à la Commission AHA-AF")
                                .icon("pi-forward")
                                .severity("info")
                                .requiresComment(true)
                                .requiresConfirmation(true)
                                .description("Envoyer pour inspection terrain")
                                .build());
                    }
                } else if (etapeInfo.getOrdre() == 4) {
                    // Final GUC phase - can approve or reject
                    if (permissions.getPeutApprouver()) {
                        availableActions.add(AvailableActionDTO.builder()
                                .action("approve")
                                .label("Approuver")
                                .icon("pi-check-circle")
                                .severity("success")
                                .requiresComment(false)
                                .requiresConfirmation(true)
                                .description("Approuver le dossier")
                                .build());
                    }
                }

                // Common GUC actions
                if (permissions.getPeutRetournerAntenne()) {
                    availableActions.add(AvailableActionDTO.builder()
                            .action("return_to_antenne")
                            .label("Retourner à l'Antenne")
                            .icon("pi-undo")
                            .severity("warning")
                            .requiresComment(true)
                            .requiresConfirmation(true)
                            .description("Retourner pour complétion")
                            .build());
                }

                if (permissions.getPeutRejeter()) {
                    availableActions.add(AvailableActionDTO.builder()
                            .action("reject")
                            .label("Rejeter")
                            .icon("pi-times-circle")
                            .severity("danger")
                            .requiresComment(true)
                            .requiresConfirmation(true)
                            .description("Rejeter le dossier")
                            .build());
                }
            }

            // RP Phase GUC actions
            if ("RP - Phase GUC".equals(currentEtape)) {
                availableActions.add(AvailableActionDTO.builder()
                        .action("finalize_realization")
                        .label("Finaliser Réalisation")
                        .icon("pi-check")
                        .severity("success")
                        .requiresComment(false)
                        .requiresConfirmation(true)
                        .description("Finaliser la phase de réalisation")
                        .build());
            }

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des actions disponibles", e);
        }

        return availableActions;
    }
}