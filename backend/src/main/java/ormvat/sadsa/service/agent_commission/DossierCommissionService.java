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
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DossierCommissionService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final WorkflowService workflowService;
    private final DossierCommonService dossierCommonService;

    // ============================================================================
    // PHASE-BASED ACTION METHODS - SIMPLIFIED
    // ============================================================================

    /**
     * Approve terrain after inspection (Phase 3 → Phase 4)
     */
    @Transactional
    public DossierActionResponse approveTerrain(Long dossierId, String userEmail, String commentaire) {
        try {
            Utilisateur utilisateur = validateCommissionUser(userEmail);
            Dossier dossier = getDossier(dossierId);
            
            // Verify we're in Phase 3
            Long currentPhase = getCurrentPhase(dossier);
            if (currentPhase != 3L) {
                throw new RuntimeException("Approbation terrain autorisée uniquement en Phase 3 (actuelle: Phase " + currentPhase + ")");
            }

            // Check team assignment
            validateTeamAssignment(dossier, utilisateur);

            // Move to next etape (Phase 4 - final AP - Phase GUC)
            workflowService.moveToNextEtape(dossier, utilisateur, 
                "Terrain approuvé après inspection. " + commentaire);

            // Create audit trail
            dossierCommonService.createAuditTrail("APPROBATION_TERRAIN_PHASE_3", dossier, utilisateur, 
                String.format("Terrain approuvé par l'équipe %s. Commentaire: %s", 
                        getTeamDisplayName(utilisateur), commentaire));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Terrain approuvé avec succès - Dossier transféré au GUC pour approbation finale")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "fromPhase", 3,
                            "toPhase", 4,
                            "nextStep", "GUC Final Approval",
                            "equipe", getTeamDisplayName(utilisateur)
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'approbation du terrain", e);
            throw new RuntimeException("Erreur lors de l'approbation: " + e.getMessage());
        }
    }

    /**
     * Reject terrain after inspection (Phase 3 → REJECTED)
     */
    @Transactional
    public DossierActionResponse rejectTerrain(Long dossierId, String userEmail, String motif, String commentaire) {
        try {
            Utilisateur utilisateur = validateCommissionUser(userEmail);
            Dossier dossier = getDossier(dossierId);
            
            // Verify we're in Phase 3
            Long currentPhase = getCurrentPhase(dossier);
            if (currentPhase != 3L) {
                throw new RuntimeException("Rejet terrain autorisé uniquement en Phase 3 (actuelle: Phase " + currentPhase + ")");
            }

            // Check team assignment
            validateTeamAssignment(dossier, utilisateur);

            // Update dossier status to rejected
            dossier.setStatus(Dossier.DossierStatus.REJECTED);
            dossierRepository.save(dossier);

            // Create audit trail for rejection
            dossierCommonService.createAuditTrail("REJET_TERRAIN_PHASE_3", dossier, utilisateur, 
                String.format("Terrain rejeté par l'équipe %s. Motif: %s. Commentaire: %s", 
                        getTeamDisplayName(utilisateur), motif, commentaire));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Terrain rejeté - Dossier définitivement rejeté")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "rejectedFromPhase", 3,
                            "motifRejet", motif,
                            "equipe", getTeamDisplayName(utilisateur),
                            "definitif", true
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du rejet du terrain", e);
            throw new RuntimeException("Erreur lors du rejet: " + e.getMessage());
        }
    }

    /**
     * Return dossier to GUC for more information (Phase 3 → Phase 2)
     */
    @Transactional
    public DossierActionResponse returnToGUC(Long dossierId, String userEmail, String motif, String commentaire) {
        try {
            Utilisateur utilisateur = validateCommissionUser(userEmail);
            Dossier dossier = getDossier(dossierId);
            
            // Verify we're in Phase 3
            Long currentPhase = getCurrentPhase(dossier);
            if (currentPhase != 3L) {
                throw new RuntimeException("Retour au GUC autorisé uniquement depuis Phase 3 (actuelle: Phase " + currentPhase + ")");
            }

            // Check team assignment
            validateTeamAssignment(dossier, utilisateur);

            // Return to previous etape (Phase 2 - AP - Phase GUC)
            workflowService.returnToPreviousEtape(dossier, utilisateur, 
                String.format("Retour au GUC depuis Commission. Motif: %s. Commentaire: %s", motif, commentaire));

            // Update status
            dossier.setStatus(Dossier.DossierStatus.IN_REVIEW);
            dossierRepository.save(dossier);

            // Create audit trail
            dossierCommonService.createAuditTrail("RETOUR_GUC_PHASE_2", dossier, utilisateur, 
                String.format("Retour au GUC depuis Phase 3. Motif: %s. Commentaire: %s", motif, commentaire));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier retourné au GUC avec succès")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "fromPhase", 3,
                            "toPhase", 2,
                            "motifRetour", motif,
                            "equipe", getTeamDisplayName(utilisateur)
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du retour du dossier au GUC", e);
            throw new RuntimeException("Erreur lors du retour: " + e.getMessage());
        }
    }

    // ============================================================================
    // HELPER METHODS - SIMPLIFIED
    // ============================================================================

    /**
     * Validate that user is Commission agent
     */
    private Utilisateur validateCommissionUser(String userEmail) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_COMMISSION_TERRAIN)) {
            throw new RuntimeException("Seul un agent de la Commission terrain peut effectuer cette action");
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
     * Validate that user's team can work on this project type
     */
    private void validateTeamAssignment(Dossier dossier, Utilisateur utilisateur) {
        // If user has no team assignment, they can see all commission dossiers
        if (utilisateur.getEquipeCommission() == null) {
            return;
        }

        // Check if user's team matches the required team for this project type
        Long rubriqueId = dossier.getSousRubrique().getRubrique().getId();
        Utilisateur.EquipeCommission requiredTeam = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);

        if (!requiredTeam.equals(utilisateur.getEquipeCommission())) {
            throw new RuntimeException("Ce dossier n'appartient pas à votre équipe de commission (" + 
                    utilisateur.getEquipeCommission().getDisplayName() + "). " +
                    "Il nécessite l'équipe: " + requiredTeam.getDisplayName());
        }
    }

    /**
     * Get team display name for user
     */
    private String getTeamDisplayName(Utilisateur utilisateur) {
        return utilisateur.getEquipeCommission() != null ? 
                utilisateur.getEquipeCommission().getDisplayName() : "Commission";
    }

    // ============================================================================
    // AVAILABLE ACTIONS - SIMPLIFIED
    // ============================================================================

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
            // Must be Commission agent
            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_COMMISSION_TERRAIN)) {
                return availableActions;
            }

            // Can only act if user can act on current etape and it's Phase 3
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                return availableActions;
            }

            Long currentPhase = getCurrentPhase(dossier);
            if (currentPhase != 3L) {
                return availableActions;
            }

            // Check if dossier belongs to agent's team
            try {
                validateTeamAssignment(dossier, utilisateur);
            } catch (RuntimeException e) {
                // If team validation fails, no actions available
                return availableActions;
            }

            // All commission actions are available when at Phase 3 and correct team
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
}