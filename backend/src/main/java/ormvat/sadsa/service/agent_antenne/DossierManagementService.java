package ormvat.sadsa.service.agent_antenne;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ormvat.sadsa.dto.agent_antenne.DossierManagementDTOs.*;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DossierManagementService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final HistoriqueWorkflowRepository historiqueWorkflowRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final NoteRepository noteRepository;
    private final DocumentRequisRepository documentRequisRepository;
    private final AuditTrailRepository auditTrailRepository;

    /**
     * Get paginated list of dossiers with filtering
     */
    public DossierListResponse getDossiersList(DossierFilterRequest filterRequest, String userEmail) {
        try {
            // Get user and their antenne
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Set up pagination and sorting
            String sortBy = filterRequest.getSortBy() != null ? filterRequest.getSortBy() : "dateCreation";
            String sortDirection = filterRequest.getSortDirection() != null ? filterRequest.getSortDirection() : "DESC";
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            
            int page = filterRequest.getPage() != null ? filterRequest.getPage() : 0;
            int size = filterRequest.getSize() != null ? filterRequest.getSize() : 20;
            Pageable pageable = PageRequest.of(page, size, sort);

            // For now, get all dossiers from user's antenne (can be extended with more complex filtering)
            List<Dossier> allDossiers;
            if (utilisateur.getAntenne() != null) {
                allDossiers = dossierRepository.findByAntenneId(utilisateur.getAntenne().getId());
            } else {
                allDossiers = dossierRepository.findAll();
            }

            // Apply filters
            List<Dossier> filteredDossiers = applyFilters(allDossiers, filterRequest);

            // Calculate pagination manually for filtered results
            int totalElements = filteredDossiers.size();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalElements);
            
            List<Dossier> paginatedDossiers = filteredDossiers.subList(startIndex, endIndex);

            // Convert to DTOs
            List<DossierSummaryDTO> dossierSummaries = paginatedDossiers.stream()
                    .map(this::mapToDossierSummaryDTO)
                    .collect(Collectors.toList());

            // Calculate statistics
            DossierStatisticsDTO statistics = calculateStatistics(allDossiers);

            return DossierListResponse.builder()
                    .dossiers(dossierSummaries)
                    .totalCount((long) totalElements)
                    .currentPage(page)
                    .pageSize(size)
                    .totalPages((int) Math.ceil((double) totalElements / size))
                    .currentUserAntenne(utilisateur.getAntenne() != null ? utilisateur.getAntenne().getDesignation() : "N/A")
                    .currentUserCDA(utilisateur.getAntenne() != null && utilisateur.getAntenne().getCda() != null ? 
                            utilisateur.getAntenne().getCda().getDescription() : "N/A")
                    .statistics(statistics)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des dossiers", e);
            throw new RuntimeException("Erreur lors de la récupération des dossiers: " + e.getMessage());
        }
    }

    /**
     * Get detailed dossier information
     */
    public DossierDetailResponse getDossierDetail(Long dossierId, String userEmail) {
        try {
            // Get user
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Get dossier
            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Check access permission (user can only access dossiers from their antenne)
            if (utilisateur.getAntenne() != null && 
                !dossier.getAntenne().getId().equals(utilisateur.getAntenne().getId()) &&
                !utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
                throw new RuntimeException("Accès non autorisé à ce dossier");
            }

            // Get workflow instance
            List<WorkflowInstance> workflowInstances = workflowInstanceRepository.findByDossierId(dossierId);
            WorkflowInstance currentWorkflow = workflowInstances.isEmpty() ? null : workflowInstances.get(0);

            // Get workflow history
            List<HistoriqueWorkflow> workflowHistory = historiqueWorkflowRepository.findByDossierId(dossierId);

            // Get piece jointes
            List<PieceJointe> pieceJointes = pieceJointeRepository.findByDossierId(dossierId);

            // Get notes
            List<Note> notes = noteRepository.findByDossierId(dossierId);

            // Get available forms for this sous-rubrique
            List<AvailableFormDTO> availableForms = getAvailableForms(dossier.getSousRubrique().getId(), pieceJointes);

            // Calculate permissions
            Map<String, Boolean> permissions = calculatePermissions(dossier, utilisateur);

            // Calculate workflow info
            Map<String, Object> workflowInfo = calculateWorkflowInfo(currentWorkflow);

            return DossierDetailResponse.builder()
                    .dossier(mapToDossierDetailDTO(dossier))
                    .agriculteur(mapToAgriculteurDetailDTO(dossier.getAgriculteur()))
                    .historiqueWorkflow(mapToWorkflowHistoryDTOs(workflowHistory))
                    .availableForms(availableForms)
                    .pieceJointes(mapToPieceJointeDTOs(pieceJointes))
                    .notes(mapToNoteDTOs(notes))
                    .validationErrors(getValidationErrors(dossier, pieceJointes))
                    .joursRestants((Integer) workflowInfo.get("joursRestants"))
                    .enRetard((Boolean) workflowInfo.get("enRetard"))
                    .etapeActuelle((String) workflowInfo.get("etapeActuelle"))
                    .emplacementActuel((WorkflowInstance.EmplacementType) workflowInfo.get("emplacementActuel"))
                    .peutEtreModifie(permissions.get("peutEtreModifie"))
                    .peutEtreEnvoye(permissions.get("peutEtreEnvoye"))
                    .peutEtreSupprime(permissions.get("peutEtreSupprime"))
                    .peutAjouterNotes(permissions.get("peutAjouterNotes"))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la récupération du détail du dossier {}", dossierId, e);
            throw new RuntimeException("Erreur lors de la récupération du détail: " + e.getMessage());
        }
    }

    /**
     * Send dossier to GUC
     */
    @Transactional
    public DossierActionResponse sendDossierToGUC(SendToGUCRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Verify permission
            if (!canSendToGUC(dossier, utilisateur)) {
                throw new RuntimeException("Vous n'avez pas l'autorisation d'envoyer ce dossier");
            }

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.SUBMITTED);
            dossier.setDateSubmission(LocalDateTime.now());
            dossierRepository.save(dossier);

            // Update workflow
            updateWorkflowForGUC(dossier, utilisateur, request.getCommentaire());

            // Create audit trail
            createAuditTrail("ENVOI_GUC", dossier, utilisateur, request.getCommentaire());

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier envoyé au GUC avec succès")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du dossier au GUC", e);
            throw new RuntimeException("Erreur lors de l'envoi: " + e.getMessage());
        }
    }

    /**
     * Delete dossier
     */
    @Transactional
    public DossierActionResponse deleteDossier(DeleteDossierRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Verify permission
            if (!canDeleteDossier(dossier, utilisateur)) {
                throw new RuntimeException("Vous n'avez pas l'autorisation de supprimer ce dossier");
            }

            // Create audit trail before deletion
            createAuditTrail("SUPPRESSION_DOSSIER", dossier, utilisateur, request.getMotif());

            // Delete dossier (cascade will handle related entities)
            dossierRepository.delete(dossier);

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier supprimé avec succès")
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la suppression du dossier", e);
            throw new RuntimeException("Erreur lors de la suppression: " + e.getMessage());
        }
    }

    // Private helper methods

    private List<Dossier> applyFilters(List<Dossier> dossiers, DossierFilterRequest filter) {
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
                .collect(Collectors.toList());
    }

    private boolean containsSearchTerm(Dossier dossier, String searchTerm) {
        String term = searchTerm.toLowerCase();
        return (dossier.getSaba() != null && dossier.getSaba().toLowerCase().contains(term)) ||
                (dossier.getReference() != null && dossier.getReference().toLowerCase().contains(term)) ||
                (dossier.getAgriculteur().getCin() != null && dossier.getAgriculteur().getCin().toLowerCase().contains(term)) ||
                (dossier.getAgriculteur().getNom() != null && dossier.getAgriculteur().getNom().toLowerCase().contains(term)) ||
                (dossier.getAgriculteur().getPrenom() != null && dossier.getAgriculteur().getPrenom().toLowerCase().contains(term));
    }

    private DossierSummaryDTO mapToDossierSummaryDTO(Dossier dossier) {
        // Get workflow instance
        List<WorkflowInstance> workflows = workflowInstanceRepository.findByDossierId(dossier.getId());
        WorkflowInstance currentWorkflow = workflows.isEmpty() ? null : workflows.get(0);

        // Get piece jointes count for completion calculation
        List<PieceJointe> pieceJointes = pieceJointeRepository.findByDossierId(dossier.getId());
        List<DocumentRequis> documentsRequis = documentRequisRepository.findBySousRubriqueId(dossier.getSousRubrique().getId());

        // Calculate completion
        double completionPercentage = calculateCompletionPercentage(pieceJointes, documentsRequis);

        // Calculate workflow info
        Map<String, Object> workflowInfo = calculateWorkflowInfo(currentWorkflow);

        // Calculate permissions
        Map<String, Boolean> permissions = calculateBasicPermissions(dossier);

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
                .formsCompleted((int) pieceJointes.stream().filter(pj -> pj.getStatus() == PieceJointe.DocumentStatus.COMPLETE).count())
                .totalForms(documentsRequis.size())
                .completionPercentage(completionPercentage)
                .peutEtreModifie(permissions.get("peutEtreModifie"))
                .peutEtreEnvoye(permissions.get("peutEtreEnvoye"))
                .peutEtreSupprime(permissions.get("peutEtreSupprime"))
                .build();
    }

    private String getDisplayStatus(Dossier.DossierStatus status, WorkflowInstance workflow) {
        if (workflow != null && workflow.getEtapeDesignation() != null) {
            return workflow.getEtapeDesignation();
        }
        
        switch (status) {
            case DRAFT: return "Brouillon";
            case SUBMITTED: return "Soumis";
            case IN_REVIEW: return "En cours d'examen";
            case APPROVED: return "Approuvé";
            case REJECTED: return "Rejeté";
            case COMPLETED: return "Terminé";
            default: return status.name();
        }
    }

    private double calculateCompletionPercentage(List<PieceJointe> pieceJointes, List<DocumentRequis> documentsRequis) {
        if (documentsRequis.isEmpty()) return 100.0;
        
        long completedDocs = pieceJointes.stream()
                .filter(pj -> pj.getStatus() == PieceJointe.DocumentStatus.COMPLETE)
                .count();
        
        return (double) completedDocs / documentsRequis.size() * 100.0;
    }

    private Map<String, Object> calculateWorkflowInfo(WorkflowInstance workflow) {
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

    private Map<String, Boolean> calculateBasicPermissions(Dossier dossier) {
        Map<String, Boolean> permissions = new HashMap<>();
        
        boolean isDraft = dossier.getStatus() == Dossier.DossierStatus.DRAFT;
        boolean isInAntenne = true; // For now, assume all visible dossiers can be modified by antenne
        
        permissions.put("peutEtreModifie", isDraft && isInAntenne);
        permissions.put("peutEtreEnvoye", isDraft && isInAntenne);
        permissions.put("peutEtreSupprime", isDraft && isInAntenne);
        
        return permissions;
    }

    private Map<String, Boolean> calculatePermissions(Dossier dossier, Utilisateur utilisateur) {
        Map<String, Boolean> permissions = calculateBasicPermissions(dossier);
        permissions.put("peutAjouterNotes", true); // Agent can always add notes
        return permissions;
    }

    private DossierDetailDTO mapToDossierDetailDTO(Dossier dossier) {
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
                .utilisateurCreateurNom(dossier.getUtilisateurCreateur().getPrenom() + " " + dossier.getUtilisateurCreateur().getNom())
                .build();
    }

    private AgriculteurDetailDTO mapToAgriculteurDetailDTO(Agriculteur agriculteur) {
        return AgriculteurDetailDTO.builder()
                .id(agriculteur.getId())
                .nom(agriculteur.getNom())
                .prenom(agriculteur.getPrenom())
                .cin(agriculteur.getCin())
                .telephone(agriculteur.getTelephone())
                .communeRurale(agriculteur.getCommuneRurale() != null ? agriculteur.getCommuneRurale().getDesignation() : null)
                .douar(agriculteur.getDouar() != null ? agriculteur.getDouar().getDesignation() : null)
                .province(agriculteur.getCommuneRurale() != null && agriculteur.getCommuneRurale().getCercle() != null 
                        && agriculteur.getCommuneRurale().getCercle().getProvince() != null ? 
                        agriculteur.getCommuneRurale().getCercle().getProvince().getDesignation() : null)
                .cercle(agriculteur.getCommuneRurale() != null && agriculteur.getCommuneRurale().getCercle() != null ? 
                        agriculteur.getCommuneRurale().getCercle().getDesignation() : null)
                .build();
    }

    private List<WorkflowHistoryDTO> mapToWorkflowHistoryDTOs(List<HistoriqueWorkflow> history) {
        return history.stream()
                .map(h -> WorkflowHistoryDTO.builder()
                        .id(h.getId())
                        .etapeDesignation(h.getEtapeDesignation())
                        .emplacementType(h.getEmplacementType())
                        .dateEntree(h.getDateEntree())
                        .dateSortie(h.getDateSortie())
                        .dureeJours(h.getDureeJours())
                        .enRetard(h.getEnRetard())
                        .utilisateurNom(h.getUtilisateur() != null ? h.getUtilisateur().getPrenom() + " " + h.getUtilisateur().getNom() : null)
                        .commentaire(h.getCommentaire())
                        .build())
                .collect(Collectors.toList());
    }

    private List<PieceJointeDetailDTO> mapToPieceJointeDTOs(List<PieceJointe> pieceJointes) {
        return pieceJointes.stream()
                .map(pj -> PieceJointeDetailDTO.builder()
                        .id(pj.getId())
                        .nomFichier(pj.getNomFichier())
                        .cheminFichier(pj.getCheminFichier())
                        .typeDocument(pj.getTypeDocument())
                        .status(pj.getStatus().name())
                        .dateUpload(pj.getDateUpload())
                        .utilisateurNom(pj.getUtilisateur() != null ? pj.getUtilisateur().getPrenom() + " " + pj.getUtilisateur().getNom() : null)
                        .customTitle(pj.getNomFichier()) // For now, use filename as custom title
                        .isOriginalDocument(true) // Default to true
                        .build())
                .collect(Collectors.toList());
    }

    private List<NoteDTO> mapToNoteDTOs(List<Note> notes) {
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
                        .utilisateurExpediteurNom(n.getUtilisateurExpediteur() != null ? 
                                n.getUtilisateurExpediteur().getPrenom() + " " + n.getUtilisateurExpediteur().getNom() : null)
                        .utilisateurDestinataireNom(n.getUtilisateurDestinataire() != null ? 
                                n.getUtilisateurDestinataire().getPrenom() + " " + n.getUtilisateurDestinataire().getNom() : null)
                        .build())
                .collect(Collectors.toList());
    }

    private List<AvailableFormDTO> getAvailableForms(Long sousRubriqueId, List<PieceJointe> pieceJointes) {
        List<DocumentRequis> documentsRequis = documentRequisRepository.findBySousRubriqueId(sousRubriqueId);
        
        return documentsRequis.stream()
                .map(doc -> {
                    Optional<PieceJointe> relatedPieceJointe = pieceJointes.stream()
                            .filter(pj -> pj.getDocumentRequis() != null && pj.getDocumentRequis().getId().equals(doc.getId()))
                            .findFirst();
                    
                    return AvailableFormDTO.builder()
                            .formId(doc.getId().toString())
                            .title(doc.getNomDocument())
                            .description(doc.getDescription())
                            .isCompleted(relatedPieceJointe.isPresent() && 
                                    relatedPieceJointe.get().getStatus() == PieceJointe.DocumentStatus.COMPLETE)
                            .lastModified(relatedPieceJointe.map(PieceJointe::getDateUpload).orElse(null))
                            .requiredDocuments(List.of(doc.getNomDocument()))
                            .formConfig(new HashMap<>()) // To be implemented with actual form configuration
                            .formData(new HashMap<>()) // To be implemented with actual form data
                            .documentType(doc.getNomDocument())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<String> getValidationErrors(Dossier dossier, List<PieceJointe> pieceJointes) {
        // Implement validation logic here
        return List.of(); // Return empty list for now
    }

    private DossierStatisticsDTO calculateStatistics(List<Dossier> dossiers) {
        long total = dossiers.size();
        long enCours = dossiers.stream().filter(d -> d.getStatus() == Dossier.DossierStatus.IN_REVIEW).count();
        long approuves = dossiers.stream().filter(d -> d.getStatus() == Dossier.DossierStatus.APPROVED).count();
        long rejetes = dossiers.stream().filter(d -> d.getStatus() == Dossier.DossierStatus.REJECTED).count();
        
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long ceMois = dossiers.stream().filter(d -> d.getDateCreation().isAfter(startOfMonth)).count();

        double tauxCompletion = total > 0 ? (double) approuves / total * 100.0 : 0.0;

        return DossierStatisticsDTO.builder()
                .totalDossiers(total)
                .dossiersEnCours(enCours)
                .dossiersApprouves(approuves)
                .dossiersRejetes(rejetes)
                .dossiersEnRetard(0L) // To be calculated based on workflow instances
                .tauxCompletion(tauxCompletion)
                .dossiersCeMois(ceMois)
                .build();
    }

    private boolean canSendToGUC(Dossier dossier, Utilisateur utilisateur) {
        return dossier.getStatus() == Dossier.DossierStatus.DRAFT &&
                (utilisateur.getRole() == Utilisateur.UserRole.AGENT_ANTENNE || utilisateur.getRole() == Utilisateur.UserRole.ADMIN);
    }

    private boolean canDeleteDossier(Dossier dossier, Utilisateur utilisateur) {
        return dossier.getStatus() == Dossier.DossierStatus.DRAFT &&
                (utilisateur.getRole() == Utilisateur.UserRole.AGENT_ANTENNE || utilisateur.getRole() == Utilisateur.UserRole.ADMIN);
    }

    private void updateWorkflowForGUC(Dossier dossier, Utilisateur utilisateur, String commentaire) {
        // Find current workflow instance
        List<WorkflowInstance> workflows = workflowInstanceRepository.findByDossierId(dossier.getId());
        
        if (!workflows.isEmpty()) {
            WorkflowInstance current = workflows.get(0);
            current.setEtapeDesignation("Phase GUC");
            current.setEmplacementActuel(WorkflowInstance.EmplacementType.GUC);
            current.setDateEntree(LocalDateTime.now());
            workflowInstanceRepository.save(current);
        }

        // Create history entry
        HistoriqueWorkflow history = new HistoriqueWorkflow();
        history.setDossier(dossier);
        history.setEtapeDesignation("Envoi au GUC");
        history.setEmplacementType(WorkflowInstance.EmplacementType.ANTENNE);
        history.setDateEntree(LocalDateTime.now());
        history.setUtilisateur(utilisateur);
        history.setCommentaire(commentaire);
        historiqueWorkflowRepository.save(history);
    }

    private void createAuditTrail(String action, Dossier dossier, Utilisateur utilisateur, String description) {
        AuditTrail audit = new AuditTrail();
        audit.setAction(action);
        audit.setEntite("Dossier");
        audit.setEntiteId(dossier.getId());
        audit.setDateAction(LocalDateTime.now());
        audit.setUtilisateur(utilisateur);
        audit.setDescription(description);
        auditTrailRepository.save(audit);
    }
}