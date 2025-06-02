package ormvat.sadsa.service.admin;

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
public class DossierAdminService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final WorkflowService workflowService;
    private final DossierCommonService dossierCommonService;

    /**
     * Force move dossier to specific etape (Admin only)
     */
    @Transactional
    public DossierActionResponse forceEtapeChange(Long dossierId, String etapeDesignation, String userEmail, String reason) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
                throw new RuntimeException("Seul un administrateur peut forcer le changement d'étape");
            }

            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Force move to specified etape
            workflowService.moveToEtape(dossier, etapeDesignation, utilisateur, 
                "Changement forcé par admin - Raison: " + reason);

            // Create audit trail
            dossierCommonService.createAuditTrail("CHANGEMENT_ETAPE_FORCE", dossier, utilisateur, 
                String.format("Changement forcé vers l'étape %s. Raison: %s", etapeDesignation, reason));

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Étape changée avec succès vers: " + etapeDesignation)
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du changement forcé d'étape", e);
            throw new RuntimeException("Erreur lors du changement: " + e.getMessage());
        }
    }

    /**
     * Reset workflow to beginning (Admin only)
     */
    @Transactional
    public DossierActionResponse resetWorkflow(Long dossierId, String userEmail, String reason) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
                throw new RuntimeException("Seul un administrateur peut réinitialiser le workflow");
            }

            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Reset to first etape
            workflowService.moveToEtape(dossier, "AP - Phase Antenne", utilisateur, 
                "Réinitialisation du workflow par admin - Raison: " + reason);

            // Reset dossier status
            dossier.setStatus(Dossier.DossierStatus.DRAFT);
            dossierRepository.save(dossier);

            // Create audit trail
            dossierCommonService.createAuditTrail("RESET_WORKFLOW", dossier, utilisateur, 
                "Réinitialisation complète du workflow. Raison: " + reason);

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Workflow réinitialisé avec succès")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la réinitialisation du workflow", e);
            throw new RuntimeException("Erreur lors de la réinitialisation: " + e.getMessage());
        }
    }

    /**
     * Force approve dossier (Admin only)
     */
    @Transactional
    public DossierActionResponse forceApproveDossier(Long dossierId, String userEmail, String reason) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
                throw new RuntimeException("Seul un administrateur peut forcer l'approbation");
            }

            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.APPROVED);
            dossier.setDateApprobation(LocalDateTime.now());
            dossierRepository.save(dossier);

            // Create audit trail
            dossierCommonService.createAuditTrail("APPROBATION_FORCEE", dossier, utilisateur, 
                "Approbation forcée par administrateur. Raison: " + reason);

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier approuvé avec succès (force)")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'approbation forcée", e);
            throw new RuntimeException("Erreur lors de l'approbation: " + e.getMessage());
        }
    }

    /**
     * Force reject dossier (Admin only)
     */
    @Transactional
    public DossierActionResponse forceRejectDossier(Long dossierId, String userEmail, String reason) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
                throw new RuntimeException("Seul un administrateur peut forcer le rejet");
            }

            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.REJECTED);
            dossierRepository.save(dossier);

            // Create audit trail
            dossierCommonService.createAuditTrail("REJET_FORCE", dossier, utilisateur, 
                "Rejet forcé par administrateur. Raison: " + reason);

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier rejeté avec succès (force)")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du rejet forcé", e);
            throw new RuntimeException("Erreur lors du rejet: " + e.getMessage());
        }
    }

    /**
     * Get available actions for Admin
     */
    public List<AvailableActionDTO> getAvailableActionsForAdmin() {
        List<AvailableActionDTO> actions = new ArrayList<>();

        actions.add(AvailableActionDTO.builder()
                .action("force_etape_change")
                .label("Changer Étape (Force)")
                .icon("pi-cog")
                .severity("secondary")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Forcer le changement vers une étape spécifique")
                .build());

        actions.add(AvailableActionDTO.builder()
                .action("reset_workflow")
                .label("Réinitialiser Workflow")
                .icon("pi-refresh")
                .severity("warning")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Réinitialiser le workflow au début")
                .build());

        actions.add(AvailableActionDTO.builder()
                .action("force_approve")
                .label("Approuver (Force)")
                .icon("pi-check-circle")
                .severity("success")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Forcer l'approbation du dossier")
                .build());

        actions.add(AvailableActionDTO.builder()
                .action("force_reject")
                .label("Rejeter (Force)")
                .icon("pi-times-circle")
                .severity("danger")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Forcer le rejet du dossier")
                .build());

        return actions;
    }

    /**
     * Get available actions for specific dossier and admin user
     */
    public List<AvailableActionDTO> getAvailableActionsForDossier(Dossier dossier, Utilisateur utilisateur) {
        if (!utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
            return new ArrayList<>();
        }

        // Admin can perform all actions on any dossier
        return getAvailableActionsForAdmin();
    }

    /**
     * Get workflow statistics for admin dashboard
     */
    public WorkflowStatisticsDTO getWorkflowStatistics() {
        try {
            List<Dossier> allDossiers = dossierRepository.findAll();
            
            long totalDossiers = allDossiers.size();
            long dossiersAP = 0;
            long dossiersRP = 0;
            long dossiersEnRetard = 0;
            long dossiersAntenne = 0;
            long dossiersGUC = 0;
            long dossiersCommission = 0;

            for (Dossier dossier : allDossiers) {
                try {
                    WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
                    
                    if ("AP".equals(etapeInfo.getPhase())) {
                        dossiersAP++;
                    } else if ("RP".equals(etapeInfo.getPhase())) {
                        dossiersRP++;
                    }
                    
                    if (etapeInfo.getEnRetard()) {
                        dossiersEnRetard++;
                    }
                    
                    switch (etapeInfo.getEmplacementActuel()) {
                        case ANTENNE:
                            dossiersAntenne++;
                            break;
                        case GUC:
                            dossiersGUC++;
                            break;
                        case COMMISSION_AHA_AF:
                            dossiersCommission++;
                            break;
                    }
                } catch (Exception e) {
                    // Skip dossiers without workflow info
                }
            }

            return WorkflowStatisticsDTO.builder()
                    .totalDossiers(totalDossiers)
                    .dossiersPhaseAP(dossiersAP)
                    .dossiersPhaseRP(dossiersRP)
                    .dossiersEnRetard(dossiersEnRetard)
                    .dossiersAntenne(dossiersAntenne)
                    .dossiersGUC(dossiersGUC)
                    .dossiersCommission(dossiersCommission)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du calcul des statistiques workflow", e);
            throw new RuntimeException("Erreur lors du calcul des statistiques: " + e.getMessage());
        }
    }

    // DTO for workflow statistics
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class WorkflowStatisticsDTO {
        private Long totalDossiers;
        private Long dossiersPhaseAP;
        private Long dossiersPhaseRP;
        private Long dossiersEnRetard;
        private Long dossiersAntenne;
        private Long dossiersGUC;
        private Long dossiersCommission;
    }
}