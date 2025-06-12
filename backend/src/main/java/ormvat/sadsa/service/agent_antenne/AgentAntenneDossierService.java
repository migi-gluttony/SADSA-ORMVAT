package ormvat.sadsa.service.agent_antenne;

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
public class AgentAntenneDossierService {

    private final DossierRepository dossierRepository;
    private final AgriculteurRepository agriculteurRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AntenneRepository antenneRepository;
    private final SousRubriqueRepository sousRubriqueRepository;
    private final CommuneRuraleRepository communeRuraleRepository;
    private final DouarRepository douarRepository;
    private final WorkflowService workflowService;
    private final AuditService auditService;

    public DossierListResponse getMyAntenneDossiers(String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        
        List<Dossier> dossiers = dossierRepository.findByAntenneId(user.getAntenne().getId());
        
        List<DossierSummaryDTO> summaries = dossiers.stream()
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

        // Check access
        if (!dossier.getAntenne().getId().equals(user.getAntenne().getId())) {
            throw new RuntimeException("Accès non autorisé");
        }

        return mapToDetailDTO(dossier);
    }

    @Transactional
    public ActionResponse createDossier(CreateDossierRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);

        // Create or find agriculteur
        Agriculteur agriculteur = createOrUpdateAgriculteur(request.getAgriculteur());

        // Create dossier
        Dossier dossier = new Dossier();
        dossier.setNumeroDossier(generateNumeroDossier());
        dossier.setSaba(request.getSaba());
        dossier.setReference(request.getReference());
        dossier.setStatus(Dossier.DossierStatus.DRAFT);
        dossier.setDateCreation(LocalDateTime.now());
        dossier.setMontantSubvention(request.getMontantSubvention());
        dossier.setAgriculteur(agriculteur);
        dossier.setAntenne(user.getAntenne());
        dossier.setSousRubrique(sousRubriqueRepository.findById(request.getSousRubriqueId())
                .orElseThrow(() -> new RuntimeException("Sous-rubrique non trouvée")));
        dossier.setUtilisateurCreateur(user);

        dossier = dossierRepository.save(dossier);

        // Initialize workflow
        workflowService.initializeWorkflow(dossier.getId(), user.getId(), "Création du dossier");

        auditService.logAction(user.getId(), "CREATE_DOSSIER", "Dossier", dossier.getId(),
                              null, "DRAFT", "Création d'un nouveau dossier");

