package ormvat.sadsa.service.agent_guc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ormvat.sadsa.dto.common.DossierCommonDTOs.*;
import ormvat.sadsa.dto.agent_guc.DossierGUCActionDTOs.*;
import ormvat.sadsa.dto.agent_guc.FicheApprobationDTOs.*;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;
import ormvat.sadsa.service.common.DossierCommonService;
import ormvat.sadsa.service.common.WorkflowService;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
     * Send dossier to Commission (Phase 2 → Phase 3)
     */
    @Transactional
    public DossierActionResponse sendDossierToCommission(SendToCommissionRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_GUC)) {
                throw new RuntimeException("Seul un agent GUC peut envoyer un dossier à la Commission");
            }

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Verify we're in Phase 2 (AP - Phase GUC, ordre 2)
            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            if (!"AP - Phase GUC".equals(etapeInfo.getDesignation()) || etapeInfo.getOrdre() != 2) {
                throw new RuntimeException("Le dossier n'est pas à la phase 2 (révision initiale GUC)");
            }

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

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.IN_REVIEW);
            dossierRepository.save(dossier);

            // Move to Commission phase (Phase 3)
            workflowService.moveToNextEtape(dossier, utilisateur, 
                String.format("Envoi à la Commission (%s) pour inspection terrain. %s",
                        equipeRequise.getDisplayName(), request.getCommentaire()));

            // Create audit trail
            dossierCommonService.createAuditTrail("ENVOI_COMMISSION_PHASE_3", dossier, utilisateur, 
                String.format("Dossier envoyé à la Commission %s pour phase 3. Commentaire: %s",
                        equipeRequise.getDisplayName(), request.getCommentaire()));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier envoyé à la Commission pour inspection terrain")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "currentPhase", 3,
                            "nextPhase", "Commission Terrain",
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
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_GUC)) {
                throw new RuntimeException("Seul un agent GUC peut retourner un dossier à l'antenne");
            }

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Verify we're in a GUC phase where return is allowed
            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            if (!"AP - Phase GUC".equals(etapeInfo.getDesignation()) || 
                (etapeInfo.getOrdre() != 2 && etapeInfo.getOrdre() != 4)) {
                throw new RuntimeException("Retour à l'antenne autorisé uniquement depuis les phases 2 ou 4");
            }

            // Check if user can act on current etape
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                throw new RuntimeException("Cette action n'est pas autorisée à l'étape actuelle");
            }

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.RETURNED_FOR_COMPLETION);
            dossierRepository.save(dossier);

            // Return to Phase 1 (AP - Phase Antenne)
            workflowService.moveToEtape(dossier, "AP - Phase Antenne", utilisateur, 
                String.format("Retour à l'antenne - Motif: %s. Commentaire: %s", 
                        request.getMotif(), request.getCommentaire()));

            // Create audit trail
            dossierCommonService.createAuditTrail("RETOUR_ANTENNE_PHASE_1", dossier, utilisateur, 
                String.format("Motif: %s - %s", request.getMotif(), request.getCommentaire()));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier retourné à l'antenne pour complétion")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "currentPhase", 1,
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
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_GUC)) {
                throw new RuntimeException("Seul un agent GUC peut rejeter un dossier");
            }

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Verify we're in a GUC phase where rejection is allowed
            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            if (!"AP - Phase GUC".equals(etapeInfo.getDesignation()) || 
                (etapeInfo.getOrdre() != 2 && etapeInfo.getOrdre() != 4)) {
                throw new RuntimeException("Rejet autorisé uniquement depuis les phases 2 ou 4");
            }

            // Check if user can act on current etape
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                throw new RuntimeException("Cette action n'est pas autorisée à l'étape actuelle");
            }

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.REJECTED);
            dossierRepository.save(dossier);

            // Create audit trail
            dossierCommonService.createAuditTrail("REJET_DOSSIER_PHASE_" + etapeInfo.getOrdre(), dossier, utilisateur, 
                String.format("Rejet depuis phase %d - Motif: %s. Commentaire: %s", 
                        etapeInfo.getOrdre(), request.getMotif(), request.getCommentaire()));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier rejeté")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "rejectedFromPhase", etapeInfo.getOrdre(),
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
     * Make final approval decision with fiche generation (Phase 4)
     */
    @Transactional
    public DossierActionResponse makeFinalApprovalDecision(FinalApprovalRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_GUC)) {
                throw new RuntimeException("Seul un agent GUC peut prendre la décision finale d'approbation");
            }

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Verify we're in Phase 4 (final GUC approval phase)
            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            if (!"AP - Phase GUC".equals(etapeInfo.getDesignation()) || etapeInfo.getOrdre() != 4) {
                throw new RuntimeException("Décision finale d'approbation autorisée uniquement en phase 4");
            }

            if (request.getApproved()) {
                // APPROVAL CASE - Generate fiche d'approbation and set special status
                dossier.setStatus(Dossier.DossierStatus.APPROVED_AWAITING_FARMER);
                dossier.setDateApprobation(LocalDateTime.now());
                
                // Use provided amount or original amount
                if (request.getMontantApprouve() != null) {
                    dossier.setMontantSubvention(request.getMontantApprouve());
                }
                dossierRepository.save(dossier);

                // Generate fiche number
                String numeroFiche = generateFicheNumber(dossier);

                // Create audit trail for approval with fiche
                dossierCommonService.createAuditTrail("APPROBATION_FINALE_PHASE_4", dossier, utilisateur, 
                    String.format("Approbation finale avec génération fiche %s. Montant approuvé: %s. Commentaire: %s", 
                            numeroFiche, dossier.getMontantSubvention(), request.getCommentaireApprobation()));

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
                                "ficheGenerated", true,
                                "specialState", "AWAITING_FARMER_RETRIEVAL"
                        ))
                        .build();

            } else {
                // REJECTION CASE
                dossier.setStatus(Dossier.DossierStatus.REJECTED);
                dossierRepository.save(dossier);

                // Create audit trail for rejection
                dossierCommonService.createAuditTrail("REJET_FINAL_PHASE_4", dossier, utilisateur, 
                    String.format("Rejet final du dossier en phase 4. Motif: %s", request.getMotifRejet()));

                return DossierActionResponse.builder()
                        .success(true)
                        .message("Dossier rejeté en phase finale")
                        .newStatut(dossier.getStatus().name())
                        .dateAction(LocalDateTime.now())
                        .additionalData(Map.of(
                                "motifRejet", request.getMotifRejet(),
                                "ficheGenerated", false,
                                "phaseFinale", true
                        ))
                        .build();
            }

        } catch (Exception e) {
            log.error("Erreur lors de la décision finale d'approbation", e);
            throw new RuntimeException("Erreur lors de la décision: " + e.getMessage());
        }
    }

    /**
     * Process realization review (Phase 6 → Phase 7) 
     * Note: Phase 5 is the "halt" state where farmer brings fiche to antenne
     */
    @Transactional
    public DossierActionResponse processRealizationReview(Long dossierId, String userEmail, String commentaire) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_GUC)) {
                throw new RuntimeException("Seul un agent GUC peut traiter la révision de réalisation");
            }

            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Verify we're in Phase 6 (RP - Phase GUC, ordre 2 which corresponds to phase 6)
            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            if (!"RP - Phase GUC".equals(etapeInfo.getDesignation()) || etapeInfo.getOrdre() != 2) {
                throw new RuntimeException("Révision de réalisation autorisée uniquement en phase 6");
            }

            // Check if user can act on current etape
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                throw new RuntimeException("Cette action n'est pas autorisée à l'étape actuelle");
            }

            // Update dossier status to realization in progress
            dossier.setStatus(Dossier.DossierStatus.REALIZATION_IN_PROGRESS);
            dossierRepository.save(dossier);

            // Move to Service Technique phase (Phase 7)
            workflowService.moveToNextEtape(dossier, utilisateur, 
                String.format("Envoi au Service Technique pour phase 7. %s", commentaire));

            // Create audit trail
            dossierCommonService.createAuditTrail("ENVOI_SERVICE_TECHNIQUE_PHASE_7", dossier, utilisateur, 
                String.format("Dossier envoyé au Service Technique pour phase 7. Commentaire: %s", commentaire));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier envoyé au Service Technique")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .additionalData(Map.of(
                            "currentPhase", 7,
                            "nextPhase", "Service Technique",
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
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_GUC)) {
                throw new RuntimeException("Seul un agent GUC peut finaliser la réalisation");
            }

            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Verify we're in Phase 8 (RP - Phase GUC, ordre 4 which is 8th overall but 4th in RP)
            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            if (!"RP - Phase GUC".equals(etapeInfo.getDesignation()) || etapeInfo.getOrdre() != 4) {
                throw new RuntimeException("Finalisation autorisée uniquement en phase 8");
            }

            // Check if user can act on current etape
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                throw new RuntimeException("Cette action n'est pas autorisée à l'étape actuelle");
            }

            // Update dossier status to completed
            dossier.setStatus(Dossier.DossierStatus.COMPLETED);
            dossierRepository.save(dossier);

            // Create audit trail for completion
            dossierCommonService.createAuditTrail("FINALISATION_REALISATION_PHASE_8", dossier, utilisateur, 
                String.format("Projet finalisé et archivé en phase 8. Commentaire: %s", commentaire));

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

    /**
     * Legacy method for backward compatibility
     */
    @Transactional
    public DossierActionResponse approveDossier(Long dossierId, String userEmail, String commentaire) {
        try {
            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            FinalApprovalRequest request = FinalApprovalRequest.builder()
                    .dossierId(dossierId)
                    .approved(true)
                    .commentaireApprobation(commentaire)
                    .montantApprouve(dossier.getMontantSubvention())
                    .build();
            
            return makeFinalApprovalDecision(request, userEmail);
            
        } catch (Exception e) {
            log.error("Erreur lors de l'approbation legacy du dossier", e);
            throw new RuntimeException("Erreur lors de l'approbation: " + e.getMessage());
        }
    }

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

        // Phase 2 & 4 actions
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
                .action("final_approve")
                .label("Décision Finale d'Approbation")
                .icon("pi-check-circle")
                .severity("success")
                .requiresComment(false)
                .requiresConfirmation(true)
                .description("Prendre la décision finale d'approbation (Phase 4)")
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
            // Check current etape
            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            String currentEtape = etapeInfo.getDesignation();
            int currentOrder = etapeInfo.getOrdre();
            
            // Can only act if user can act on current etape
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                return availableActions; // Read-only phases (3, 6, 7)
            }

            // Phase-based actions for GUC
            if ("AP - Phase GUC".equals(currentEtape)) {
                if (currentOrder == 2) {
                    // Phase 2: Initial GUC review
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

                } else if (currentOrder == 4) {
                    // Phase 4: Final GUC approval
                    availableActions.add(AvailableActionDTO.builder()
                            .action("final_approve")
                            .label("Décision Finale")
                            .icon("pi-check-circle")
                            .severity("success")
                            .requiresComment(false)
                            .requiresConfirmation(true)
                            .description("Approuver ou rejeter définitivement")
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
                }
            }

            // RP Phase GUC actions
            else if ("RP - Phase GUC".equals(currentEtape)) {
                if (currentOrder == 2) {
                    // Phase 6: Realization review
                    availableActions.add(AvailableActionDTO.builder()
                            .action("process_realization_review")
                            .label("Révision Réalisation")
                            .icon("pi-eye")
                            .severity("info")
                            .requiresComment(false)
                            .requiresConfirmation(true)
                            .description("Réviser et envoyer au Service Technique")
                            .build());

                } else if (currentOrder == 4) {
                    // Phase 8: Final realization
                    availableActions.add(AvailableActionDTO.builder()
                            .action("finalize_realization")
                            .label("Finaliser Réalisation")
                            .icon("pi-check")
                            .severity("success")
                            .requiresComment(false)
                            .requiresConfirmation(true)
                            .description("Finaliser et archiver le projet")
                            .build());
                }
            }

            // Note: Phase 5 is the "halt" state where farmer brings fiche to antenne
            // GUC doesn't act in Phase 5, 3, or 7 (read-only phases)

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des actions disponibles", e);
        }

        return availableActions;
    }

    /**
     * Helper method for fiche generation
     */
    private String generateFicheNumber(Dossier dossier) {
        LocalDate today = LocalDate.now();
        String year = String.valueOf(today.getYear());
        String month = String.format("%02d", today.getMonthValue());
        
        // Format: FA-YYYY-MM-DOSSIER_ID
        return String.format("FA-%s-%s-%06d", year, month, dossier.getId());
    }
}