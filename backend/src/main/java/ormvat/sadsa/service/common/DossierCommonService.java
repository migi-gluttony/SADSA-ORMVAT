package ormvat.sadsa.service.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ormvat.sadsa.dto.common.DossierCommonDTOs.*;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DossierCommonService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final NoteRepository noteRepository;
    private final DocumentRequisRepository documentRequisRepository;
    private final AuditTrailRepository auditTrailRepository;
    private final HistoriqueWorkflowRepository historiqueWorkflowRepository;
    private final WorkflowService workflowService;

    /**
     * Get dossiers for user role - UPDATED to show dossiers that have passed
     * through user's phase
     */
    public List<Dossier> getDossiersForUserRole(Utilisateur utilisateur, DossierFilterRequest filterRequest) {
        switch (utilisateur.getRole()) {
            case AGENT_ANTENNE:
                // Agent Antenne can see ALL dossiers from their antenne, regardless of current
                // phase
                if (utilisateur.getAntenne() != null) {
                    return dossierRepository.findByAntenneId(utilisateur.getAntenne().getId());
                }
                return List.of();

            case AGENT_GUC:
                // Agent GUC can see all dossiers that have passed the initial antenne phase
                // (not DRAFT)
                return dossierRepository.findAll().stream()
                        .filter(d -> hasPassedAntennePhase(d))
                        .collect(Collectors.toList());

            case AGENT_COMMISSION_TERRAIN:
                // Agent Commission can see all dossiers that have reached or passed commission
                // phase
                return dossierRepository.findAll().stream()
                        .filter(d -> hasReachedCommissionPhase(d, utilisateur))
                        .collect(Collectors.toList());

            case ADMIN:
                return dossierRepository.findAll();

            default:
                return List.of();
        }
    }

    /**
     * Check if user can access dossier - UPDATED for new visibility logic
     */
    public boolean canAccessDossier(Dossier dossier, Utilisateur utilisateur) {
        switch (utilisateur.getRole()) {
            case ADMIN:
                return true;
            case AGENT_ANTENNE:
                // Can access any dossier from their antenne
                return utilisateur.getAntenne() != null &&
                        dossier.getAntenne().getId().equals(utilisateur.getAntenne().getId());
            case AGENT_GUC:
                // Can access any dossier that has passed antenne phase
                return hasPassedAntennePhase(dossier);
            case AGENT_COMMISSION_TERRAIN:
                // Can access any dossier that has reached commission phase
                return hasReachedCommissionPhase(dossier, utilisateur);
            default:
                return false;
        }
    }

    /**
     * Check if dossier has passed the initial antenne phase
     */
    private boolean hasPassedAntennePhase(Dossier dossier) {
        // A dossier has passed antenne phase if it's not in DRAFT status
        return !dossier.getStatus().equals(Dossier.DossierStatus.DRAFT);
    }

    /**
     * Check if dossier has reached commission phase
     */
    private boolean hasReachedCommissionPhase(Dossier dossier, Utilisateur utilisateur) {
        try {
            // Check if dossier has ever been at commission phase by looking at workflow
            // history
            List<HistoriqueWorkflow> history = historiqueWorkflowRepository.findByDossierId(dossier.getId());

            boolean hasBeenAtCommission = history.stream()
                    .anyMatch(h -> h.getEmplacementType() == WorkflowInstance.EmplacementType.COMMISSION_AHA_AF);

            if (hasBeenAtCommission) {
                // If it has been at commission, check team assignment
                return isAssignedToCommissionTeam(dossier, utilisateur);
            }

            // Check if currently at commission phase
            try {
                WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
                boolean currentlyAtCommission = etapeInfo
                        .getEmplacementActuel() == WorkflowInstance.EmplacementType.COMMISSION_AHA_AF;

                if (currentlyAtCommission) {
                    return isAssignedToCommissionTeam(dossier, utilisateur);
                }
            } catch (Exception e) {
                log.debug("Could not get current etape info for dossier {}", dossier.getId());
            }

            return false;
        } catch (Exception e) {
            log.error("Error checking commission phase access for dossier {}", dossier.getId(), e);
            return false;
        }
    }

    /**
     * Check if user is assigned to the appropriate commission team for this dossier
     */
    private boolean isAssignedToCommissionTeam(Dossier dossier, Utilisateur utilisateur) {
        // If user has no team assignment, they can see all commission dossiers
        if (utilisateur.getEquipeCommission() == null) {
            return true;
        }

        // Check if user's team matches the required team for this project type
        Long rubriqueId = dossier.getSousRubrique().getRubrique().getId();
        Utilisateur.EquipeCommission requiredTeam = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);

        return requiredTeam.equals(utilisateur.getEquipeCommission());
    }

    /**
     * Map to DossierSummaryDTO using workflow system
     */
    public DossierSummaryDTO mapToDossierSummaryDTO(Dossier dossier, Utilisateur currentUser) {
        List<PieceJointe> pieceJointes = pieceJointeRepository.findByDossierId(dossier.getId());
        List<DocumentRequis> documentsRequis = documentRequisRepository
                .findBySousRubriqueId(dossier.getSousRubrique().getId());

        double completionPercentage = calculateCompletionPercentage(pieceJointes, documentsRequis);
        DossierPermissionsDTO permissions = calculatePermissions(dossier, currentUser);

        int notesCount = noteRepository.findByDossierId(dossier.getId()).size();

        // Get current etape information
        WorkflowService.EtapeInfo etapeInfo = null;
        try {
            etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
        } catch (Exception e) {
            log.warn("Impossible de récupérer l'étape pour le dossier {}: {}", dossier.getId(), e.getMessage());
        }

        return DossierSummaryDTO.builder()
                .id(dossier.getId())
                .numeroDossier(dossier.getNumeroDossier())
                .saba(dossier.getSaba())
                .reference(dossier.getReference())
                .statut(getDisplayStatus(dossier.getStatus(), etapeInfo))
                .dateCreation(dossier.getDateCreation())
                .dateSubmission(dossier.getDateSubmission())
                .agriculteurNom(dossier.getAgriculteur().getNom())
                .agriculteurPrenom(dossier.getAgriculteur().getPrenom())
                .agriculteurCin(dossier.getAgriculteur().getCin())
                .agriculteurTelephone(dossier.getAgriculteur().getTelephone())
                .rubriqueDesignation(dossier.getSousRubrique().getRubrique().getDesignation())
                .sousRubriqueDesignation(dossier.getSousRubrique().getDesignation())
                .antenneDesignation(dossier.getAntenne().getDesignation())
                .cdaNom(dossier.getAntenne().getCda() != null ? dossier.getAntenne().getCda().getDescription() : "N/A")
                .montantSubvention(dossier.getMontantSubvention())
                .etapeActuelle(
                        etapeInfo != null ? getEtapeDesignationFromId(etapeInfo.getEtape().getId()) : "Non définie")
                .emplacementActuel(etapeInfo != null ? etapeInfo.getEmplacementActuel() : null)
                .joursRestants(etapeInfo != null ? etapeInfo.getJoursRestants() : 0)
                .enRetard(etapeInfo != null ? etapeInfo.getEnRetard() : false)
                .dateLimite(etapeInfo != null ? etapeInfo.getDateLimite() : null)
                .formsCompleted((int) pieceJointes.stream()
                        .filter(pj -> pj.getStatus() == PieceJointe.DocumentStatus.COMPLETE).count())
                .totalForms(documentsRequis.size())
                .completionPercentage(completionPercentage)
                .availableActions(new ArrayList<>())
                .permissions(permissions)
                .notesCount(notesCount)
                .hasUnreadNotes(false)
                .priorite("NORMALE")
                .build();
    }

    private String getEtapeDesignationFromId(Long etapeId) {
        switch (etapeId.intValue()) {
            case 1:
                return "AP - Phase Antenne (Phase 1: Création & Documents)";
            case 2:
                return "AP - Phase GUC (Phase 2: Validation GUC Initiale)";
            case 3:
                return "AP - Phase Commission Visite Terrain (Phase 3: Commission Terrain)";
            case 4:
                return "AP - Phase GUC (Phase 4: Approbation Finale GUC)";
            case 5:
                return "RP - Phase Antenne (Phase 5: Réalisation - Antenne)";
            case 6:
                return "RP - Phase GUC (Phase 6: Réalisation - GUC)";
            case 7:
                return "RP - Phase service technique (Phase 7: Réalisation - Service Technique)";
            case 8:
                return "RP - Phase GUC (Phase 8: Réalisation - GUC Final)";
            default:
                return "Phase inconnue";
        }
    }

    /**
     * Calculate dossier permissions based on user role and current etape - UPDATED
     * for Agent Antenne only
     */
    public DossierPermissionsDTO calculatePermissions(Dossier dossier, Utilisateur utilisateur) {
        boolean canUserActOnCurrentEtape = false;

        try {
            canUserActOnCurrentEtape = workflowService.canUserActOnCurrentEtape(dossier, utilisateur);
        } catch (Exception e) {
            log.warn("Impossible de vérifier les permissions pour le dossier {}: {}", dossier.getId(), e.getMessage());
        }

        switch (utilisateur.getRole()) {
            case AGENT_ANTENNE:
                // Use existing Agent Antenne logic
                return calculateAntenneAgentPermissions(dossier, utilisateur, canUserActOnCurrentEtape);

            case AGENT_GUC:
                // NEW: Use phase-based GUC permissions
                return calculateGUCAgentPermissions(dossier, utilisateur, canUserActOnCurrentEtape);

            case AGENT_COMMISSION_TERRAIN:
                // Keep existing Commission logic
                return DossierPermissionsDTO.builder()
                        .peutEtreModifie(false)
                        .peutEtreEnvoye(false)
                        .peutEtreSupprime(false)
                        .peutAjouterNotes(true)
                        .peutRetournerAntenne(false)
                        .peutEnvoyerCommission(false)
                        .peutRejeter(canUserActOnCurrentEtape)
                        .peutApprouver(canUserActOnCurrentEtape)
                        .peutVoirDocuments(true)
                        .peutTelechargerDocuments(true)
                        .build();

            case ADMIN:
                // Keep existing Admin logic
                return DossierPermissionsDTO.builder()
                        .peutEtreModifie(true)
                        .peutEtreEnvoye(true)
                        .peutEtreSupprime(true)
                        .peutAjouterNotes(true)
                        .peutRetournerAntenne(true)
                        .peutEnvoyerCommission(true)
                        .peutRejeter(true)
                        .peutApprouver(true)
                        .peutVoirDocuments(true)
                        .peutTelechargerDocuments(true)
                        .build();

            default:
                return createReadOnlyPermissions();
        }
    }

    /**
     * NEW: Calculate specific permissions for Agent GUC based on phase
     */
    private DossierPermissionsDTO calculateGUCAgentPermissions(Dossier dossier, Utilisateur utilisateur,
            boolean canUserActOnCurrentEtape) {
        // Get current etape information
        WorkflowService.EtapeInfo etapeInfo = null;
        try {
            etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
        } catch (Exception e) {
            log.warn("Cannot get etape info for GUC permissions: {}", e.getMessage());
        }

        if (etapeInfo == null || etapeInfo.getEtape() == null) {
            return createReadOnlyPermissions();
        }

        Long etapeId = etapeInfo.getEtape().getId();
        String etapeDesignation = etapeInfo.getDesignation();
        int currentOrder = etapeInfo.getOrdre();

        // PHASE 2: AP - Phase GUC (Initial Review) - etapeId=2, ordre=2
        if ("AP - Phase GUC".equals(etapeDesignation) && currentOrder == 2 && canUserActOnCurrentEtape) {
            return DossierPermissionsDTO.builder()
                    .peutEtreModifie(false) // GUC cannot modify documents
                    .peutEtreEnvoye(true) // Can send to commission
                    .peutEtreSupprime(false)
                    .peutAjouterNotes(true)
                    .peutRetournerAntenne(true)
                    .peutEnvoyerCommission(true)
                    .peutRejeter(true)
                    .peutApprouver(false) // Not final approval phase
                    .peutVoirDocuments(true)
                    .peutTelechargerDocuments(true)
                    .build();
        }

        // PHASE 3: AP - Phase AHA-AF (Commission) - READ ONLY for GUC
        else if ("AP - Phase AHA-AF".equals(etapeDesignation) && currentOrder == 3) {
            return createReadOnlyPermissions();
        }

        // PHASE 4: AP - Phase GUC (Final Approval) - etapeId=4, ordre=4
        else if ("AP - Phase GUC".equals(etapeDesignation) && currentOrder == 4 && canUserActOnCurrentEtape) {
            return DossierPermissionsDTO.builder()
                    .peutEtreModifie(false)
                    .peutEtreEnvoye(false)
                    .peutEtreSupprime(false)
                    .peutAjouterNotes(true)
                    .peutRetournerAntenne(true)
                    .peutEnvoyerCommission(false) // Already been to commission
                    .peutRejeter(true)
                    .peutApprouver(true) // FINAL APPROVAL PHASE
                    .peutVoirDocuments(true)
                    .peutTelechargerDocuments(true)
                    .build();
        }

        // PHASE 5: RP - Phase Antenne (Halt State) - READ ONLY for GUC
        else if ("RP - Phase Antenne".equals(etapeDesignation) && currentOrder == 1) {
            return createReadOnlyPermissions();
        }

        // PHASE 6: RP - Phase GUC (Realization Review) - etapeId=6, ordre=2
        else if ("RP - Phase GUC".equals(etapeDesignation) && currentOrder == 2 && canUserActOnCurrentEtape) {
            return DossierPermissionsDTO.builder()
                    .peutEtreModifie(false)
                    .peutEtreEnvoye(true) // Can send to Service Technique
                    .peutEtreSupprime(false)
                    .peutAjouterNotes(true)
                    .peutRetournerAntenne(false)
                    .peutEnvoyerCommission(false)
                    .peutRejeter(false) // Cannot reject in realization phase
                    .peutApprouver(false) // Not approval, just review
                    .peutVoirDocuments(true)
                    .peutTelechargerDocuments(true)
                    .build();
        }

        // PHASE 7: RP - Phase Service Technique - READ ONLY for GUC
        else if ("RP - Phase service technique".equals(etapeDesignation) && currentOrder == 3) {
            return createReadOnlyPermissions();
        }

        // PHASE 8: RP - Phase GUC (Final Realization) - etapeId=8, ordre=4
        else if ("RP - Phase GUC".equals(etapeDesignation) && currentOrder == 4 && canUserActOnCurrentEtape) {
            return DossierPermissionsDTO.builder()
                    .peutEtreModifie(false)
                    .peutEtreEnvoye(false)
                    .peutEtreSupprime(false)
                    .peutAjouterNotes(true)
                    .peutRetournerAntenne(false)
                    .peutEnvoyerCommission(false)
                    .peutRejeter(false)
                    .peutApprouver(true) // FINALIZE REALIZATION
                    .peutVoirDocuments(true)
                    .peutTelechargerDocuments(true)
                    .build();
        }

        // SPECIAL CASE: Approved and waiting for farmer (Halt State)
        else if (dossier.getStatus() == Dossier.DossierStatus.APPROVED_AWAITING_FARMER) {
            return createReadOnlyPermissions();
        }

        // ALL OTHER CASES: Read-only
        return createReadOnlyPermissions();
    }

    /**
     * NEW: Calculate specific permissions for Agent Antenne based on phase
     */
    private DossierPermissionsDTO calculateAntenneAgentPermissions(Dossier dossier, Utilisateur utilisateur,
            boolean canUserActOnCurrentEtape) {
        // Check if user is from correct antenne
        boolean isCorrectAntenneAgent = utilisateur.getAntenne() != null &&
                dossier.getAntenne().getId().equals(utilisateur.getAntenne().getId());

        // Agent Antenne can only act on their own antenne's dossiers
        if (!isCorrectAntenneAgent) {
            return createReadOnlyPermissions();
        }

        // Get current etape information
        WorkflowService.EtapeInfo etapeInfo = null;
        try {
            etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
        } catch (Exception e) {
            log.warn("Cannot get etape info for Agent Antenne permissions: {}", e.getMessage());
        }

        if (etapeInfo == null || etapeInfo.getEtape() == null) {
            return createReadOnlyPermissions();
        }

        Long etapeId = etapeInfo.getEtape().getId();

        // PHASE 1: AP - Phase Antenne (etapeId=1) - Full permissions
        if (etapeId == 1 && canUserActOnCurrentEtape) {
            boolean isDraftOrReturned = dossier.getStatus() == Dossier.DossierStatus.DRAFT ||
                    dossier.getStatus() == Dossier.DossierStatus.RETURNED_FOR_COMPLETION;
            boolean isDraft = dossier.getStatus() == Dossier.DossierStatus.DRAFT;

            return DossierPermissionsDTO.builder()
                    .peutEtreModifie(isDraftOrReturned)
                    .peutEtreEnvoye(isDraftOrReturned)
                    .peutEtreSupprime(isDraft)
                    .peutAjouterNotes(true)
                    .peutRetournerAntenne(false)
                    .peutEnvoyerCommission(false)
                    .peutRejeter(false)
                    .peutApprouver(false)
                    .peutVoirDocuments(true)
                    .peutTelechargerDocuments(true)
                    .build();
        }

        // PHASE 5: RP - Phase Antenne (etapeId=5) - Realization antenne phase
        else if (etapeId == 5 && canUserActOnCurrentEtape) {
            return DossierPermissionsDTO.builder()
                    .peutEtreModifie(false)
                    .peutEtreEnvoye(true) // Can send to GUC for realization
                    .peutEtreSupprime(false)
                    .peutAjouterNotes(true)
                    .peutRetournerAntenne(false)
                    .peutEnvoyerCommission(false)
                    .peutRejeter(false)
                    .peutApprouver(false)
                    .peutVoirDocuments(true)
                    .peutTelechargerDocuments(true)
                    .build();
        }

        // SPECIAL CASE: Approved and waiting for farmer (Realization initialization)
        else if (dossier.getStatus() == Dossier.DossierStatus.APPROVED_AWAITING_FARMER) {
            return DossierPermissionsDTO.builder()
                    .peutEtreModifie(false)
                    .peutEtreEnvoye(false) // Special action: initialize_realization
                    .peutEtreSupprime(false)
                    .peutAjouterNotes(true)
                    .peutRetournerAntenne(false)
                    .peutEnvoyerCommission(false)
                    .peutRejeter(false)
                    .peutApprouver(false)
                    .peutVoirDocuments(true)
                    .peutTelechargerDocuments(true)
                    .build();
        }

        // ALL OTHER PHASES: Read-only (etapeId 2,3,4,6,7,8)
        return createReadOnlyPermissions();
    }

    /**
     * Helper method for read-only permissions
     */
    private DossierPermissionsDTO createReadOnlyPermissions() {
        return DossierPermissionsDTO.builder()
                .peutEtreModifie(false)
                .peutEtreEnvoye(false)
                .peutEtreSupprime(false)
                .peutAjouterNotes(true)
                .peutRetournerAntenne(false)
                .peutEnvoyerCommission(false)
                .peutRejeter(false)
                .peutApprouver(false)
                .peutVoirDocuments(true)
                .peutTelechargerDocuments(true)
                .build();
    }

    /**
     * UPDATED: Get display status using etape information and new statuses
     */
    public String getDisplayStatus(Dossier.DossierStatus status, WorkflowService.EtapeInfo etapeInfo) {
        if (etapeInfo != null && etapeInfo.getEtape() != null) {
            String phaseInfo = "";

            // Map etape ID directly to phase number
            int phaseNumber = getPhaseNumberByEtapeId(etapeInfo.getEtape().getId());

            switch (phaseNumber) {
                case 1:
                    phaseInfo = " (Phase 1: Création & Documents)";
                    break;
                case 2:
                    phaseInfo = " (Phase 2: Validation GUC Initiale)";
                    break;
                case 3:
                    phaseInfo = " (Phase 3: Commission Terrain)";
                    break;
                case 4:
                    phaseInfo = " (Phase 4: Approbation Finale GUC)";
                    break;
                case 5:
                    phaseInfo = " (Phase 5: Réalisation - Antenne)";
                    break;
                case 6:
                    phaseInfo = " (Phase 6: Réalisation - GUC)";
                    break;
                case 7:
                    phaseInfo = " (Phase 7: Réalisation - Service Technique)";
                    break;
                case 8:
                    phaseInfo = " (Phase 8: Réalisation - GUC Final)";
                    break;
                default:
                    phaseInfo = " (Phase " + phaseNumber + ")";
            }

            return etapeInfo.getDesignation() + phaseInfo +
                    (etapeInfo.getEnRetard() ? " - EN RETARD" : "") +
                    (etapeInfo.getJoursRestants() > 0 ? " (" + etapeInfo.getJoursRestants() + "j restants)" : "");
        }

        // Fallback to status-based display
        switch (status) {
            case DRAFT:
                return "Brouillon - Phase 1";
            case SUBMITTED:
                return "Soumis au GUC - Phase 2";
            case IN_REVIEW:
                return "En cours d'examen";
            case APPROVED:
                return "Approuvé - Phase 4";
            case APPROVED_AWAITING_FARMER:
                return "Approuvé - En attente fermier";
            case REALIZATION_IN_PROGRESS:
                return "Réalisation en cours";
            case REJECTED:
                return "Rejeté";
            case COMPLETED:
                return "Terminé";
            case RETURNED_FOR_COMPLETION:
                return "Retourné pour complétion - Phase 1";
            default:
                return status.name();
        }
    }

    /**
     * Add helper method to map etape ID to phase number
     */
    public int getPhaseNumberByEtapeId(Long etapeId) {
        if (etapeId == null)
            return 0;

        // Direct mapping: etape ID = phase number
        switch (etapeId.intValue()) {
            case 1:
                return 1; // AP - Phase Antenne
            case 2:
                return 2; // AP - Phase GUC (Initial)
            case 3:
                return 3; // AP - Phase AHA-AF
            case 4:
                return 4; // AP - Phase GUC (Final)
            case 5:
                return 5; // RP - Phase Antenne
            case 6:
                return 6; // RP - Phase GUC
            case 7:
                return 7; // RP - Phase service technique
            case 8:
                return 8; // RP - Phase GUC (Final)
            default:
                return 0;
        }
    }

    /**
     * Calculate statistics using workflow system
     */
    public DossierStatisticsDTO calculateStatistics(List<Dossier> dossiers, Utilisateur utilisateur) {
        long total = dossiers.size();
        long enCours = dossiers.stream().filter(d -> d.getStatus() == Dossier.DossierStatus.IN_REVIEW ||
                d.getStatus() == Dossier.DossierStatus.REALIZATION_IN_PROGRESS).count();
        long approuves = dossiers.stream().filter(d -> d.getStatus() == Dossier.DossierStatus.APPROVED ||
                d.getStatus() == Dossier.DossierStatus.APPROVED_AWAITING_FARMER).count();
        long rejetes = dossiers.stream().filter(d -> d.getStatus() == Dossier.DossierStatus.REJECTED).count();
        long attenteTraitement = dossiers.stream().filter(d -> d.getStatus() == Dossier.DossierStatus.SUBMITTED)
                .count();

        // Count dossiers in commission phase
        long dossiersEnCommission = dossiers.stream()
                .filter(d -> {
                    try {
                        WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(d);
                        return "AP - Phase AHA-AF".equals(etapeInfo.getDesignation());
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();

        // Count overdue dossiers
        long dossiersEnRetard = dossiers.stream()
                .filter(d -> {
                    try {
                        WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(d);
                        return etapeInfo.getEnRetard();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long ceMois = dossiers.stream().filter(d -> d.getDateCreation().isAfter(startOfMonth)).count();

        double tauxCompletion = total > 0 ? (double) approuves / total * 100.0 : 0.0;

        return DossierStatisticsDTO.builder()
                .totalDossiers(total)
                .dossiersEnCours(enCours)
                .dossiersApprouves(approuves)
                .dossiersRejetes(rejetes)
                .dossiersEnRetard(dossiersEnRetard)
                .tauxCompletion(tauxCompletion)
                .dossiersCeMois(ceMois)
                .dossiersAttenteTraitement(attenteTraitement)
                .dossiersEnCommission(dossiersEnCommission)
                .build();
    }

    /**
     * Calculate completion percentage
     */
    public double calculateCompletionPercentage(List<PieceJointe> pieceJointes, List<DocumentRequis> documentsRequis) {
        if (documentsRequis.isEmpty())
            return 100.0;

        long completedDocs = pieceJointes.stream()
                .filter(pj -> pj.getStatus() == PieceJointe.DocumentStatus.COMPLETE)
                .count();

        return (double) completedDocs / documentsRequis.size() * 100.0;
    }

    /**
     * Create audit trail
     */
    public void createAuditTrail(String action, Dossier dossier, Utilisateur utilisateur, String description) {
        AuditTrail audit = new AuditTrail();
        audit.setAction(action);
        audit.setEntite("Dossier");
        audit.setEntiteId(dossier.getId());
        audit.setDateAction(LocalDateTime.now());
        audit.setUtilisateur(utilisateur);
        audit.setDescription(description);
        auditTrailRepository.save(audit);
    }

    // Rest of mapping methods remain the same
    public DossierDetailDTO mapToDossierDetailDTO(Dossier dossier) {
        return DossierDetailDTO.builder()
                .id(dossier.getId())
                .numeroDossier(dossier.getNumeroDossier())
                .saba(dossier.getSaba())
                .reference(dossier.getReference())
                .statut(dossier.getStatus().name())
                .dateCreation(dossier.getDateCreation())
                .dateSubmission(dossier.getDateSubmission())
                .dateApprobation(dossier.getDateApprobation())
                .montantSubvention(dossier.getMontantSubvention())
                .rubriqueDesignation(dossier.getSousRubrique().getRubrique().getDesignation())
                .sousRubriqueDesignation(dossier.getSousRubrique().getDesignation())
                .antenneDesignation(dossier.getAntenne().getDesignation())
                .cdaNom(dossier.getAntenne().getCda() != null ? dossier.getAntenne().getCda().getDescription() : "N/A")
                .utilisateurCreateurNom(
                        dossier.getUtilisateurCreateur().getPrenom() + " " + dossier.getUtilisateurCreateur().getNom())
                .priorite("NORMALE")
                .commentaireGUC("")
                .build();
    }

    public AgriculteurDetailDTO mapToAgriculteurDetailDTO(Agriculteur agriculteur) {
        return AgriculteurDetailDTO.builder()
                .id(agriculteur.getId())
                .nom(agriculteur.getNom())
                .prenom(agriculteur.getPrenom())
                .cin(agriculteur.getCin())
                .telephone(agriculteur.getTelephone())
                .communeRurale(
                        agriculteur.getCommuneRurale() != null ? agriculteur.getCommuneRurale().getDesignation() : null)
                .douar(agriculteur.getDouar() != null ? agriculteur.getDouar().getDesignation() : null)
                .province(agriculteur.getCommuneRurale() != null && agriculteur.getCommuneRurale().getCercle() != null
                        && agriculteur.getCommuneRurale().getCercle().getProvince() != null
                                ? agriculteur.getCommuneRurale().getCercle().getProvince().getDesignation()
                                : null)
                .cercle(agriculteur.getCommuneRurale() != null && agriculteur.getCommuneRurale().getCercle() != null
                        ? agriculteur.getCommuneRurale().getCercle().getDesignation()
                        : null)
                .build();
    }

    public List<WorkflowHistoryDTO> mapToWorkflowHistoryDTOs(List<HistoriqueWorkflow> history) {
        return history.stream()
                .map(h -> WorkflowHistoryDTO.builder()
                        .id(h.getId())
                        .etapeDesignation(h.getEtapeDesignation())
                        .emplacementType(h.getEmplacementType())
                        .dateEntree(h.getDateEntree())
                        .dateSortie(h.getDateSortie())
                        .dureeJours(h.getDureeJours())
                        .enRetard(h.getEnRetard())
                        .utilisateurNom(h.getUtilisateur() != null
                                ? h.getUtilisateur().getPrenom() + " " + h.getUtilisateur().getNom()
                                : null)
                        .commentaire(h.getCommentaire())
                        .action("action")
                        .build())
                .collect(Collectors.toList());
    }

    public List<PieceJointeDetailDTO> mapToPieceJointeDTOs(List<PieceJointe> pieceJointes, Utilisateur currentUser) {
        return pieceJointes.stream()
                .map(pj -> PieceJointeDetailDTO.builder()
                        .id(pj.getId())
                        .nomFichier(pj.getNomFichier())
                        .cheminFichier(pj.getCheminFichier())
                        .typeDocument(pj.getTypeDocument())
                        .status(pj.getStatus().name())
                        .dateUpload(pj.getDateUpload())
                        .utilisateurNom(pj.getUtilisateur() != null
                                ? pj.getUtilisateur().getPrenom() + " " + pj.getUtilisateur().getNom()
                                : null)
                        .customTitle(pj.getNomFichier())
                        .isOriginalDocument(true)
                        .canDownload(true)
                        .canDelete(currentUser.getRole() == Utilisateur.UserRole.ADMIN ||
                                currentUser.getRole() == Utilisateur.UserRole.AGENT_ANTENNE)
                        .build())
                .collect(Collectors.toList());
    }

    public List<NoteDTO> mapToNoteDTOs(List<Note> notes, Utilisateur currentUser) {
        return notes.stream()
                .map(n -> NoteDTO.builder()
                        .id(n.getId())
                        .objet(n.getObjet())
                        .contenu(n.getContenu())
                        .reponse(n.getReponse())
                        .dateCreation(n.getDateCreation())
                        .dateReponse(n.getDateReponse())
                        .typeNote(n.getTypeNote())
                        .priorite(n.getPriorite())
                        .utilisateurExpediteurNom(n.getUtilisateurExpediteur() != null
                                ? n.getUtilisateurExpediteur().getPrenom() + " " + n.getUtilisateurExpediteur().getNom()
                                : null)
                        .utilisateurDestinataireNom(
                                n.getUtilisateurDestinataire() != null
                                        ? n.getUtilisateurDestinataire().getPrenom() + " "
                                                + n.getUtilisateurDestinataire().getNom()
                                        : null)
                        .isRead(true)
                        .statut("ACTIVE")
                        .canReply(true)
                        .canEdit(n.getUtilisateurExpediteur() != null &&
                                n.getUtilisateurExpediteur().getId().equals(currentUser.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    public List<AvailableFormDTO> getAvailableForms(Long sousRubriqueId, List<PieceJointe> pieceJointes,
            Utilisateur currentUser) {
        List<DocumentRequis> documentsRequis = documentRequisRepository.findBySousRubriqueId(sousRubriqueId);

        return documentsRequis.stream()
                .map(doc -> {
                    Optional<PieceJointe> relatedPieceJointe = pieceJointes.stream()
                            .filter(pj -> pj.getDocumentRequis() != null
                                    && pj.getDocumentRequis().getId().equals(doc.getId()))
                            .findFirst();

                    boolean isReadOnly = currentUser.getRole() != Utilisateur.UserRole.AGENT_ANTENNE &&
                            currentUser.getRole() != Utilisateur.UserRole.ADMIN;

                    return AvailableFormDTO.builder()
                            .formId(doc.getId().toString())
                            .title(doc.getNomDocument())
                            .description(doc.getDescription())
                            .isCompleted(relatedPieceJointe.isPresent() &&
                                    relatedPieceJointe.get().getStatus() == PieceJointe.DocumentStatus.COMPLETE)
                            .lastModified(relatedPieceJointe.map(PieceJointe::getDateUpload).orElse(null))
                            .requiredDocuments(List.of(doc.getNomDocument()))
                            .formConfig(new HashMap<>())
                            .formData(new HashMap<>())
                            .documentType(doc.getNomDocument())
                            .isReadOnly(isReadOnly)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<String> getValidationErrors(Dossier dossier, List<PieceJointe> pieceJointes) {
        return List.of();
    }

    // Apply filters (keep existing implementation)
    public List<Dossier> applyFilters(List<Dossier> dossiers, DossierFilterRequest filter) {
        return dossiers.stream()
                .filter(d -> filter.getSearchTerm() == null ||
                        containsSearchTerm(d, filter.getSearchTerm()))
                .filter(d -> filter.getStatut() == null ||
                        d.getStatus().name().equals(filter.getStatut()))
                .filter(d -> filter.getSousRubriqueId() == null ||
                        d.getSousRubrique().getId().equals(filter.getSousRubriqueId()))
                .filter(d -> filter.getDateDebutCreation() == null ||
                        d.getDateCreation().isAfter(filter.getDateDebutCreation()))
                .filter(d -> filter.getDateFinCreation() == null ||
                        d.getDateCreation().isBefore(filter.getDateFinCreation()))
                .filter(d -> filter.getAntenneId() == null ||
                        d.getAntenne().getId().equals(filter.getAntenneId()))
                .collect(Collectors.toList());
    }

    private boolean containsSearchTerm(Dossier dossier, String searchTerm) {
        String term = searchTerm.toLowerCase();
        return (dossier.getSaba() != null && dossier.getSaba().toLowerCase().contains(term)) ||
                (dossier.getReference() != null && dossier.getReference().toLowerCase().contains(term)) ||
                (dossier.getAgriculteur().getCin() != null
                        && dossier.getAgriculteur().getCin().toLowerCase().contains(term))
                ||
                (dossier.getAgriculteur().getNom() != null
                        && dossier.getAgriculteur().getNom().toLowerCase().contains(term))
                ||
                (dossier.getAgriculteur().getPrenom() != null
                        && dossier.getAgriculteur().getPrenom().toLowerCase().contains(term));
    }
}