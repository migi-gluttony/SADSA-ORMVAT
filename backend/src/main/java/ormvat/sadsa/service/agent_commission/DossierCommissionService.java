package ormvat.sadsa.service.agent_commission;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class DossierCommissionService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final WorkflowService workflowService;
    private final DossierCommonService dossierCommonService;

    /**
     * Approve terrain after inspection
     */
    @Transactional
    public DossierActionResponse approveTerrain(Long dossierId, String userEmail, String commentaire) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_COMMISSION_TERRAIN)) {
                throw new RuntimeException("Seul un agent de la Commission AHA-AF peut approuver le terrain");
            }

            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Check if user can act on current etape
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                throw new RuntimeException("Cette action n'est pas autorisée à l'étape actuelle");
            }

            // Verify current etape is Commission AHA-AF
            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            if (!"AP - Phase AHA-AF".equals(etapeInfo.getDesignation())) {
                throw new RuntimeException("Le dossier n'est pas à l'étape Commission AHA-AF");
            }

            // Check if dossier belongs to agent's team
            if (utilisateur.getEquipeCommission() != null) {
                Long rubriqueId = dossier.getSousRubrique().getRubrique().getId();
                Utilisateur.EquipeCommission equipeRequise = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);
                if (!equipeRequise.equals(utilisateur.getEquipeCommission())) {
                    throw new RuntimeException("Ce dossier n'appartient pas à votre équipe de commission");
                }
            }

            // Move to next etape (final AP - Phase GUC)
            workflowService.moveToNextEtape(dossier, utilisateur, 
                "Terrain approuvé après inspection. " + commentaire);

            // Create audit trail
            dossierCommonService.createAuditTrail("APPROBATION_TERRAIN", dossier, utilisateur, 
                String.format("Terrain approuvé par l'équipe %s. Commentaire: %s", 
                        utilisateur.getEquipeCommission() != null ? 
                                utilisateur.getEquipeCommission().getDisplayName() : "Commission", 
                        commentaire));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Terrain approuvé avec succès - Dossier transféré au GUC pour approbation finale")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'approbation du terrain", e);
            throw new RuntimeException("Erreur lors de l'approbation: " + e.getMessage());
        }
    }

    /**
     * Reject terrain after inspection
     */
    @Transactional
    public DossierActionResponse rejectTerrain(Long dossierId, String userEmail, String motif, String commentaire) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_COMMISSION_TERRAIN)) {
                throw new RuntimeException("Seul un agent de la Commission AHA-AF peut rejeter le terrain");
            }

            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Check if user can act on current etape
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                throw new RuntimeException("Cette action n'est pas autorisée à l'étape actuelle");
            }

            // Verify current etape is Commission AHA-AF
            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            if (!"AP - Phase AHA-AF".equals(etapeInfo.getDesignation())) {
                throw new RuntimeException("Le dossier n'est pas à l'étape Commission AHA-AF");
            }

            // Check team assignment
            if (utilisateur.getEquipeCommission() != null) {
                Long rubriqueId = dossier.getSousRubrique().getRubrique().getId();
                Utilisateur.EquipeCommission equipeRequise = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);
                if (!equipeRequise.equals(utilisateur.getEquipeCommission())) {
                    throw new RuntimeException("Ce dossier n'appartient pas à votre équipe de commission");
                }
            }

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.REJECTED);
            dossierRepository.save(dossier);

            // Create audit trail for rejection
            dossierCommonService.createAuditTrail("REJET_TERRAIN", dossier, utilisateur, 
                String.format("Terrain rejeté par l'équipe %s. Motif: %s. Commentaire: %s", 
                        utilisateur.getEquipeCommission() != null ? 
                                utilisateur.getEquipeCommission().getDisplayName() : "Commission", 
                        motif, commentaire));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Terrain rejeté - Dossier définitivement rejeté")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du rejet du terrain", e);
            throw new RuntimeException("Erreur lors du rejet: " + e.getMessage());
        }
    }

    /**
     * Return dossier to GUC for more information
     */
    @Transactional
    public DossierActionResponse returnToGUC(Long dossierId, String userEmail, String motif, String commentaire) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_COMMISSION_TERRAIN)) {
                throw new RuntimeException("Seul un agent de la Commission AHA-AF peut retourner un dossier");
            }

            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Check if user can act on current etape
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                throw new RuntimeException("Cette action n'est pas autorisée à l'étape actuelle");
            }

            // Return to previous etape (AP - Phase GUC)
            workflowService.returnToPreviousEtape(dossier, utilisateur, 
                String.format("Retour du terrain à GUC. Motif: %s. Commentaire: %s", motif, commentaire));

            // Update status
            dossier.setStatus(Dossier.DossierStatus.RETURNED_FOR_COMPLETION);
            dossierRepository.save(dossier);

            // Create audit trail
            dossierCommonService.createAuditTrail("RETOUR_GUC_COMMISSION", dossier, utilisateur, 
                String.format("Retour au GUC depuis la Commission. Motif: %s. Commentaire: %s", motif, commentaire));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier retourné au GUC avec succès")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du retour du dossier au GUC", e);
            throw new RuntimeException("Erreur lors du retour: " + e.getMessage());
        }
    }

    /**
     * Get available actions for Commission agents
     */
    public List<AvailableActionDTO> getAvailableActionsForCommission() {
        List<AvailableActionDTO> actions = new ArrayList<>();

        actions.add(AvailableActionDTO.builder()
                .action("approve_terrain")
                .label("Approuver Terrain")
                .icon("pi-check-circle")
                .severity("success")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Approuver la conformité du terrain après visite")
                .build());

        actions.add(AvailableActionDTO.builder()
                .action("reject_terrain")
                .label("Rejeter Terrain")
                .icon("pi-times-circle")
                .severity("danger")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Rejeter la demande suite à non-conformité du terrain")
                .build());

        actions.add(AvailableActionDTO.builder()
                .action("return_to_guc")
                .label("Retourner au GUC")
                .icon("pi-undo")
                .severity("warning")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Retourner au GUC pour complément d'information")
                .build());

        return actions;
    }

    /**
     * Get available actions for specific dossier and commission user
     */
    public List<AvailableActionDTO> getAvailableActionsForDossier(Dossier dossier, Utilisateur utilisateur) {
        List<AvailableActionDTO> availableActions = new ArrayList<>();
        
        try {
            // Check current etape
            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            String currentEtape = etapeInfo.getDesignation();
            
            // Can only act if user can act on current etape and it's Commission phase
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur) || 
                !"AP - Phase AHA-AF".equals(currentEtape)) {
                return availableActions;
            }

            // Check if dossier belongs to agent's team
            if (utilisateur.getEquipeCommission() != null) {
                Long rubriqueId = dossier.getSousRubrique().getRubrique().getId();
                Utilisateur.EquipeCommission equipeRequise = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);
                if (!equipeRequise.equals(utilisateur.getEquipeCommission())) {
                    return availableActions; // No actions if not in correct team
                }
            }

            // All commission actions are available when at correct etape and team
            availableActions.add(AvailableActionDTO.builder()
                    .action("approve_terrain")
                    .label("Approuver Terrain")
                    .icon("pi-check-circle")
                    .severity("success")
                    .requiresComment(true)
                    .requiresConfirmation(true)
                    .description("Approuver la conformité du terrain")
                    .build());

            availableActions.add(AvailableActionDTO.builder()
                    .action("reject_terrain")
                    .label("Rejeter Terrain")
                    .icon("pi-times-circle")
                    .severity("danger")
                    .requiresComment(true)
                    .requiresConfirmation(true)
                    .description("Rejeter pour non-conformité")
                    .build());

            availableActions.add(AvailableActionDTO.builder()
                    .action("return_to_guc")
                    .label("Retourner au GUC")
                    .icon("pi-undo")
                    .severity("warning")
                    .requiresComment(true)
                    .requiresConfirmation(true)
                    .description("Retourner pour complément")
                    .build());

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des actions disponibles", e);
        }

        return availableActions;
    }

    /**
     * Get dossiers assigned to commission agent's team
     */
    public List<Dossier> getDossiersForCommissionAgent(Utilisateur utilisateur) {
        if (utilisateur.getEquipeCommission() == null) {
            // Agent not assigned to specific team - can see all commission dossiers
            return dossierRepository.findAll().stream()
                    .filter(d -> {
                        try {
                            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(d);
                            return "AP - Phase AHA-AF".equals(etapeInfo.getDesignation());
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());
        }

        // Filter by team assignment
        return dossierRepository.findAll().stream()
                .filter(d -> {
                    try {
                        WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(d);
                        if (!"AP - Phase AHA-AF".equals(etapeInfo.getDesignation())) {
                            return false;
                        }
                        
                        Long rubriqueId = d.getSousRubrique().getRubrique().getId();
                        Utilisateur.EquipeCommission equipeRequise = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);
                        return equipeRequise.equals(utilisateur.getEquipeCommission());
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(java.util.stream.Collectors.toList());
    }
}