        return ActionResponse.builder()
                .success(true)
                .message("Dossier créé avec succès")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public ActionResponse updateDossier(Long id, UpdateDossierRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForUpdate(id, user);

        String oldValues = String.format("SABA: %s, Ref: %s, Montant: %s", 
                                        dossier.getSaba(), dossier.getReference(), dossier.getMontantSubvention());

        // Update agriculteur
        Agriculteur agriculteur = createOrUpdateAgriculteur(request.getAgriculteur());
        dossier.setAgriculteur(agriculteur);

        // Update dossier
        dossier.setSaba(request.getSaba());
        dossier.setReference(request.getReference());
        dossier.setMontantSubvention(request.getMontantSubvention());
        dossier.setSousRubrique(sousRubriqueRepository.findById(request.getSousRubriqueId())
                .orElseThrow(() -> new RuntimeException("Sous-rubrique non trouvée")));

        dossierRepository.save(dossier);

        String newValues = String.format("SABA: %s, Ref: %s, Montant: %s", 
                                        dossier.getSaba(), dossier.getReference(), dossier.getMontantSubvention());

        auditService.logAction(user.getId(), "UPDATE_DOSSIER", "Dossier", dossier.getId(),
                              oldValues, newValues, "Modification du dossier");

        return ActionResponse.builder()
                .success(true)
                .message("Dossier modifié avec succès")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public ActionResponse submitDossier(Long id, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForUpdate(id, user);

        dossier.setStatus(Dossier.DossierStatus.SUBMITTED);
        dossier.setDateSubmission(LocalDateTime.now());
        dossierRepository.save(dossier);

        // Move to Phase 2 (GUC)
        workflowService.moveToStep(dossier.getId(), 2L, user.getId(), "Soumission au GUC");

        auditService.logAction(user.getId(), "SUBMIT_DOSSIER", "Dossier", dossier.getId(),
                              "DRAFT", "SUBMITTED", "Soumission du dossier au GUC");

        return ActionResponse.builder()
                .success(true)
                .message("Dossier soumis au GUC avec succès")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public ActionResponse deleteDossier(Long id, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForUpdate(id, user);

        auditService.logAction(user.getId(), "DELETE_DOSSIER", "Dossier", dossier.getId(),
                              dossier.getStatus().name(), "DELETED", "Suppression du dossier");

        dossierRepository.delete(dossier);

        return ActionResponse.builder()
                .success(true)
                .message("Dossier supprimé avec succès")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public List<ActionDTO> getAvailableActions(Long dossierId, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        // Check access
        if (!dossier.getAntenne().getId().equals(user.getAntenne().getId())) {
            return List.of();
        }

        return getActionsForStatus(dossier.getStatus(), dossierId);
    }

    // Helper methods
    private Utilisateur getUserByEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private Dossier getDossierForUpdate(Long id, Utilisateur user) {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        if (!dossier.getAntenne().getId().equals(user.getAntenne().getId())) {
            throw new RuntimeException("Accès non autorisé");
        }

        if (dossier.getStatus() != Dossier.DossierStatus.DRAFT && 
            dossier.getStatus() != Dossier.DossierStatus.RETURNED_FOR_COMPLETION) {
            throw new RuntimeException("Dossier ne peut plus être modifié");
        }

        return dossier;
    }

    private Agriculteur createOrUpdateAgriculteur(AgriculteurCreateDTO dto) {
        // Check if agriculteur exists by CIN
        Agriculteur agriculteur = agriculteurRepository.findByCin(dto.getCin())
                .orElse(new Agriculteur());

        agriculteur.setNom(dto.getNom());
        agriculteur.setPrenom(dto.getPrenom());
        agriculteur.setCin(dto.getCin());
        agriculteur.setTelephone(dto.getTelephone());
        
        if (dto.getCommuneRuraleId() != null) {
            agriculteur.setCommuneRurale(communeRuraleRepository.findById(dto.getCommuneRuraleId())
                    .orElse(null));
        }
        
        if (dto.getDouarId() != null) {
            agriculteur.setDouar(douarRepository.findById(dto.getDouarId())
                    .orElse(null));
        }

        return agriculteurRepository.save(agriculteur);
    }

    private String generateNumeroDossier() {
        return "DOS-" + System.currentTimeMillis();
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
                .availableActions(getActionsForStatus(dossier.getStatus(), dossier.getId()))
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
                .availableActions(getActionsForStatus(dossier.getStatus(), dossier.getId()))
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

    private List<ActionDTO> getActionsForStatus(Dossier.DossierStatus status, Long dossierId) {
        return switch (status) {
            case DRAFT, RETURNED_FOR_COMPLETION -> List.of(
                    ActionDTO.builder()
                            .action("update")
                            .label("Modifier")
                            .endpoint("/api/agent-antenne/dossiers/update/" + dossierId)
                            .method("PUT")
                            .build(),
                    ActionDTO.builder()
                            .action("submit")
                            .label("Soumettre")
                            .endpoint("/api/agent-antenne/dossiers/submit/" + dossierId)
                            .method("POST")
                            .build(),
                    ActionDTO.builder()
                            .action("delete")
                            .label("Supprimer")
                            .endpoint("/api/agent-antenne/dossiers/delete/" + dossierId)
                            .method("DELETE")
                            .build()
            );
            case AWAITING_FARMER -> List.of(
                    ActionDTO.builder()
                            .action("start_realization")
                            .label("Démarrer Réalisation")
                            .endpoint("/api/agent-antenne/dossiers/start-realization/" + dossierId)
                            .method("POST")
                            .build()
            );
            default -> List.of();
        };
    }
}