package ormvat.sadsa.service.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ormvat.sadsa.dto.common.DossierManagementDTOs.*;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Optional;
import java.util.ArrayList;

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
     * Get paginated list of dossiers with filtering based on user role
     */
    public DossierListResponse getDossiersList(DossierFilterRequest filterRequest, String userEmail) {
        try {
            // Get user and their role
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Set up pagination and sorting
            String sortBy = filterRequest.getSortBy() != null ? filterRequest.getSortBy() : "dateCreation";
            String sortDirection = filterRequest.getSortDirection() != null ? filterRequest.getSortDirection() : "DESC";
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            
            int page = filterRequest.getPage() != null ? filterRequest.getPage() : 0;
            int size = filterRequest.getSize() != null ? filterRequest.getSize() : 20;
            Pageable pageable = PageRequest.of(page, size, sort);

            // Get dossiers based on user role
            List<Dossier> allDossiers = getDossiersForUserRole(utilisateur, filterRequest);

            // Apply filters
            List<Dossier> filteredDossiers = applyFilters(allDossiers, filterRequest);

            // Calculate pagination manually for filtered results
            int totalElements = filteredDossiers.size();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalElements);
            
            List<Dossier> paginatedDossiers = filteredDossiers.subList(startIndex, endIndex);

            // Convert to DTOs with role-specific permissions
            List<DossierSummaryDTO> dossierSummaries = paginatedDossiers.stream()
                    .map(d -> mapToDossierSummaryDTO(d, utilisateur))
                    .collect(Collectors.toList());

            // Calculate statistics
            DossierStatisticsDTO statistics = calculateStatistics(allDossiers, utilisateur);

            // Get available actions for this user role
            List<AvailableActionDTO> availableActions = getAvailableActionsForRole(utilisateur.getRole());

            return DossierListResponse.builder()
                    .dossiers(dossierSummaries)
                    .totalCount((long) totalElements)
                    .currentPage(page)
                    .pageSize(size)
                    .totalPages((int) Math.ceil((double) totalElements / size))
                    .currentUserRole(utilisateur.getRole().name())
                    .currentUserAntenne(utilisateur.getAntenne() != null ? utilisateur.getAntenne().getDesignation() : "N/A")
                    .currentUserCDA(utilisateur.getAntenne() != null && utilisateur.getAntenne().getCda() != null ? 
                            utilisateur.getAntenne().getCda().getDescription() : "N/A")
                    .statistics(statistics)
                    .availableActions(availableActions)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des dossiers", e);
            throw new RuntimeException("Erreur lors de la récupération des dossiers: " + e.getMessage());
        }
    }

    /**
     * Get detailed dossier information with role-specific data
     */
    public DossierDetailResponse getDossierDetail(Long dossierId, String userEmail) {
        try {
            // Get user
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Get dossier
            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Check access permission based on role
            if (!canAccessDossier(dossier, utilisateur)) {
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
            List<AvailableFormDTO> availableForms = getAvailableForms(dossier.getSousRubrique().getId(), pieceJointes, utilisateur);

            // Calculate permissions based on role
            DossierPermissionsDTO permissions = calculatePermissions(dossier, utilisateur);

            // Get available actions for this dossier and user
            List<AvailableActionDTO> availableActions = getAvailableActionsForDossier(dossier, utilisateur);

            // Calculate workflow info
            Map<String, Object> workflowInfo = calculateWorkflowInfo(currentWorkflow);

            return DossierDetailResponse.builder()
                    .dossier(mapToDossierDetailDTO(dossier))
                    .agriculteur(mapToAgriculteurDetailDTO(dossier.getAgriculteur()))
                    .historiqueWorkflow(mapToWorkflowHistoryDTOs(workflowHistory))
                    .availableForms(availableForms)
                    .pieceJointes(mapToPieceJointeDTOs(pieceJointes, utilisateur))
                    .notes(mapToNoteDTOs(notes, utilisateur))
                    .validationErrors(getValidationErrors(dossier, pieceJointes))
                    .joursRestants((Integer) workflowInfo.get("joursRestants"))
                    .enRetard((Boolean) workflowInfo.get("enRetard"))
                    .etapeActuelle((String) workflowInfo.get("etapeActuelle"))
                    .emplacementActuel((WorkflowInstance.EmplacementType) workflowInfo.get("emplacementActuel"))
                    .permissions(permissions)
                    .availableActions(availableActions)
                    .currentUserRole(utilisateur.getRole().name())
                    .currentUserAntenne(utilisateur.getAntenne() != null ? utilisateur.getAntenne().getDesignation() : null)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la récupération du détail du dossier {}", dossierId, e);
            throw new RuntimeException("Erreur lors de la récupération du détail: " + e.getMessage());
        }
    }

    /**
     * Send dossier to GUC (Agent Antenne action)
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
     * Send dossier to Commission (Agent GUC action)
     */
    @Transactional
    public DossierActionResponse sendDossierToCommission(SendToCommissionRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_GUC)) {
                throw new RuntimeException("Seul un agent GUC peut envoyer un dossier à la commission");
            }

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.IN_REVIEW);
            dossierRepository.save(dossier);

            // Update workflow to Commission
            updateWorkflowForCommission(dossier, utilisateur, request);

            // Create audit trail
            createAuditTrail("ENVOI_COMMISSION", dossier, utilisateur, request.getCommentaire());

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier envoyé à la commission avec succès")
                    .newStatut(dossier.getStatus().name())
                    .dateAction(LocalDateTime.now())
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

            // Update dossier status to allow editing at antenne
            dossier.setStatus(Dossier.DossierStatus.RETURNED_FOR_COMPLETION);
            dossierRepository.save(dossier);

            // Update workflow back to Antenne
            updateWorkflowBackToAntenne(dossier, utilisateur, request);

            // Create audit trail
            createAuditTrail("RETOUR_ANTENNE", dossier, utilisateur, request.getCommentaire());

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

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.REJECTED);
            dossierRepository.save(dossier);

            // Update workflow
            updateWorkflowForRejection(dossier, utilisateur, request);

            // Create audit trail
            createAuditTrail("REJET_DOSSIER", dossier, utilisateur, request.getCommentaire());

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
     * Delete dossier (role-based)
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

    /**
     * Add note to dossier
     */
    @Transactional
    public DossierActionResponse addNote(AddNoteRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Create note
            Note note = new Note();
            note.setDossier(dossier);
            note.setObjet(request.getObjet());
            note.setContenu(request.getContenu());
            note.setTypeNote(request.getTypeNote());
            note.setPriorite(request.getPriorite());
            note.setUtilisateurExpediteur(utilisateur);
            note.setDateCreation(LocalDateTime.now());

            // Set destinataire if specified
            if (request.getUtilisateurDestinataireId() != null) {
                Utilisateur destinataire = utilisateurRepository.findById(request.getUtilisateurDestinataireId())
                        .orElse(null);
                note.setUtilisateurDestinataire(destinataire);
            }

            noteRepository.save(note);

            // Create audit trail
            createAuditTrail("AJOUT_NOTE", dossier, utilisateur, request.getObjet());

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Note ajoutée avec succès")
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'ajout de la note", e);
            throw new RuntimeException("Erreur lors de l'ajout de la note: " + e.getMessage());
        }
    }

    // Private helper methods

    private List<Dossier> getDossiersForUserRole(Utilisateur utilisateur, DossierFilterRequest filterRequest) {
        switch (utilisateur.getRole()) {
            case AGENT_ANTENNE:
                // Agent Antenne sees only dossiers from their antenne
                if (utilisateur.getAntenne() != null) {
                    return dossierRepository.findByAntenneId(utilisateur.getAntenne().getId());
                }
                return List.of();
                
            case AGENT_GUC:
                // Agent GUC sees all dossiers that have been submitted to GUC
                return dossierRepository.findAll().stream()
                        .filter(d -> !d.getStatus().equals(Dossier.DossierStatus.DRAFT))
                        .collect(Collectors.toList());
                
            case ADMIN:
                // Admin sees all dossiers
                return dossierRepository.findAll();
                
            default:
                return List.of();
        }
    }

    private boolean canAccessDossier(Dossier dossier, Utilisateur utilisateur) {
        switch (utilisateur.getRole()) {
            case ADMIN:
                return true;
            case AGENT_ANTENNE:
                return utilisateur.getAntenne() != null && 
                       dossier.getAntenne().getId().equals(utilisateur.getAntenne().getId());
            case AGENT_GUC:
                return !dossier.getStatus().equals(Dossier.DossierStatus.DRAFT);
            default:
                return false;
        }
    }

    private DossierPermissionsDTO calculatePermissions(Dossier dossier, Utilisateur utilisateur) {
        boolean isDraft = dossier.getStatus() == Dossier.DossierStatus.DRAFT;
        boolean isReturnedForCompletion = dossier.getStatus() == Dossier.DossierStatus.RETURNED_FOR_COMPLETION;
        boolean isSubmitted = dossier.getStatus() == Dossier.DossierStatus.SUBMITTED;
        boolean isInReview = dossier.getStatus() == Dossier.DossierStatus.IN_REVIEW;
        
        // Agent Antenne can edit if it's DRAFT or RETURNED_FOR_COMPLETION
        boolean canEditAtAntenne = isDraft || isReturnedForCompletion;
        
        switch (utilisateur.getRole()) {
            case AGENT_ANTENNE:
                return DossierPermissionsDTO.builder()
                        .peutEtreModifie(canEditAtAntenne)
                        .peutEtreEnvoye(canEditAtAntenne) // Can send if can edit
                        .peutEtreSupprime(isDraft) // Only delete drafts
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

    private List<AvailableActionDTO> getAvailableActionsForRole(Utilisateur.UserRole role) {
        List<AvailableActionDTO> actions = new ArrayList<>();
        
        switch (role) {
            case AGENT_ANTENNE:
                actions.add(AvailableActionDTO.builder()
                        .action("send_to_guc")
                        .label("Envoyer au GUC")
                        .icon("pi-send")
                        .severity("success")
                        .requiresComment(false)
                        .requiresConfirmation(true)
                        .description("Envoyer le dossier au Guichet Unique Central pour traitement")
                        .build());
                break;
                
            case AGENT_GUC:
                actions.add(AvailableActionDTO.builder()
                        .action("send_to_commission")
                        .label("Envoyer à la Commission")
                        .icon("pi-forward")
                        .severity("info")
                        .requiresComment(true)
                        .requiresConfirmation(true)
                        .description("Envoyer le dossier à la Commission AHA-AF pour visite terrain")
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
                break;
        }
        
        return actions;
    }

    private List<AvailableActionDTO> getAvailableActionsForDossier(Dossier dossier, Utilisateur utilisateur) {
        List<AvailableActionDTO> allActions = getAvailableActionsForRole(utilisateur.getRole());
        DossierPermissionsDTO permissions = calculatePermissions(dossier, utilisateur);
        
        return allActions.stream()
                .filter(action -> {
                    switch (action.getAction()) {
                        case "send_to_guc":
                            return permissions.getPeutEtreEnvoye();
                        case "send_to_commission":
                            return permissions.getPeutEnvoyerCommission();
                        case "return_to_antenne":
                            return permissions.getPeutRetournerAntenne();
                        case "reject":
                            return permissions.getPeutRejeter();
                        default:
                            return true;
                    }
                })
                .collect(Collectors.toList());
    }

    // Add other existing helper methods here with appropriate updates for role handling
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
                .filter(d -> filter.getAntenneId() == null || 
                        d.getAntenne().getId().equals(filter.getAntenneId()))
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

    private DossierSummaryDTO mapToDossierSummaryDTO(Dossier dossier, Utilisateur currentUser) {
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
        DossierPermissionsDTO permissions = calculatePermissions(dossier, currentUser);

        // Get available actions
        List<String> availableActions = getAvailableActionsForDossier(dossier, currentUser)
                .stream()
                .map(AvailableActionDTO::getAction)
                .collect(Collectors.toList());

        // Count notes
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
                .formsCompleted((int) pieceJointes.stream().filter(pj -> pj.getStatus() == PieceJointe.DocumentStatus.COMPLETE).count())
                .totalForms(documentsRequis.size())
                .completionPercentage(completionPercentage)
                .availableActions(availableActions)
                .permissions(permissions)
                .notesCount(notesCount)
                .hasUnreadNotes(false) // To be implemented
                .priorite("NORMALE") // To be implemented
                .build();
    }

    // Continue with other existing helper methods...
    private String getDisplayStatus(Dossier.DossierStatus status, WorkflowInstance workflow) {
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

    private DossierStatisticsDTO calculateStatistics(List<Dossier> dossiers, Utilisateur utilisateur) {
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
                .dossiersEnRetard(0L) // To be calculated based on workflow instances
                .tauxCompletion(tauxCompletion)
                .dossiersCeMois(ceMois)
                .dossiersAttenteTraitement(attenteTraitement)
                .dossiersEnCommission(0L) // To be implemented
                .build();
    }

    // Add other missing methods with proper implementations for role-based access
    private boolean canSendToGUC(Dossier dossier, Utilisateur utilisateur) {
        boolean canEdit = dossier.getStatus() == Dossier.DossierStatus.DRAFT || 
                         dossier.getStatus() == Dossier.DossierStatus.RETURNED_FOR_COMPLETION;
        return canEdit && utilisateur.getRole() == Utilisateur.UserRole.AGENT_ANTENNE;
    }

    private boolean canDeleteDossier(Dossier dossier, Utilisateur utilisateur) {
        if (utilisateur.getRole() == Utilisateur.UserRole.ADMIN) {
            return true;
        }
        return dossier.getStatus() == Dossier.DossierStatus.DRAFT &&
                utilisateur.getRole() == Utilisateur.UserRole.AGENT_ANTENNE;
    }

    private void updateWorkflowForGUC(Dossier dossier, Utilisateur utilisateur, String commentaire) {
        // Implementation for workflow update
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

    private void updateWorkflowForCommission(Dossier dossier, Utilisateur utilisateur, SendToCommissionRequest request) {
        List<WorkflowInstance> workflows = workflowInstanceRepository.findByDossierId(dossier.getId());
        
        if (!workflows.isEmpty()) {
            WorkflowInstance current = workflows.get(0);
            current.setEtapeDesignation("Commission AHA-AF");
            current.setEmplacementActuel(WorkflowInstance.EmplacementType.COMMISSION_AHA_AF);
            current.setDateEntree(LocalDateTime.now());
            workflowInstanceRepository.save(current);
        }

        HistoriqueWorkflow history = new HistoriqueWorkflow();
        history.setDossier(dossier);
        history.setEtapeDesignation("Envoi à la Commission");
        history.setEmplacementType(WorkflowInstance.EmplacementType.GUC);
        history.setDateEntree(LocalDateTime.now());
        history.setUtilisateur(utilisateur);
        history.setCommentaire(request.getCommentaire());
        historiqueWorkflowRepository.save(history);
    }

    private void updateWorkflowBackToAntenne(Dossier dossier, Utilisateur utilisateur, ReturnToAntenneRequest request) {
        List<WorkflowInstance> workflows = workflowInstanceRepository.findByDossierId(dossier.getId());
        
        if (!workflows.isEmpty()) {
            WorkflowInstance current = workflows.get(0);
            current.setEtapeDesignation("Phase Antenne - Complétion");
            current.setEmplacementActuel(WorkflowInstance.EmplacementType.ANTENNE);
            current.setDateEntree(LocalDateTime.now());
            workflowInstanceRepository.save(current);
        }

        HistoriqueWorkflow history = new HistoriqueWorkflow();
        history.setDossier(dossier);
        history.setEtapeDesignation("Retour à l'Antenne");
        history.setEmplacementType(WorkflowInstance.EmplacementType.GUC);
        history.setDateEntree(LocalDateTime.now());
        history.setUtilisateur(utilisateur);
        history.setCommentaire(request.getCommentaire());
        historiqueWorkflowRepository.save(history);
    }

    private void updateWorkflowForRejection(Dossier dossier, Utilisateur utilisateur, RejectDossierRequest request) {
        List<WorkflowInstance> workflows = workflowInstanceRepository.findByDossierId(dossier.getId());
        
        if (!workflows.isEmpty()) {
            WorkflowInstance current = workflows.get(0);
            current.setEtapeDesignation("Rejeté");
            current.setEmplacementActuel(WorkflowInstance.EmplacementType.GUC);
            current.setDateEntree(LocalDateTime.now());
            workflowInstanceRepository.save(current);
        }

        HistoriqueWorkflow history = new HistoriqueWorkflow();
        history.setDossier(dossier);
        history.setEtapeDesignation("Rejet du dossier");
        history.setEmplacementType(WorkflowInstance.EmplacementType.GUC);
        history.setDateEntree(LocalDateTime.now());
        history.setUtilisateur(utilisateur);
        history.setCommentaire(request.getCommentaire());
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

    // Add missing method implementations
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
                .priorite("NORMALE")
                .commentaireGUC("")
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
                        .action("action") // To be implemented
                        .build())
                .collect(Collectors.toList());
    }

    private List<PieceJointeDetailDTO> mapToPieceJointeDTOs(List<PieceJointe> pieceJointes, Utilisateur currentUser) {
        return pieceJointes.stream()
                .map(pj -> PieceJointeDetailDTO.builder()
                        .id(pj.getId())
                        .nomFichier(pj.getNomFichier())
                        .cheminFichier(pj.getCheminFichier())
                        .typeDocument(pj.getTypeDocument())
                        .status(pj.getStatus().name())
                        .dateUpload(pj.getDateUpload())
                        .utilisateurNom(pj.getUtilisateur() != null ? pj.getUtilisateur().getPrenom() + " " + pj.getUtilisateur().getNom() : null)
                        .customTitle(pj.getNomFichier())
                        .isOriginalDocument(true)
                        .canDownload(true)
                        .canDelete(currentUser.getRole() == Utilisateur.UserRole.ADMIN || 
                                  currentUser.getRole() == Utilisateur.UserRole.AGENT_ANTENNE)
                        .build())
                .collect(Collectors.toList());
    }

    private List<NoteDTO> mapToNoteDTOs(List<Note> notes, Utilisateur currentUser) {
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
                        .isRead(true) // To be implemented
                        .statut("ACTIVE") // To be implemented
                        .canReply(true)
                        .canEdit(n.getUtilisateurExpediteur() != null && 
                                n.getUtilisateurExpediteur().getId().equals(currentUser.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<AvailableFormDTO> getAvailableForms(Long sousRubriqueId, List<PieceJointe> pieceJointes, Utilisateur currentUser) {
        List<DocumentRequis> documentsRequis = documentRequisRepository.findBySousRubriqueId(sousRubriqueId);
        
        return documentsRequis.stream()
                .map(doc -> {
                    Optional<PieceJointe> relatedPieceJointe = pieceJointes.stream()
                            .filter(pj -> pj.getDocumentRequis() != null && pj.getDocumentRequis().getId().equals(doc.getId()))
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

    private List<String> getValidationErrors(Dossier dossier, List<PieceJointe> pieceJointes) {
        // Implementation for validation logic
        return List.of();
    }
}