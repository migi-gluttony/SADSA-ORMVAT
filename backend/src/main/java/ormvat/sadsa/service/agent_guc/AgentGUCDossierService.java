package ormvat.sadsa.service.agent_guc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;
import ormvat.sadsa.service.workflow.WorkflowService;
import ormvat.sadsa.service.workflow.AuditService;
import ormvat.sadsa.dto.role_based.RoleBasedDossierDTOs.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentGUCDossierService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final WorkflowService workflowService;
    private final AuditService auditService;

    public DossierListResponse getAllDossiers(String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        
        List<Dossier> dossiers = dossierRepository.findAll();
        
        List<DossierSummaryDTO> summaries = dossiers.stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());

        return DossierListResponse.builder()
                .dossiers(summaries)
                .build();
    }

    public DossierListResponse getPendingDossiers(String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        
        List<Dossier> dossiers = dossierRepository.findByStatusIn(List.of(
                Dossier.DossierStatus.SUBMITTED,
                Dossier.DossierStatus.IN_REVIEW
        ));
        
        List<DossierSummaryDTO> summaries = dossiers.stream()
                .filter(this::isPendingGUCAction)
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());

        return DossierListResponse.builder()
                .dossiers(summaries)
                .build();
    }

    public DossierDetailResponse getDossierById(Long id, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        return mapToDetailDTO(dossier);
    }

    @Transactional
    public ActionResponse assignToCommission(Long id, AssignCommissionRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForGUCAction(id);

        dossier.setStatus(Dossier.DossierStatus.IN_REVIEW);
        dossierRepository.save(dossier);

        // Move to Phase 3 (Commission)
        workflowService.moveToStep(dossier.getId(), 3L, user.getId(), 
                                  "Assignation à la Commission - " + request.getCommentaire());

        auditService.logAction(user.getId(), "ASSIGN_COMMISSION", "Dossier", dossier.getId(),
                              "SUBMITTED", "IN_REVIEW", "Assignation à la Commission Visite Terrain");

        return ActionResponse.builder()
                .success(true)
                .message("Dossier assigné à la Commission Visite Terrain")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public ActionResponse approveDossier(Long id, ApproveRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForGUCAction(id);

        dossier.setStatus(Dossier.DossierStatus.APPROVED_AWAITING_FARMER);
        dossier.setDateApprobation(LocalDateTime.now());
        dossierRepository.save(dossier);

        auditService.logAction(user.getId(), "APPROVE_DOSSIER", "Dossier", dossier.getId(),
                              dossier.getStatus().name(), "APPROVED_AWAITING_FARMER", 
                              "Approbation finale - " + request.getCommentaire());

        return ActionResponse.builder()
                .success(true)
                .message("Dossier approuvé avec succès")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public ActionResponse rejectDossier(Long id, RejectRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForGUCAction(id);

        dossier.setStatus(Dossier.DossierStatus.REJECTED);
        dossierRepository.save(dossier);

        auditService.logAction(user.getId(), "REJECT_DOSSIER", "Dossier", dossier.getId(),
                              dossier.getStatus().name(), "REJECTED", 
                              "Rejet - " + request.getMotif() + " - " + request.getCommentaire());

        return ActionResponse.builder()
                .success(true)
                .message("Dossier rejeté")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public ActionResponse returnToAntenne(Long id, ReturnRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForGUCAction(id);

        dossier.setStatus(Dossier.DossierStatus.RETURNED_FOR_COMPLETION);
        dossierRepository.save(dossier);

        // Move back to Phase 1 (Antenne)
        workflowService.moveToStep(dossier.getId(), 1L, user.getId(), 
                                  "Retour à l'antenne - " + request.getMotif() + " - " + request.getCommentaire());

        auditService.logAction(user.getId(), "RETURN_TO_ANTENNE", "Dossier", dossier.getId(),
                              dossier.getStatus().name(), "RETURNED_FOR_COMPLETION", 
                              "Retour à l'antenne - " + request.getMotif());

        return ActionResponse.builder()
                .success(true)
                .message("Dossier retourné à l'antenne")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public List<ActionDTO> getAvailableActions(Long dossierId, String userEmail) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        return getActionsForCurrentState(dossier, dossierId);
    }

    // Helper methods
    private Utilisateur getUserByEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private Dossier getDossierForGUCAction(Long id) {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        if (!canGUCActOn(dossier)) {
            throw new RuntimeException("Action GUC non autorisée sur ce dossier");
        }

        return dossier;
    }

    private boolean canGUCActOn(Dossier dossier) {
        return dossier.getStatus() == Dossier.DossierStatus.SUBMITTED ||
               dossier.getStatus() == Dossier.DossierStatus.IN_REVIEW;
    }

    private boolean isPendingGUCAction(Dossier dossier) {
        if (!canGUCActOn(dossier)) return false;
        
        // Check if current workflow step is in GUC phases (2, 4, 6, 8)
        var currentStep = workflowService.getCurrentStep(dossier.getId());
        if (currentStep == null) return false;
        
        Long etapeId = currentStep.getIdEtape();
        return etapeId == 2L || etapeId == 4L || etapeId == 6L || etapeId == 8L;
    }

    private DossierSummaryDTO mapToSummaryDTO(Dossier dossier) {
        TimingDTO timing = workflowService.getTimingInfo(dossier.getId());

        return DossierSummaryDTO.builder()
                .id(dossier.getId())
                .numeroDossier(dossier.getNumeroDossier())
                .saba(dossier.getSaba())
                .reference(dossier.getReference())
                .agriculteurNom(dossier.getAgriculteur().getNom() + " " + dossier.getAgriculteur().getPrenom())
                .agriculteurCin(dossier.getAgriculteur().getCin())
                .statut(dossier.getStatus().name())
                .dateCreation(dossier.getDateCreation())
                .montantSubvention(dossier.getMontantSubvention())
                .currentStep(timing.getCurrentStep())
                .enRetard(timing.getEnRetard())
                .joursRetard(timing.getJoursRetard())
                .joursRestants(timing.getJoursRestants())
                .availableActions(getActionsForCurrentState(dossier, dossier.getId()))
                .build();
    }

    private DossierDetailResponse mapToDetailDTO(Dossier dossier) {
        TimingDTO timing = workflowService.getTimingInfo(dossier.getId());

        return DossierDetailResponse.builder()
                .id(dossier.getId())
                .numeroDossier(dossier.getNumeroDossier())
                .saba(dossier.getSaba())
                .reference(dossier.getReference())
                .statut(dossier.getStatus().name())
                .dateCreation(dossier.getDateCreation())
                .dateSubmission(dossier.getDateSubmission())
                .dateApprobation(dossier.getDateApprobation())
                .montantSubvention(dossier.getMontantSubvention())
                .agriculteur(mapToAgriculteurDTO(dossier.getAgriculteur()))
                .antenne(mapToAntenneDTO(dossier.getAntenne()))
                .projet(mapToProjetDTO(dossier.getSousRubrique()))
                .utilisateurCreateur(mapToUtilisateurCreateurDTO(dossier.getUtilisateurCreateur()))
                .timing(timing)
                .workflowHistory(mapToWorkflowHistoryDTOs(dossier.getId()))
                .documents(List.of()) // TODO: Implement documents
                .availableActions(getActionsForCurrentState(dossier, dossier.getId()))
                .build();
    }

    private AgriculteurDTO mapToAgriculteurDTO(Agriculteur agriculteur) {
        return AgriculteurDTO.builder()
                .id(agriculteur.getId())
                .nom(agriculteur.getNom())
                .prenom(agriculteur.getPrenom())
                .cin(agriculteur.getCin())
                .telephone(agriculteur.getTelephone())
                .communeRurale(agriculteur.getCommuneRurale() != null ? 
                              agriculteur.getCommuneRurale().getDesignation() : null)
                .douar(agriculteur.getDouar() != null ? 
                      agriculteur.getDouar().getDesignation() : null)
                .build();
    }

    private AntenneDTO mapToAntenneDTO(Antenne antenne) {
        return AntenneDTO.builder()
                .id(antenne.getId())
                .designation(antenne.getDesignation())
                .abreviation(antenne.getAbreviation())
                .build();
    }

    private ProjetDTO mapToProjetDTO(SousRubrique sousRubrique) {
        return ProjetDTO.builder()
                .rubrique(sousRubrique.getRubrique().getDesignation())
                .sousRubrique(sousRubrique.getDesignation())
                .description(sousRubrique.getDescription())
                .build();
    }

    private UtilisateurCreateurDTO mapToUtilisateurCreateurDTO(Utilisateur utilisateur) {
        return UtilisateurCreateurDTO.builder()
                .id(utilisateur.getId())
                .nom(utilisateur.getNom() + " " + utilisateur.getPrenom())
                .email(utilisateur.getEmail())
                .build();
    }

    private List<WorkflowHistoryDTO> mapToWorkflowHistoryDTOs(Long dossierId) {
        return workflowService.getWorkflowHistory(dossierId).stream()
                .map(wi -> WorkflowHistoryDTO.builder()
                        .id(wi.getId())
                        .idEtape(wi.getIdEtape())
                        .phaseNom("Phase " + wi.getIdEtape())
                        .dateEntree(wi.getDateEntree())
                        .dateSortie(wi.getDateSortie())
                        .userId(wi.getIdUser())
                        .userName("User " + wi.getIdUser()) // TODO: Get actual name
                        .commentaire(wi.getCommentaire())
                        .dureeJours(wi.getDateSortie() != null ? 
                                   (int) java.time.Duration.between(wi.getDateEntree(), wi.getDateSortie()).toDays() : null)
                        .build())
                .collect(Collectors.toList());
    }

    private List<ActionDTO> getActionsForCurrentState(Dossier dossier, Long dossierId) {
        var currentStep = workflowService.getCurrentStep(dossier.getId());
        if (currentStep == null) return List.of();

        Long etapeId = currentStep.getIdEtape();
        
        return switch (etapeId.intValue()) {
            case 2 -> List.of( // Phase 2: Initial GUC review
                    ActionDTO.builder()
                            .action("assign-commission")
                            .label("Assigner à la Commission")
                            .endpoint("/api/agent-guc/dossiers/assign-commission/" + dossierId)
                            .method("POST")
                            .build(),
                    ActionDTO.builder()
                            .action("return")
                            .label("Retourner à l'antenne")
                            .endpoint("/api/agent-guc/dossiers/return/" + dossierId)
                            .method("POST")
                            .build(),
                    ActionDTO.builder()
                            .action("reject")
                            .label("Rejeter")
                            .endpoint("/api/agent-guc/dossiers/reject/" + dossierId)
                            .method("POST")
                            .build()
            );
            case 4 -> List.of( // Phase 4: Final approval
                    ActionDTO.builder()
                            .action("approve")
                            .label("Approuver")
                            .endpoint("/api/agent-guc/dossiers/approve/" + dossierId)
                            .method("POST")
                            .build(),
                    ActionDTO.builder()
                            .action("reject")
                            .label("Rejeter")
                            .endpoint("/api/agent-guc/dossiers/reject/" + dossierId)
                            .method("POST")
                            .build()
            );
            case 6, 8 -> List.of( // Phase 6, 8: Realization phases
                    ActionDTO.builder()
                            .action("validate-realization")
                            .label("Valider Réalisation")
                            .endpoint("/api/agent-guc/dossiers/validate-realization/" + dossierId)
                            .method("POST")
                            .build()
            );
            default -> List.of();
        };
    }
}