package ormvat.sadsa.service.service_technique;

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
public class ServiceTechniqueDossierService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final WorkflowService workflowService;
    private final AuditService auditService;

    public DossierListResponse getDossiersForImplementation(String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        
        List<Dossier> dossiers = dossierRepository.findByStatus(Dossier.DossierStatus.REALIZATION_IN_PROGRESS)
                .stream()
                .filter(this::isInServiceTechniquePhase)
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
        
        // For now, return all dossiers in service technique phase
        // TODO: Implement actual assignment logic
        return getDossiersForImplementation(userEmail);
    }

    public DossierDetailResponse getDossierById(Long id, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        if (!isInServiceTechniquePhase(dossier)) {
            throw new RuntimeException("Dossier non accessible par le Service Technique");
        }

        return mapToDetailDTO(dossier);
    }

    @Transactional
    public ActionResponse verifyImplementation(Long id, VerificationRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForServiceTechniqueAction(id);

        auditService.logAction(user.getId(), "VERIFY_IMPLEMENTATION", "Dossier", dossier.getId(),
                              null, "VERIFIED", 
                              "Vérification implémentation - " + request.getCommentaire());

        return ActionResponse.builder()
                .success(true)
                .message("Implémentation vérifiée")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public ActionResponse markComplete(Long id, CompletionRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForServiceTechniqueAction(id);

        // Move to Phase 8 (Final GUC validation)
        workflowService.moveToStep(dossier.getId(), 8L, user.getId(), 
                                  "Réalisation terminée - " + request.getCommentaire());

        auditService.logAction(user.getId(), "COMPLETE_IMPLEMENTATION", "Dossier", dossier.getId(),
                              "Phase 7", "Phase 8", 
                              "Réalisation terminée - " + request.getCommentaire());

        return ActionResponse.builder()
                .success(true)
                .message("Réalisation marquée comme terminée - Envoyé au GUC pour validation finale")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public ActionResponse reportIssues(Long id, IssueRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForServiceTechniqueAction(id);

        auditService.logAction(user.getId(), "REPORT_ISSUES", "Dossier", dossier.getId(),
                              null, "ISSUES_REPORTED", 
                              "Problèmes signalés - " + request.getProbleme() + " - " + request.getCommentaire());

        return ActionResponse.builder()
                .success(true)
                .message("Problèmes signalés")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public List<ActionDTO> getAvailableActions(Long dossierId, String userEmail) {
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        if (!isInServiceTechniquePhase(dossier)) {
            return List.of();
        }

        return getActionsForServiceTechniquePhase(dossierId);
    }

    // Helper methods
    private Utilisateur getUserByEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private Dossier getDossierForServiceTechniqueAction(Long id) {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        if (!isInServiceTechniquePhase(dossier)) {
            throw new RuntimeException("Action Service Technique non autorisée sur ce dossier");
        }

        return dossier;
    }

    private boolean isInServiceTechniquePhase(Dossier dossier) {
        if (dossier.getStatus() != Dossier.DossierStatus.REALIZATION_IN_PROGRESS) return false;
        
        // Check if current workflow step is Phase 7 (Service Technique)
        var currentStep = workflowService.getCurrentStep(dossier.getId());
        if (currentStep == null) return false;
        
        return currentStep.getIdEtape() == 7L;
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
                .availableActions(getActionsForServiceTechniquePhase(dossier.getId()))
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
                .availableActions(getActionsForServiceTechniquePhase(dossier.getId()))
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

    private List<ActionDTO> getActionsForServiceTechniquePhase(Long dossierId) {
        return List.of(
                ActionDTO.builder()
                        .action("verify")
                        .label("Vérifier Implémentation")
                        .endpoint("/api/service-technique/dossiers/verify/" + dossierId)
                        .method("POST")
                        .build(),
                ActionDTO.builder()
                        .action("complete")
                        .label("Marquer Terminé")
                        .endpoint("/api/service-technique/dossiers/complete/" + dossierId)
                        .method("POST")
                        .build(),
                ActionDTO.builder()
                        .action("report-issues")
                        .label("Signaler Problèmes")
                        .endpoint("/api/service-technique/dossiers/report-issues/" + dossierId)
                        .method("POST")
                        .build()
        );
    }
}