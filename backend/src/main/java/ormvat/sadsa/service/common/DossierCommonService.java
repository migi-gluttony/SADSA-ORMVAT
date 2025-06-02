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
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final HistoriqueWorkflowRepository historiqueWorkflowRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final NoteRepository noteRepository;
    private final DocumentRequisRepository documentRequisRepository;
    private final AuditTrailRepository auditTrailRepository;

    /**
     * Get dossiers for user role
     */
    public List<Dossier> getDossiersForUserRole(Utilisateur utilisateur, DossierFilterRequest filterRequest) {
        switch (utilisateur.getRole()) {
            case AGENT_ANTENNE:
                if (utilisateur.getAntenne() != null) {
                    return dossierRepository.findByAntenneId(utilisateur.getAntenne().getId());
                }
                return List.of();

            case AGENT_GUC:
                return dossierRepository.findAll().stream()
                        .filter(d -> !d.getStatus().equals(Dossier.DossierStatus.DRAFT))
                        .collect(Collectors.toList());

            case AGENT_COMMISSION_TERRAIN:
                if (utilisateur.getEquipeCommission() != null) {
                    return dossierRepository.findAll().stream()
                            .filter(d -> d.getStatus().equals(Dossier.DossierStatus.SUBMITTED) ||
                                    d.getStatus().equals(Dossier.DossierStatus.IN_REVIEW) ||
                                    d.getStatus().equals(Dossier.DossierStatus.APPROVED) ||
                                    d.getStatus().equals(Dossier.DossierStatus.REJECTED))
                            .filter(d -> {
                                Long rubriqueId = d.getSousRubrique().getRubrique().getId();
                                Utilisateur.EquipeCommission equipeRequise = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);
                                return equipeRequise.equals(utilisateur.getEquipeCommission());
                            })
                            .collect(Collectors.toList());
                } else {
                    return dossierRepository.findAll().stream()
                            .filter(d -> d.getStatus().equals(Dossier.DossierStatus.SUBMITTED) ||
                                    d.getStatus().equals(Dossier.DossierStatus.IN_REVIEW) ||
                                    d.getStatus().equals(Dossier.DossierStatus.APPROVED) ||
                                    d.getStatus().equals(Dossier.DossierStatus.REJECTED))
                            .collect(Collectors.toList());
                }

            case ADMIN:
                return dossierRepository.findAll();

            default:
                return List.of();
        }
    }

    /**
     * Check if user can access dossier
     */
    public boolean canAccessDossier(Dossier dossier, Utilisateur utilisateur) {
        switch (utilisateur.getRole()) {
            case ADMIN:
                return true;
            case AGENT_ANTENNE:
                return utilisateur.getAntenne() != null &&
                        dossier.getAntenne().getId().equals(utilisateur.getAntenne().getId());
            case AGENT_GUC:
                return !dossier.getStatus().equals(Dossier.DossierStatus.DRAFT);
            case AGENT_COMMISSION_TERRAIN:
                boolean statusAllowed = dossier.getStatus().equals(Dossier.DossierStatus.SUBMITTED) ||
                        dossier.getStatus().equals(Dossier.DossierStatus.IN_REVIEW) ||
                        dossier.getStatus().equals(Dossier.DossierStatus.APPROVED) ||
                        dossier.getStatus().equals(Dossier.DossierStatus.REJECTED);
                
                if (utilisateur.getEquipeCommission() != null) {
                    Long rubriqueId = dossier.getSousRubrique().getRubrique().getId();
                    Utilisateur.EquipeCommission equipeRequise = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);
                    return statusAllowed && equipeRequise.equals(utilisateur.getEquipeCommission());
                } else {
                    return statusAllowed;
                }
            default:
                return false;
        }
    }

    /**
     * Apply filters to dossiers list
     */
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

    /**
     * Check if dossier contains search term
     */
    private boolean containsSearchTerm(Dossier dossier, String searchTerm) {
        String term = searchTerm.toLowerCase();
        return (dossier.getSaba() != null && dossier.getSaba().toLowerCase().contains(term)) ||
                (dossier.getReference() != null && dossier.getReference().toLowerCase().contains(term)) ||
                (dossier.getAgriculteur().getCin() != null
                        && dossier.getAgriculteur().getCin().toLowerCase().contains(term)) ||
                (dossier.getAgriculteur().getNom() != null
                        && dossier.getAgriculteur().getNom().toLowerCase().contains(term)) ||
                (dossier.getAgriculteur().getPrenom() != null
                        && dossier.getAgriculteur().getPrenom().toLowerCase().contains(term));
    }

    /**
     * Map to DossierSummaryDTO
     */
    public DossierSummaryDTO mapToDossierSummaryDTO(Dossier dossier, Utilisateur currentUser) {
        List<WorkflowInstance> workflows = workflowInstanceRepository.findByDossierId(dossier.getId());
        WorkflowInstance currentWorkflow = workflows.isEmpty() ? null : workflows.get(0);

        List<PieceJointe> pieceJointes = pieceJointeRepository.findByDossierId(dossier.getId());
        List<DocumentRequis> documentsRequis = documentRequisRepository
                .findBySousRubriqueId(dossier.getSousRubrique().getId());

        double completionPercentage = calculateCompletionPercentage(pieceJointes, documentsRequis);
        Map<String, Object> workflowInfo = calculateWorkflowInfo(currentWorkflow);
        DossierPermissionsDTO permissions = calculatePermissions(dossier, currentUser);

        int notesCount = noteRepository.findByDossierId(dossier.getId()).size();

        return DossierSummaryDTO.builder()
                .id(dossier.getId())
                .numeroDossier(dossier.getNumeroDossier())
                .saba(dossier.getSaba())
                .reference(dossier.getReference())
                .statut(getDisplayStatus(dossier.getStatus(), currentWorkflow))
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
                .etapeActuelle((String) workflowInfo.get("etapeActuelle"))
                .emplacementActuel((WorkflowInstance.EmplacementType) workflowInfo.get("emplacementActuel"))
                .joursRestants((Integer) workflowInfo.get("joursRestants"))
                .enRetard((Boolean) workflowInfo.get("enRetard"))
                .dateLimite(currentWorkflow != null ? currentWorkflow.getDateLimite() : null)
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

    /**
     * Calculate dossier permissions based on user role
     */
    public DossierPermissionsDTO calculatePermissions(Dossier dossier, Utilisateur utilisateur) {
        boolean isDraft = dossier.getStatus() == Dossier.DossierStatus.DRAFT;
        boolean isReturnedForCompletion = dossier.getStatus() == Dossier.DossierStatus.RETURNED_FOR_COMPLETION;
        boolean isSubmitted = dossier.getStatus() == Dossier.DossierStatus.SUBMITTED;
        boolean isInReview = dossier.getStatus() == Dossier.DossierStatus.IN_REVIEW;

        boolean canEditAtAntenne = isDraft || isReturnedForCompletion;

        switch (utilisateur.getRole()) {
            case AGENT_ANTENNE:
                return DossierPermissionsDTO.builder()
                        .peutEtreModifie(canEditAtAntenne)
                        .peutEtreEnvoye(canEditAtAntenne)
                        .peutEtreSupprime(isDraft)
                        .peutAjouterNotes(true)
                        .peutRetournerAntenne(false)
                        .peutEnvoyerCommission(false)
                        .peutRejeter(false)
                        .peutApprouver(false)
                        .peutVoirDocuments(true)
                        .peutTelechargerDocuments(true)
                        .build();

            case AGENT_GUC:
                return DossierPermissionsDTO.builder()
                        .peutEtreModifie(false)
                        .peutEtreEnvoye(false)
                        .peutEtreSupprime(false)
                        .peutAjouterNotes(true)
                        .peutRetournerAntenne(isSubmitted || isInReview)
                        .peutEnvoyerCommission(isSubmitted)
                        .peutRejeter(isSubmitted || isInReview)
                        .peutApprouver(false)
                        .peutVoirDocuments(true)
                        .peutTelechargerDocuments(true)
                        .build();

            case ADMIN:
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
                return DossierPermissionsDTO.builder()
                        .peutEtreModifie(false)
                        .peutEtreEnvoye(false)
                        .peutEtreSupprime(false)
                        .peutAjouterNotes(false)
                        .peutRetournerAntenne(false)
                        .peutEnvoyerCommission(false)
                        .peutRejeter(false)
                        .peutApprouver(false)
                        .peutVoirDocuments(false)
                        .peutTelechargerDocuments(false)
                        .build();
        }
    }

    /**
     * Calculate completion percentage
     */
    public double calculateCompletionPercentage(List<PieceJointe> pieceJointes, List<DocumentRequis> documentsRequis) {
        if (documentsRequis.isEmpty()) return 100.0;

        long completedDocs = pieceJointes.stream()
                .filter(pj -> pj.getStatus() == PieceJointe.DocumentStatus.COMPLETE)
                .count();

        return (double) completedDocs / documentsRequis.size() * 100.0;
    }

    /**
     * Calculate workflow information
     */
    public Map<String, Object> calculateWorkflowInfo(WorkflowInstance workflow) {
        Map<String, Object> info = new HashMap<>();

        if (workflow != null) {
            info.put("etapeActuelle", workflow.getEtapeDesignation());
            info.put("emplacementActuel", workflow.getEmplacementActuel());
            info.put("joursRestants", workflow.getJoursRestants());
            info.put("enRetard", workflow.getEnRetard());
        } else {
            info.put("etapeActuelle", "Phase Antenne");
            info.put("emplacementActuel", WorkflowInstance.EmplacementType.ANTENNE);
            info.put("joursRestants", 3);
            info.put("enRetard", false);
        }

        return info;
    }

    /**
     * Get display status
     */
    public String getDisplayStatus(Dossier.DossierStatus status, WorkflowInstance workflow) {
        if (workflow != null && workflow.getEtapeDesignation() != null) {
            return workflow.getEtapeDesignation();
        }

        switch (status) {
            case DRAFT: return "Brouillon";
            case SUBMITTED: return "Soumis au GUC";
            case IN_REVIEW: return "En cours d'examen";
            case APPROVED: return "Approuvé";
            case REJECTED: return "Rejeté";
            case COMPLETED: return "Terminé";
            case RETURNED_FOR_COMPLETION: return "Retourné pour complétion";
            default: return status.name();
        }
    }

    /**
     * Calculate statistics
     */
    public DossierStatisticsDTO calculateStatistics(List<Dossier> dossiers, Utilisateur utilisateur) {
        long total = dossiers.size();
        long enCours = dossiers.stream().filter(d -> d.getStatus() == Dossier.DossierStatus.IN_REVIEW).count();
        long approuves = dossiers.stream().filter(d -> d.getStatus() == Dossier.DossierStatus.APPROVED).count();
        long rejetes = dossiers.stream().filter(d -> d.getStatus() == Dossier.DossierStatus.REJECTED).count();
        long attenteTraitement = dossiers.stream().filter(d -> d.getStatus() == Dossier.DossierStatus.SUBMITTED).count();

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long ceMois = dossiers.stream().filter(d -> d.getDateCreation().isAfter(startOfMonth)).count();

        double tauxCompletion = total > 0 ? (double) approuves / total * 100.0 : 0.0;

        return DossierStatisticsDTO.builder()
                .totalDossiers(total)
                .dossiersEnCours(enCours)
                .dossiersApprouves(approuves)
                .dossiersRejetes(rejetes)
                .dossiersEnRetard(0L)
                .tauxCompletion(tauxCompletion)
                .dossiersCeMois(ceMois)
                .dossiersAttenteTraitement(attenteTraitement)
                .dossiersEnCommission(0L)
                .build();
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

    /**
     * Map to detail DTOs
     */
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

    public List<AvailableFormDTO> getAvailableForms(Long sousRubriqueId, List<PieceJointe> pieceJointes, Utilisateur currentUser) {
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