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
import ormvat.sadsa.service.agent_antenne.DossierAntenneService;

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
    private final PieceJointeRepository pieceJointeRepository;
    private final NoteRepository noteRepository;
    private final WorkflowService workflowService;

    // Services
    private final DossierCommonService dossierCommonService;
    private final DossierAntenneService dossierAntenneService;
    // Note: Other role-specific services are already implemented elsewhere

    /**
     * Get paginated list of dossiers with filtering based on user role and workflow
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

            // Get dossiers based on user role using workflow system
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
     * Get detailed dossier information with workflow-based data
     */
    public DossierDetailResponse getDossierDetail(Long dossierId, String userEmail) {
        try {
            // Get user
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Get dossier
            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Check access permission based on role and workflow
            if (!dossierCommonService.canAccessDossier(dossier, utilisateur)) {
                throw new RuntimeException("Accès non autorisé à ce dossier");
            }

            // Get current workflow information
            WorkflowService.EtapeInfo etapeInfo = null;
            try {
                etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            } catch (Exception e) {
                log.warn("Impossible de récupérer l'étape pour le dossier {}: {}", dossierId, e.getMessage());
            }

            // Get workflow history
            List<HistoriqueWorkflow> workflowHistory = workflowService.getWorkflowHistory(dossier);

            // Get piece jointes
            List<PieceJointe> pieceJointes = pieceJointeRepository.findByDossierId(dossierId);

            // Get notes
            List<Note> notes = noteRepository.findByDossierId(dossierId);

            // Get available forms for this sous-rubrique
            List<AvailableFormDTO> availableForms = dossierCommonService.getAvailableForms(
                    dossier.getSousRubrique().getId(), pieceJointes, utilisateur);

            // Calculate permissions based on role and workflow
            DossierPermissionsDTO permissions = dossierCommonService.calculatePermissions(dossier, utilisateur);

            // Get available actions for this dossier and user
            List<AvailableActionDTO> availableActions = getAvailableActionsForDossier(dossier, utilisateur);

            return DossierDetailResponse.builder()
                    .dossier(dossierCommonService.mapToDossierDetailDTO(dossier))
                    .agriculteur(dossierCommonService.mapToAgriculteurDetailDTO(dossier.getAgriculteur()))
                    .historiqueWorkflow(dossierCommonService.mapToWorkflowHistoryDTOs(workflowHistory))
                    .availableForms(availableForms)
                    .pieceJointes(dossierCommonService.mapToPieceJointeDTOs(pieceJointes, utilisateur))
                    .notes(dossierCommonService.mapToNoteDTOs(notes, utilisateur))
                    .validationErrors(dossierCommonService.getValidationErrors(dossier, pieceJointes))
                    .joursRestants(etapeInfo != null ? etapeInfo.getJoursRestants() : 0)
                    .enRetard(etapeInfo != null ? etapeInfo.getEnRetard() : false)
                    .etapeActuelle(etapeInfo != null ? etapeInfo.getDesignation() : "Non définie")
                    .emplacementActuel(etapeInfo != null ? etapeInfo.getEmplacementActuel() : null)
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
     * Send dossier to Commission (Agent GUC action) - Uses existing implementation
     */
    @Transactional
    public DossierActionResponse sendDossierToCommission(SendToCommissionRequest request, String userEmail) {
        // TODO: Delegate to existing GUC service implementation
        throw new RuntimeException("Cette fonctionnalité utilise l'implémentation GUC existante");
    }

    /**
     * Return dossier to Antenne (Agent GUC action) - Uses existing implementation
     */
    @Transactional
    public DossierActionResponse returnDossierToAntenne(ReturnToAntenneRequest request, String userEmail) {
        // TODO: Delegate to existing GUC service implementation
        throw new RuntimeException("Cette fonctionnalité utilise l'implémentation GUC existante");
    }

    /**
     * Reject dossier (Agent GUC action) - Uses existing implementation
     */
    @Transactional
    public DossierActionResponse rejectDossier(RejectDossierRequest request, String userEmail) {
        // TODO: Delegate to existing GUC service implementation
        throw new RuntimeException("Cette fonctionnalité utilise l'implémentation GUC existante");
    }

    /**
     * Approve dossier (Agent GUC action) - Uses existing implementation
     */
    @Transactional
    public DossierActionResponse approveDossier(Long dossierId, String userEmail, String commentaire) {
        // TODO: Delegate to existing GUC service implementation
        throw new RuntimeException("Cette fonctionnalité utilise l'implémentation GUC existante");
    }

    /**
     * Approve terrain (Commission action) - Uses existing implementation
     */
    @Transactional
    public DossierActionResponse approveTerrain(Long dossierId, String userEmail, String commentaire) {
        // TODO: Delegate to existing Commission service implementation
        throw new RuntimeException("Cette fonctionnalité utilise l'implémentation Commission existante");
    }

    /**
     * Reject terrain (Commission action) - Uses existing implementation
     */
    @Transactional
    public DossierActionResponse rejectTerrain(Long dossierId, String userEmail, String motif, String commentaire) {
        // TODO: Delegate to existing Commission service implementation
        throw new RuntimeException("Cette fonctionnalité utilise l'implémentation Commission existante");
    }

    /**
     * Return to GUC from Commission - Uses existing implementation
     */
    @Transactional
    public DossierActionResponse returnToGUCFromCommission(Long dossierId, String userEmail, String motif, String commentaire) {
        // TODO: Delegate to existing Commission service implementation
        throw new RuntimeException("Cette fonctionnalité utilise l'implémentation Commission existante");
    }

    /**
     * Initialize realization phase (Agent Antenne action) - Updated to use new workflow
     */
    @Transactional
    public DossierActionResponse startRealizationPhase(Long dossierId, String userEmail) {
        return dossierAntenneService.startRealizationPhase(dossierId, userEmail);
    }

    /**
     * NEW: Initialize realization phase - Separate endpoint for clarity
     */
    @Transactional
    public DossierActionResponse initializeRealizationPhase(Long dossierId, String userEmail) {
        return dossierAntenneService.startRealizationPhase(dossierId, userEmail);
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

    /**
     * Process workflow action based on action type
     */
    @Transactional
    public DossierActionResponse processWorkflowAction(Long dossierId, String action, Map<String, Object> parameters, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Check if user can act on current etape
            if (!workflowService.canUserActOnCurrentEtape(dossier, utilisateur)) {
                throw new RuntimeException("Cette action n'est pas autorisée à l'étape actuelle");
            }

            String commentaire = (String) parameters.getOrDefault("commentaire", "");

            switch (action) {
                case "move_to_next":
                    workflowService.moveToNextEtape(dossier, utilisateur, commentaire);
                    break;
                case "return_to_previous":
                    workflowService.returnToPreviousEtape(dossier, utilisateur, commentaire);
                    break;
                default:
                    throw new RuntimeException("Action de workflow non reconnue: " + action);
            }

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Action de workflow exécutée avec succès")
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'exécution de l'action de workflow", e);
            throw new RuntimeException("Erreur lors de l'action: " + e.getMessage());
        }
    }

    /**
     * Get workflow information for dossier
     */
    public WorkflowInfoResponse getWorkflowInfo(Long dossierId, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            List<HistoriqueWorkflow> history = workflowService.getWorkflowHistory(dossier);

            return WorkflowInfoResponse.builder()
                    .currentEtape(etapeInfo.getDesignation())
                    .phase(etapeInfo.getPhase())
                    .ordre(etapeInfo.getOrdre())
                    .dureeJours(etapeInfo.getDureeJours())
                    .joursRestants(etapeInfo.getJoursRestants())
                    .enRetard(etapeInfo.getEnRetard())
                    .dateLimite(etapeInfo.getDateLimite())
                    .emplacementActuel(etapeInfo.getEmplacementActuel())
                    .canUserAct(workflowService.canUserActOnCurrentEtape(dossier, utilisateur))
                    .history(dossierCommonService.mapToWorkflowHistoryDTOs(history))
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des informations de workflow", e);
            throw new RuntimeException("Erreur lors de la récupération: " + e.getMessage());
        }
    }

    // Private helper methods for delegation

    private List<AvailableActionDTO> getAvailableActionsForRole(Utilisateur.UserRole role) {
        switch (role) {
            case AGENT_ANTENNE:
                return dossierAntenneService.getAvailableActionsForAntenne();
            case AGENT_GUC:
            case AGENT_COMMISSION_TERRAIN:
            case ADMIN:
                // These roles use existing implementations
                return new ArrayList<>();
            default:
                return new ArrayList<>();
        }
    }

    private List<AvailableActionDTO> getAvailableActionsForDossier(Dossier dossier, Utilisateur utilisateur) {
        switch (utilisateur.getRole()) {
            case AGENT_ANTENNE:
                return dossierAntenneService.getAvailableActionsForDossier(dossier, utilisateur);
            case AGENT_GUC:
            case AGENT_COMMISSION_TERRAIN:
            case ADMIN:
                // These roles use existing implementations - return empty for now
                return new ArrayList<>();
            default:
                return new ArrayList<>();
        }
    }

    // DTO for workflow information
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class WorkflowInfoResponse {
        private String currentEtape;
        private String phase;
        private Integer ordre;
        private Integer dureeJours;
        private Integer joursRestants;
        private Boolean enRetard;
        private LocalDateTime dateLimite;
        private WorkflowInstance.EmplacementType emplacementActuel;
        private Boolean canUserAct;
        private List<WorkflowHistoryDTO> history;
    }
}