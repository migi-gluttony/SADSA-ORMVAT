package ormvat.sadsa.service.admin;

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
public class AdminDossierService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final WorkflowService workflowService;
    private final AuditService auditService;

    public DossierListResponse getAllDossiers(String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        validateAdminRole(user);
        
        List<Dossier> dossiers = dossierRepository.findAll();
        
        List<DossierSummaryDTO> summaries = dossiers.stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());

        return DossierListResponse.builder()
                .dossiers(summaries)
                .build();
    }

    public DossierDetailResponse getDossierById(Long id, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        validateAdminRole(user);
        
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        return mapToDetailDTO(dossier);
    }

    @Transactional
    public ActionResponse forceStatusChange(Long id, String newStatus, String reason, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        validateAdminRole(user);
        
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        String oldStatus = dossier.getStatus().name();
        dossier.setStatus(Dossier.DossierStatus.valueOf(newStatus));
        dossierRepository.save(dossier);

        auditService.logAction(user.getId(), "ADMIN_FORCE_STATUS_CHANGE", "Dossier", dossier.getId(),
                              oldStatus, newStatus, "Admin force status change: " + reason);

        return ActionResponse.builder()
                .success(true)
                .message("Statut forcé par admin: " + newStatus)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public ActionResponse forceWorkflowMove(Long id, Long targetPhase, String reason, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        validateAdminRole(user);
        
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        workflowService.moveToStep(dossier.getId(), targetPhase, user.getId(), 
                                  "Admin force workflow move: " + reason);

        auditService.logAction(user.getId(), "ADMIN_FORCE_WORKFLOW_MOVE", "Dossier", dossier.getId(),
                              null, "Phase " + targetPhase, "Admin force workflow move: " + reason);

        return ActionResponse.builder()
                .success(true)
                .message("Workflow forcé vers Phase " + targetPhase)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public ActionResponse deleteDossier(Long id, String reason, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        validateAdminRole(user);
        
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        auditService.logAction(user.getId(), "ADMIN_DELETE_DOSSIER", "Dossier", dossier.getId(),
                              dossier.getStatus().name(), "DELETED", "Admin deletion: " + reason);

        dossierRepository.delete(dossier);

        return ActionResponse.builder()
                .success(true)
                .message("Dossier supprimé par admin")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public List<ActionDTO> getAvailableActions(Long dossierId, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        validateAdminRole(user);
        
        return getAdminActionsForDossier(dossierId);
    }

    // Helper methods
    private Utilisateur getUserByEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private void validateAdminRole(Utilisateur user) {
        if (user.getRole() != Utilisateur.UserRole.ADMIN) {
            throw new RuntimeException("Accès réservé aux administrateurs");
        }
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
                .availableActions(getAdminActionsForDossier(dossier.getId()))
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
                .availableActions(getAdminActionsForDossier(dossier.getId()))
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

    private List<ActionDTO> getAdminActionsForDossier(Long dossierId) {
        return List.of(
                ActionDTO.builder()
                        .action("force-status")
                        .label("Forcer Statut")
                        .endpoint("/api/admin/dossiers/force-status/" + dossierId)
                        .method("POST")
                        .build(),
                ActionDTO.builder()
                        .action("force-workflow")
                        .label("Forcer Workflow")
                        .endpoint("/api/admin/dossiers/force-workflow/" + dossierId)
                        .method("POST")
                        .build(),
                ActionDTO.builder()
                        .action("delete")
                        .label("Supprimer (Admin)")
                        .endpoint("/api/admin/dossiers/delete/" + dossierId)
                        .method("DELETE")
                        .build(),
                ActionDTO.builder()
                        .action("view-audit")
                        .label("Voir Audit Trail")
                        .endpoint("/api/admin/dossiers/audit/" + dossierId)
                        .method("GET")
                        .build()
        );
    }
}