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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class DossierGUCService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final WorkflowService workflowService;
    private final DossierCommonService dossierCommonService;

    // ============================================================================
    // PHASE-BASED ACTION METHODS - SIMPLIFIED
    // ============================================================================

    /**
     * Send dossier to Commission (Phase 2 → Phase 3)
     */
    @Transactional
    public DossierActionResponse sendDossierToCommission(SendToCommissionRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = validateGUCUser(userEmail);
            Dossier dossier = getDossier(request.getDossierId());
            
            // Verify we're in Phase 2
            Long currentPhase = getCurrentPhase(dossier);
            if (currentPhase != 2L) {
                throw new RuntimeException("Action autorisée uniquement en Phase 2 (actuelle: Phase " + currentPhase + ")");
            }

            // Determine commission team based on project type
            Long rubriqueId = dossier.getSousRubrique().getRubrique().getId();
            Utilisateur.EquipeCommission equipeRequise = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);
            
            // Find available commission agents
            List<Utilisateur> agentsCommission = utilisateurRepository.findByRoleAndEquipeCommissionAndActifTrue(
                    Utilisateur.UserRole.AGENT_COMMISSION_TERRAIN, equipeRequise);

            if (agentsCommission.isEmpty()) {
                throw new RuntimeException("Aucun agent de la " + equipeRequise.getDisplayName() + 
                        " n'est disponible pour traiter ce type de projet");
            }

            // Update dossier and move to Phase 3
            dossier.setStatus(Dossier.DossierStatus.IN_REVIEW);
            dossierRepository.save(dossier);

            workflowService.moveToNextEtape(dossier, utilisateur, 
                String.format("Envoi à la Commission (%s) pour inspection terrain. %s",
                        equipeRequise.getDisplayName(), request.getCommentaire()));

            // Create audit trail
            dossierCommonService.createAuditTrail("ENVOI_COMMISSION_PHASE_3", dossier, utilisateur, 
                String.format("Dossier envoyé à la Commission %s. Commentaire: %s",
                        equipeRequise.getDisplayName(), request.getCommentaire()));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier envoyé à la Commission pour inspection terrain")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "fromPhase", 2,
                            "toPhase", 3,
                            "nextStep", "Commission Terrain",
                            "equipeAssignee", equipeRequise.getDisplayName()
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du dossier à la commission", e);
            throw new RuntimeException("Erreur lors de l'envoi: " + e.getMessage());
        }
    }

    /**
     * Return dossier to Antenne (from Phase 2 or 4)
     */
    @Transactional
    public DossierActionResponse returnDossierToAntenne(ReturnToAntenneRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = validateGUCUser(userEmail);
            Dossier dossier = getDossier(request.getDossierId());
            
            // Verify we're in Phase 2 or 4 (GUC phases)
            Long currentPhase = getCurrentPhase(dossier);
            if (currentPhase != 2L && currentPhase != 4L) {
                throw new RuntimeException("Retour autorisé uniquement depuis Phase 2 ou 4 (actuelle: Phase " + currentPhase + ")");
            }

            // Update dossier and return to Phase 1
            dossier.setStatus(Dossier.DossierStatus.RETURNED_FOR_COMPLETION);
            dossierRepository.save(dossier);

            workflowService.moveToEtape(dossier, 1L, utilisateur, 
                String.format("Retour à l'antenne depuis Phase %d - Motif: %s. Commentaire: %s", 
                        currentPhase, request.getMotif(), request.getCommentaire()));

            // Create audit trail
            dossierCommonService.createAuditTrail("RETOUR_ANTENNE_PHASE_1", dossier, utilisateur, 
                String.format("Retour depuis Phase %d - Motif: %s - %s", 
                        currentPhase, request.getMotif(), request.getCommentaire()));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier retourné à l'antenne pour complétion")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "fromPhase", currentPhase,
                            "toPhase", 1,
                            "motifRetour", request.getMotif()
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du retour du dossier à l'antenne", e);
            throw new RuntimeException("Erreur lors du retour: " + e.getMessage());
        }
    }

    /**
     * Reject dossier (from Phase 2 or 4)
     */
    @Transactional
    public DossierActionResponse rejectDossier(RejectDossierRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = validateGUCUser(userEmail);
            Dossier dossier = getDossier(request.getDossierId());
            
            // Verify we're in Phase 2 or 4 (GUC phases)
            Long currentPhase = getCurrentPhase(dossier);
            if (currentPhase != 2L && currentPhase != 4L) {
                throw new RuntimeException("Rejet autorisé uniquement depuis Phase 2 ou 4 (actuelle: Phase " + currentPhase + ")");
            }

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.REJECTED);
            dossierRepository.save(dossier);

            // Create audit trail
            dossierCommonService.createAuditTrail("REJET_DOSSIER_PHASE_" + currentPhase, dossier, utilisateur, 
                String.format("Rejet depuis Phase %d - Motif: %s. Commentaire: %s", 
                        currentPhase, request.getMotif(), request.getCommentaire()));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier rejeté")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "rejectedFromPhase", currentPhase,
                            "motifRejet", request.getMotif(),
                            "definitif", request.getDefinitif() != null ? request.getDefinitif() : true
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du rejet du dossier", e);
            throw new RuntimeException("Erreur lors du rejet: " + e.getMessage());
        }
    }

    /**
     * Make final approval decision (Phase 4)
     */
    @Transactional
    public DossierActionResponse approveDossier(Long dossierId, String userEmail, String commentaire) {
        try {
            Utilisateur utilisateur = validateGUCUser(userEmail);
            Dossier dossier = getDossier(dossierId);
            
            // Verify we're in Phase 4 (final GUC approval phase)
            Long currentPhase = getCurrentPhase(dossier);
            if (currentPhase != 4L) {
                throw new RuntimeException("Approbation finale autorisée uniquement en Phase 4 (actuelle: Phase " + currentPhase + ")");
            }

            // Approve and set special status for farmer retrieval
            dossier.setStatus(Dossier.DossierStatus.APPROVED_AWAITING_FARMER);
            dossier.setDateApprobation(LocalDateTime.now());
            dossierRepository.save(dossier);

            // Generate fiche number
            String numeroFiche = generateFicheNumber(dossier);

            // Create audit trail
            dossierCommonService.createAuditTrail("APPROBATION_FINALE_PHASE_4", dossier, utilisateur, 
                String.format("Approbation finale avec génération fiche %s. Commentaire: %s", 
                        numeroFiche, commentaire));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier approuvé - Fiche d'approbation générée")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "phaseTerminee", "AP",
                            "numeroFiche", numeroFiche,
                            "montantApprouve", dossier.getMontantSubvention(),
                            "prochainEtape", "En attente de récupération de la fiche par l'agriculteur",
                            "specialState", "AWAITING_FARMER_RETRIEVAL"
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'approbation du dossier", e);
            throw new RuntimeException("Erreur lors de l'approbation: " + e.getMessage());
        }
    }

    /**
     * Process realization review (Phase 6 → Phase 7)
     */
    @Transactional
    public DossierActionResponse processRealizationReview(Long dossierId, String userEmail, String commentaire) {
        try {
            Utilisateur utilisateur = validateGUCUser(userEmail);
            Dossier dossier = getDossier(dossierId);
            
            // Verify we're in Phase 6
            Long currentPhase = getCurrentPhase(dossier);
            if (currentPhase != 6L) {
                throw new RuntimeException("Révision de réalisation autorisée uniquement en Phase 6 (actuelle: Phase " + currentPhase + ")");
            }

            // Update dossier and move to Phase 7
            dossier.setStatus(Dossier.DossierStatus.REALIZATION_IN_PROGRESS);
            dossierRepository.save(dossier);

            workflowService.moveToNextEtape(dossier, utilisateur, 
                String.format("Envoi au Service Technique pour Phase 7. %s", commentaire));

            // Create audit trail
            dossierCommonService.createAuditTrail("ENVOI_SERVICE_TECHNIQUE_PHASE_7", dossier, utilisateur, 
                String.format("Dossier envoyé au Service Technique. Commentaire: %s", commentaire));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier envoyé au Service Technique")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "fromPhase", 6,
                            "toPhase", 7,
                            "nextStep", "Service Technique",
                            "realizationPhase", true
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la révision de réalisation", e);
            throw new RuntimeException("Erreur lors de la révision: " + e.getMessage());
        }
    }

    /**
     * Finalize realization (Phase 8 - Final)
     */
    @Transactional
    public DossierActionResponse finalizeRealization(Long dossierId, String userEmail, String commentaire) {
        try {
            Utilisateur utilisateur = validateGUCUser(userEmail);
            Dossier dossier = getDossier(dossierId);
            
            // Verify we're in Phase 8
            Long currentPhase = getCurrentPhase(dossier);
            if (currentPhase != 8L) {
                throw new RuntimeException("Finalisation autorisée uniquement en Phase 8 (actuelle: Phase " + currentPhase + ")");
            }

            // Complete the dossier
            dossier.setStatus(Dossier.DossierStatus.COMPLETED);
            dossierRepository.save(dossier);

            // Create audit trail
            dossierCommonService.createAuditTrail("FINALISATION_REALISATION_PHASE_8", dossier, utilisateur, 
                String.format("Projet finalisé et archivé. Commentaire: %s", commentaire));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Projet finalisé et archivé")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "phaseTerminee", "RP",
                            "archived", true,
                            "finalPhase", 8,
                            "projectCompleted", true
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la finalisation de la réalisation", e);
            throw new RuntimeException("Erreur lors de la finalisation: " + e.getMessage());
        }
    }

    // ============================================================================
    // HELPER METHODS - SIMPLIFIED
    // ============================================================================

    /**
     * Validate that user is GUC agent
     */
    private Utilisateur validateGUCUser(String userEmail) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_GUC)) {
            throw new RuntimeException("Seul un agent GUC peut effectuer cette action");
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
     * Generate fiche d'approbation number
     */
    private String generateFicheNumber(Dossier dossier) {
        LocalDate today = LocalDate.now();
        String year = String.valueOf(today.getYear());
        String month = String.format("%02d", today.getMonthValue());
        
        // Format: FA-YYYY-MM-DOSSIER_ID
        return String.format("FA-%s-%s-%06d", year, month, dossier.getId());
    }

    // ============================================================================
    // AVAILABLE ACTIONS - SIMPLIFIED
    // ============================================================================

    /**
     * Get available actions for Agent GUC based on current phase
     */
    public List<AvailableActionDTO> getAvailableActionsForGUC() {
        List<AvailableActionDTO> actions = new ArrayList<>();

        // Phase 2 actions
        actions.add(AvailableActionDTO.builder()
                .action("send_to_commission")
                .label("Envoyer à la Commission (Phase 3)")
                .icon("pi-forward")
                .severity("info")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Envoyer le dossier à la Commission pour inspection terrain")
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
                .description("Rejeter le dossier")
                .build());

        // Phase 4 actions
        actions.add(AvailableActionDTO.builder()
                .action("approve")
                .label("Approuver (Phase 4)")
                .icon("pi-check-circle")
                .severity("success")
                .requiresComment(false)
                .requiresConfirmation(true)
                .description("Approbation finale avec génération de fiche")
                .build());

        // Phase 6 actions
        actions.add(AvailableActionDTO.builder()
                .action("process_realization_review")
                .label("Révision Réalisation (Phase 6)")
                .icon("pi-eye")
                .severity("info")
                .requiresComment(false)
                .requiresConfirmation(true)
                .description("Réviser et envoyer au Service Technique")
                .build());

        // Phase 8 actions
        actions.add(AvailableActionDTO.builder()
                .action("finalize_realization")
                .label("Finaliser Réalisation (Phase 8)")
                .icon("pi-check")
                .severity("success")
                .requiresComment(false)
                .requiresConfirmation(true)
                .description("Finaliser et archiver le projet")
                .build());

        return actions;
    }

    /**
     * Get available actions for specific dossier and GUC user based on current phase
     */
    public List<AvailableActionDTO> getAvailableActionsForDossier(Dossier dossier, Utilisateur utilisateur) {
        List<AvailableActionDTO> availableActions = new ArrayList<>();
        
        try {
            // Must be GUC agent
            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_GUC)) {
                return availableActions;
            }

            // Can only act if user can act on current etape
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                return availableActions;
            }

            Long currentPhase = getCurrentPhase(dossier);
            
            switch (currentPhase.intValue()) {
                case 2: // Phase 2: Initial GUC review
                    availableActions.add(AvailableActionDTO.builder()
                            .action("send_to_commission")
                            .label("Envoyer à la Commission")
                            .icon("pi-forward")
                            .severity("info")
                            .requiresComment(true)
                            .requiresConfirmation(true)
                            .description("Envoyer pour inspection terrain (Phase 3)")
                            .build());

                    availableActions.add(AvailableActionDTO.builder()
                            .action("return_to_antenne")
                            .label("Retourner à l'Antenne")
                            .icon("pi-undo")
                            .severity("warning")
                            .requiresComment(true)
                            .requiresConfirmation(true)
                            .description("Retourner pour complétion")
                            .build());

                    availableActions.add(AvailableActionDTO.builder()
                            .action("reject")
                            .label("Rejeter")
                            .icon("pi-times-circle")
                            .severity("danger")
                            .requiresComment(true)
                            .requiresConfirmation(true)
                            .description("Rejeter le dossier")
                            .build());
                    break;

                case 4: // Phase 4: Final GUC approval
                    availableActions.add(AvailableActionDTO.builder()
                            .action("approve")
                            .label("Approuver")
                            .icon("pi-check-circle")
                            .severity("success")
                            .requiresComment(false)
                            .requiresConfirmation(true)
                            .description("Approbation finale")
                            .build());

                    availableActions.add(AvailableActionDTO.builder()
                            .action("return_to_antenne")
                            .label("Retourner à l'Antenne")
                            .icon("pi-undo")
                            .severity("warning")
                            .requiresComment(true)
                            .requiresConfirmation(true)
                            .description("Retourner pour complétion")
                            .build());
                    break;

                case 6: // Phase 6: Realization review
                    availableActions.add(AvailableActionDTO.builder()
                            .action("process_realization_review")
                            .label("Révision Réalisation")
                            .icon("pi-eye")
                            .severity("info")
                            .requiresComment(false)
                            .requiresConfirmation(true)
                            .description("Réviser et envoyer au Service Technique")
                            .build());
                    break;

                case 8: // Phase 8: Final realization
                    availableActions.add(AvailableActionDTO.builder()
                            .action("finalize_realization")
                            .label("Finaliser Réalisation")
                            .icon("pi-check")
                            .severity("success")
                            .requiresComment(false)
                            .requiresConfirmation(true)
                            .description("Finaliser et archiver le projet")
                            .build());
                    break;
            }

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des actions disponibles", e);
        }

        return availableActions;
    }
}