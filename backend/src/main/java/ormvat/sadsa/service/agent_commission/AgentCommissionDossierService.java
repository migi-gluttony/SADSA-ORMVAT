package ormvat.sadsa.service.agent_commission;

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
public class AgentCommissionDossierService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final WorkflowService workflowService;
    private final AuditService auditService;

    public DossierListResponse getDossiersForInspection(String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        
        List<Dossier> dossiers = dossierRepository.findByStatus(Dossier.DossierStatus.IN_REVIEW)
                .stream()
                .filter(this::isInCommissionPhase)
                .collect(Collectors.toList());
        
        List<DossierSummaryDTO> summaries = dossiers.stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());

        return DossierListResponse.builder()
                .dossiers(summaries)
                .build();
    }

    public DossierListResponse getMyAssignedDossiers(String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        
        // For now, return all dossiers in commission phase
        // TODO: Implement actual assignment logic
        return getDossiersForInspection(userEmail);
    }

    public DossierDetailResponse getDossierById(Long id, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        if (!isInCommissionPhase(dossier)) {
            throw new RuntimeException("Dossier non accessible par la Commission");
        }

        return mapToDetailDTO(dossier);
    }

    @Transactional
    public ActionResponse approveTerrainInspection(Long id, TerrainApprovalRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForCommissionAction(id);

        // Move to Phase 4 (Final GUC approval)
        workflowService.moveToStep(dossier.getId(), 4L, user.getId(), 
                                  "Terrain approuvé - " + request.getCommentaire());

        auditService.logAction(user.getId(), "APPROVE_TERRAIN", "Dossier", dossier.getId(),
                              "Phase 3", "Phase 4", 
                              "Approbation terrain - " + request.getCommentaire());

        return ActionResponse.builder()
                .success(true)
                .message("Terrain approuvé - Dossier envoyé au GUC pour approbation finale")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public ActionResponse rejectTerrainInspection(Long id, TerrainRejectionRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForCommissionAction(id);

        dossier.setStatus(Dossier.DossierStatus.REJECTED);
        dossierRepository.save(dossier);

        auditService.logAction(user.getId(), "REJECT_TERRAIN", "Dossier", dossier.getId(),
                              "IN_REVIEW", "REJECTED", 
                              "Rejet terrain - " + request.getMotif() + " - " + request.getCommentaire());

        return ActionResponse.builder()
                .success(true)
                .message("Terrain rejeté - Dossier refusé")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public ActionResponse scheduleVisit(Long id, ScheduleVisitRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForCommissionAction(id);

        auditService.logAction(user.getId(), "SCHEDULE_VISIT", "Dossier", dossier.getId(),
                              null, request.getDateVisite().toString(), 
                              "Visite programmée - " + request.getCommentaire());

        return ActionResponse.builder()
                .success(true)
                .message("Visite terrain programmée pour le " + request.getDateVisite().toLocalDate())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public List<ActionDTO> getAvailableActions(Long dossierId, String userEmail) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        if (!isInCommissionPhase(dossier)) {
            return List.of();
        }

        return getActionsForCommissionPhase(dossierId);
    }

    // Helper methods
    private Utilisateur getUserByEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private Dossier getDossierForCommissionAction(Long id) {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        if (!isInCommissionPhase(dossier)) {
            throw new RuntimeException("Action Commission non autorisée sur ce dossier");
        }

        return dossier;
    }

    private boolean isInCommissionPhase(Dossier dossier) {
        if (dossier.getStatus() != Dossier.DossierStatus.IN_REVIEW) return false;
        
        // Check if current workflow step is Phase 3 (Commission)
        var currentStep = workflowService.getCurrentStep(dossier.getId());
        if (currentStep == null) return false;
        
        return currentStep.getIdEtape() == 3L;
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
                .availableActions(getActionsForCommissionPhase(dossier.getId()))
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
                .availableActions(getActionsForCommissionPhase(dossier.getId()))
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

    private List<ActionDTO> getActionsForCommissionPhase(Long dossierId) {
        return List.of(
                ActionDTO.builder()
                        .action("schedule-visit")
                        .label("Programmer Visite")
                        .endpoint("/api/agent-commission/dossiers/schedule-visit/" + dossierId)
                        .method("POST")
                        .build(),
                ActionDTO.builder()
                        .action("approve-terrain")
                        .label("Approuver Terrain")
                        .endpoint("/api/agent-commission/dossiers/approve-terrain/" + dossierId)
                        .method("POST")
                        .build(),
                ActionDTO.builder()
                        .action("reject-terrain")
                        .label("Rejeter Terrain")
                        .endpoint("/api/agent-commission/dossiers/reject-terrain/" + dossierId)
                        .method("POST")
                        .build()
        );
    }
}