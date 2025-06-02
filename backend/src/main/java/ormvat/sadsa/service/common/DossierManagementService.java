package ormvat.sadsa.service.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ormvat.sadsa.dto.common.DossierCommonDTOs.*;
import ormvat.sadsa.dto.agent_antenne.DossierAntenneActionDTOs.*;
import ormvat.sadsa.dto.agent_guc.DossierGUCActionDTOs.*;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;
import ormvat.sadsa.service.admin.DossierAdminService;
import ormvat.sadsa.service.agent_antenne.DossierAntenneService;
import ormvat.sadsa.service.agent_commission.DossierCommissionService;
import ormvat.sadsa.service.agent_guc.DossierGUCService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    // Role-specific services
    private final DossierCommonService dossierCommonService;
    private final DossierAntenneService dossierAntenneService;
    private final DossierGUCService dossierGUCService;
    private final DossierCommissionService dossierCommissionService;
    private final DossierAdminService dossierAdminService;

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
            List<Dossier> allDossiers = dossierCommonService.getDossiersForUserRole(utilisateur, filterRequest);

            // Apply filters
            List<Dossier> filteredDossiers = dossierCommonService.applyFilters(allDossiers, filterRequest);

            // Calculate pagination manually for filtered results
            int totalElements = filteredDossiers.size();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalElements);

            List<Dossier> paginatedDossiers = filteredDossiers.subList(startIndex, endIndex);

            // Convert to DTOs with role-specific permissions
            List<DossierSummaryDTO> dossierSummaries = paginatedDossiers.stream()
                    .map(d -> dossierCommonService.mapToDossierSummaryDTO(d, utilisateur))
                    .collect(Collectors.toList());

            // Calculate statistics
            DossierStatisticsDTO statistics = dossierCommonService.calculateStatistics(allDossiers, utilisateur);

            // Get available actions for this user role
            List<AvailableActionDTO> availableActions = getAvailableActionsForRole(utilisateur.getRole());

            return DossierListResponse.builder()
                    .dossiers(dossierSummaries)
                    .totalCount((long) totalElements)
                    .currentPage(page)
                    .pageSize(size)
                    .totalPages((int) Math.ceil((double) totalElements / size))
                    .currentUserRole(utilisateur.getRole().name())
                    .currentUserAntenne(
                            utilisateur.getAntenne() != null ? utilisateur.getAntenne().getDesignation() : "N/A")
                    .currentUserCDA(utilisateur.getAntenne() != null && utilisateur.getAntenne().getCda() != null
                            ? utilisateur.getAntenne().getCda().getDescription()
                            : "N/A")
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
            if (!dossierCommonService.canAccessDossier(dossier, utilisateur)) {
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
            List<AvailableFormDTO> availableForms = dossierCommonService.getAvailableForms(
                    dossier.getSousRubrique().getId(), pieceJointes, utilisateur);

            // Calculate permissions based on role
            DossierPermissionsDTO permissions = dossierCommonService.calculatePermissions(dossier, utilisateur);

            // Get available actions for this dossier and user
            List<AvailableActionDTO> availableActions = getAvailableActionsForDossier(dossier, utilisateur);

            // Calculate workflow info
            Map<String, Object> workflowInfo = dossierCommonService.calculateWorkflowInfo(currentWorkflow);

            return DossierDetailResponse.builder()
                    .dossier(dossierCommonService.mapToDossierDetailDTO(dossier))
                    .agriculteur(dossierCommonService.mapToAgriculteurDetailDTO(dossier.getAgriculteur()))
                    .historiqueWorkflow(dossierCommonService.mapToWorkflowHistoryDTOs(workflowHistory))
                    .availableForms(availableForms)
                    .pieceJointes(dossierCommonService.mapToPieceJointeDTOs(pieceJointes, utilisateur))
                    .notes(dossierCommonService.mapToNoteDTOs(notes, utilisateur))
                    .validationErrors(dossierCommonService.getValidationErrors(dossier, pieceJointes))
                    .joursRestants((Integer) workflowInfo.get("joursRestants"))
                    .enRetard((Boolean) workflowInfo.get("enRetard"))
                    .etapeActuelle((String) workflowInfo.get("etapeActuelle"))
                    .emplacementActuel((WorkflowInstance.EmplacementType) workflowInfo.get("emplacementActuel"))
                    .permissions(permissions)
                    .availableActions(availableActions)
                    .currentUserRole(utilisateur.getRole().name())
                    .currentUserAntenne(
                            utilisateur.getAntenne() != null ? utilisateur.getAntenne().getDesignation() : null)
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
        return dossierAntenneService.sendDossierToGUC(request, userEmail);
    }

    /**
     * Send dossier to Commission (Agent GUC action)
     */
    @Transactional
    public DossierActionResponse sendDossierToCommission(SendToCommissionRequest request, String userEmail) {
        return dossierGUCService.sendDossierToCommission(request, userEmail);
    }

    /**
     * Return dossier to Antenne (Agent GUC action)
     */
    @Transactional
    public DossierActionResponse returnDossierToAntenne(ReturnToAntenneRequest request, String userEmail) {
        return dossierGUCService.returnDossierToAntenne(request, userEmail);
    }

    /**
     * Reject dossier (Agent GUC action)
     */
    @Transactional
    public DossierActionResponse rejectDossier(RejectDossierRequest request, String userEmail) {
        return dossierGUCService.rejectDossier(request, userEmail);
    }

    /**
     * Delete dossier (role-based)
     */
    @Transactional
    public DossierActionResponse deleteDossier(DeleteDossierRequest request, String userEmail) {
        return dossierAntenneService.deleteDossier(request, userEmail);
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
            dossierCommonService.createAuditTrail("AJOUT_NOTE", dossier, utilisateur, request.getObjet());

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

    // Private helper methods for delegation

    private List<AvailableActionDTO> getAvailableActionsForRole(Utilisateur.UserRole role) {
        switch (role) {
            case AGENT_ANTENNE:
                return dossierAntenneService.getAvailableActionsForAntenne();
            case AGENT_GUC:
                return dossierGUCService.getAvailableActionsForGUC();
            case AGENT_COMMISSION_TERRAIN:
                return dossierCommissionService.getAvailableActionsForCommission();
            case ADMIN:
                return dossierAdminService.getAvailableActionsForAdmin();
            default:
                return new ArrayList<>();
        }
    }

    private List<AvailableActionDTO> getAvailableActionsForDossier(Dossier dossier, Utilisateur utilisateur) {
        switch (utilisateur.getRole()) {
            case AGENT_ANTENNE:
                return dossierAntenneService.getAvailableActionsForDossier(dossier, utilisateur);
            case AGENT_GUC:
                return dossierGUCService.getAvailableActionsForDossier(dossier, utilisateur);
            case AGENT_COMMISSION_TERRAIN:
                return dossierCommissionService.getAvailableActionsForDossier(dossier, utilisateur);
            case ADMIN:
                return dossierAdminService.getAvailableActionsForDossier(dossier, utilisateur);
            default:
                return new ArrayList<>();
        }
    }
}