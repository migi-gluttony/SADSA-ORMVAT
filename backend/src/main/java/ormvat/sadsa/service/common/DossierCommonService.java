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

    // ============================================================================
    // DOSSIER VISIBILITY LOGIC - SIMPLIFIED
    // ============================================================================

    /**
     * Get dossiers for user role - SIMPLIFIED: Users see dossiers based on their role
     */
    public List<Dossier> getDossiersForUserRole(Utilisateur utilisateur, DossierFilterRequest filterRequest) {
        switch (utilisateur.getRole()) {
            case AGENT_ANTENNE:
                // Agent Antenne: Only their antenne's dossiers
                if (utilisateur.getAntenne() != null) {
                    return dossierRepository.findByAntenneId(utilisateur.getAntenne().getId());
                }
                return List.of();

            case AGENT_GUC:
                // Agent GUC: All dossiers (they coordinate everything)
                return dossierRepository.findAll();

            case AGENT_COMMISSION_TERRAIN:
                // Agent Commission: All dossiers (they might inspect any project)
                return dossierRepository.findAll();

            case SERVICE_TECHNIQUE:
                // Service Technique: All dossiers (they might work on any infrastructure project)
                return dossierRepository.findAll();

            case ADMIN:
                // Admin: All dossiers
                return dossierRepository.findAll();

            default:
                return List.of();
        }
    }

    /**
     * Check if user can access dossier - SIMPLIFIED
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
            case AGENT_COMMISSION_TERRAIN:
            case SERVICE_TECHNIQUE:
                // These roles can see all dossiers for coordination purposes
                return true;
                
            default:
                return false;
        }
    }

    // ============================================================================
    // DOSSIER SUMMARY MAPPING - ID-BASED
    // ============================================================================

    /**
     * Map to DossierSummaryDTO using ID-based workflow system
     */
    public DossierSummaryDTO mapToDossierSummaryDTO(Dossier dossier, Utilisateur currentUser) {
        List<PieceJointe> pieceJointes = pieceJointeRepository.findByDossierId(dossier.getId());
        List<DocumentRequis> documentsRequis = documentRequisRepository
                .findBySousRubriqueId(dossier.getSousRubrique().getId());

        double completionPercentage = calculateCompletionPercentage(pieceJointes, documentsRequis);
        DossierPermissionsDTO permissions = calculatePermissions(dossier, currentUser);

        int notesCount = noteRepository.findByDossierId(dossier.getId()).size();

        // Get current etape information using ID-based logic
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
                .etapeActuelle(etapeInfo != null ? etapeInfo.getDesignation() : "Non définie")
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

    // ============================================================================
    // PERMISSION CALCULATION - SIMPLIFIED ID-BASED
    // ============================================================================

    /**
     * Calculate dossier permissions based on user role and current etape ID
     */
    public DossierPermissionsDTO calculatePermissions(Dossier dossier, Utilisateur utilisateur) {
        // Get current etape ID
        Long currentEtapeId = null;
        try {
            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            currentEtapeId = etapeInfo.getEtapeId();
        } catch (Exception e) {
            log.warn("Cannot get etape info for permissions: {}", e.getMessage());
            return createReadOnlyPermissions();
        }

        // Check if user can act on current etape
        boolean canUserAct = workflowService.canUserActOnCurrentEtape(dossier, utilisateur);

        switch (utilisateur.getRole()) {
            case ADMIN:
                return createAdminPermissions();

            case AGENT_ANTENNE:
                return calculateAntenneAgentPermissions(dossier, utilisateur, currentEtapeId, canUserAct);

            case AGENT_GUC:
                return calculateGUCAgentPermissions(dossier, utilisateur, currentEtapeId, canUserAct);

            case AGENT_COMMISSION_TERRAIN:
                return calculateCommissionAgentPermissions(dossier, utilisateur, currentEtapeId, canUserAct);

            case SERVICE_TECHNIQUE:
                return calculateServiceTechniquePermissions(dossier, utilisateur, currentEtapeId, canUserAct);

            default:
                return createReadOnlyPermissions();
        }
    }

    /**
     * Calculate Agent Antenne permissions based on current phase
     */
    private DossierPermissionsDTO calculateAntenneAgentPermissions(Dossier dossier, Utilisateur utilisateur, 
                                                                  Long currentEtapeId, boolean canUserAct) {
        // Must be from correct antenne
        if (utilisateur.getAntenne() == null || 
            !dossier.getAntenne().getId().equals(utilisateur.getAntenne().getId())) {
            return createReadOnlyPermissions();
        }

        if (currentEtapeId == null) return createReadOnlyPermissions();

        switch (currentEtapeId.intValue()) {
            case 1: // Phase 1: AP - Phase Antenne
                if (canUserAct) {
                    boolean canModify = dossier.getStatus() == Dossier.DossierStatus.DRAFT ||
                                      dossier.getStatus() == Dossier.DossierStatus.RETURNED_FOR_COMPLETION;
                    boolean canDelete = dossier.getStatus() == Dossier.DossierStatus.DRAFT;
                    
                    return DossierPermissionsDTO.builder()
                            .peutEtreModifie(canModify)
                            .peutEtreEnvoye(canModify)
                            .peutEtreSupprime(canDelete)
                            .peutAjouterNotes(true)
                            .peutRetournerAntenne(false)
                            .peutEnvoyerCommission(false)
                            .peutRejeter(false)
                            .peutApprouver(false)
                            .peutVoirDocuments(true)
                            .peutTelechargerDocuments(true)
                            .build();
                }
                break;

            case 5: // Phase 5: RP - Phase Antenne
                if (canUserAct) {
                    return DossierPermissionsDTO.builder()
                            .peutEtreModifie(false)
                            .peutEtreEnvoye(true)
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
                break;
        }

        // For all other phases, antenne has read-only access
        return createReadOnlyPermissions();
    }

    /**
     * Calculate Agent GUC permissions based on current phase
     */
    private DossierPermissionsDTO calculateGUCAgentPermissions(Dossier dossier, Utilisateur utilisateur, 
                                                              Long currentEtapeId, boolean canUserAct) {
        if (currentEtapeId == null || !canUserAct) return createReadOnlyPermissions();

        switch (currentEtapeId.intValue()) {
            case 2: // Phase 2: AP - Phase GUC (Initial Review)
                return DossierPermissionsDTO.builder()
                        .peutEtreModifie(false)
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

            case 4: // Phase 4: AP - Phase GUC (Final Approval)
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

            case 6: // Phase 6: RP - Phase GUC (Realization Review)
                return DossierPermissionsDTO.builder()
                        .peutEtreModifie(false)
                        .peutEtreEnvoye(true) // Can send to Service Technique
                        .peutEtreSupprime(false)
                        .peutAjouterNotes(true)
                        .peutRetournerAntenne(false)
                        .peutEnvoyerCommission(false)
                        .peutRejeter(false)
                        .peutApprouver(false)
                        .peutVoirDocuments(true)
                        .peutTelechargerDocuments(true)
                        .build();

            case 8: // Phase 8: RP - Phase GUC (Final Realization)
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

        return createReadOnlyPermissions();
    }

    /**
     * Calculate Commission Agent permissions
     */
    private DossierPermissionsDTO calculateCommissionAgentPermissions(Dossier dossier, Utilisateur utilisateur, 
                                                                     Long currentEtapeId, boolean canUserAct) {
        if (currentEtapeId == null || !canUserAct || currentEtapeId != 3) {
            return createReadOnlyPermissions();
        }

        // Check if dossier belongs to agent's team
        if (utilisateur.getEquipeCommission() != null) {
            Long rubriqueId = dossier.getSousRubrique().getRubrique().getId();
            Utilisateur.EquipeCommission equipeRequise = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);
            if (!equipeRequise.equals(utilisateur.getEquipeCommission())) {
                return createReadOnlyPermissions();
            }
        }

        // Phase 3: Commission terrain inspection
        return DossierPermissionsDTO.builder()
                .peutEtreModifie(false)
                .peutEtreEnvoye(false)
                .peutEtreSupprime(false)
                .peutAjouterNotes(true)
                .peutRetournerAntenne(false)
                .peutEnvoyerCommission(false)
                .peutRejeter(true)
                .peutApprouver(true)
                .peutVoirDocuments(true)
                .peutTelechargerDocuments(true)
                .build();
    }

    /**
     * Calculate Service Technique permissions
     */
    private DossierPermissionsDTO calculateServiceTechniquePermissions(Dossier dossier, Utilisateur utilisateur, 
                                                                      Long currentEtapeId, boolean canUserAct) {
        if (currentEtapeId == null || !canUserAct || currentEtapeId != 7) {
            return createReadOnlyPermissions();
        }

        // Phase 7: Service Technique implementation
        return DossierPermissionsDTO.builder()
                .peutEtreModifie(false)
                .peutEtreEnvoye(true) // Can send back to GUC when done
                .peutEtreSupprime(false)
                .peutAjouterNotes(true)
                .peutRetournerAntenne(false)
                .peutEnvoyerCommission(false)
                .peutRejeter(false)
                .peutApprouver(true) // Approve implementation completion
                .peutVoirDocuments(true)
                .peutTelechargerDocuments(true)
                .build();
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
     * Helper method for admin permissions
     */
    private DossierPermissionsDTO createAdminPermissions() {
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
    }

    // ============================================================================
    // DISPLAY STATUS - ID-BASED
    // ============================================================================

    /**
     * Get display status using etape ID instead of complex string matching
     */
    public String getDisplayStatus(Dossier.DossierStatus status, WorkflowService.EtapeInfo etapeInfo) {
        if (etapeInfo != null && etapeInfo.getEtapeId() != null) {
            String phaseInfo = etapeInfo.getDesignation();
            
            String statusSuffix = "";
            if (etapeInfo.getEnRetard()) {
                statusSuffix += " - EN RETARD";
            }
            if (etapeInfo.getJoursRestants() > 0) {
                statusSuffix += " (" + etapeInfo.getJoursRestants() + "j restants)";
            }
            
            return phaseInfo + statusSuffix;
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

    // ============================================================================
    // STATISTICS CALCULATION - SIMPLIFIED
    // ============================================================================

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

        // Count dossiers in commission phase (Phase 3)
        long dossiersEnCommission = dossiers.stream()
                .filter(d -> {
                    try {
                        WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(d);
                        return etapeInfo.getEtapeId() == 3L;
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

    // ============================================================================
    // UTILITY METHODS - UNCHANGED
    // ============================================================================

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

    // ============================================================================
    // MAPPING METHODS - UNCHANGED BUT SIMPLIFIED
    // ============================================================================

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
}