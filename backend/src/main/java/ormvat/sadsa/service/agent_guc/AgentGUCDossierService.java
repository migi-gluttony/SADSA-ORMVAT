package ormvat.sadsa.service.agent_guc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;
import ormvat.sadsa.service.workflow.WorkflowService;
import ormvat.sadsa.service.workflow.AuditService;
import ormvat.sadsa.service.agent_guc.FicheApprobationService;
import ormvat.sadsa.dto.role_based.RoleBasedDossierDTOs.*;
import ormvat.sadsa.dto.agent_guc.FicheApprobationDTOs.FinalApprovalRequest;
import ormvat.sadsa.dto.agent_guc.FicheApprobationDTOs.FinalApprovalResponse;
import ormvat.sadsa.dto.agent_guc.FicheApprobationDTOs.GenerateFicheRequest;
import ormvat.sadsa.dto.agent_guc.FicheApprobationDTOs.FicheApprobationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentGUCDossierService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final VisiteTerrainRepository visiteTerrainRepository;
    private final WorkflowService workflowService;
    private final AuditService auditService;
    private final FicheApprobationService ficheApprobationService;

    public DossierListResponse getAllDossiers(String userEmail) {
        getUserByEmail(userEmail); // Validate user exists
        
        List<Dossier> dossiers = dossierRepository.findAll();
        
        List<DossierSummaryDTO> summaries = dossiers.stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());

        return DossierListResponse.builder()
                .dossiers(summaries)
                .build();
    }

    public DossierListResponse getPendingDossiers(String userEmail) {
        getUserByEmail(userEmail); // Validate user exists
        
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
        getUserByEmail(userEmail); // Validate user exists
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

        // Move to Phase 3 (Commission Terrain)
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
    public ActionResponse assignToServiceTechnique(Long id, AssignServiceTechniqueRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForGUCAction(id);

        // Verify this is an infrastructure project that requires Service Technique
        if (!isInfrastructureProject(dossier)) {
            throw new RuntimeException("Ce type de projet ne nécessite pas l'intervention du Service Technique");
        }

        dossier.setStatus(Dossier.DossierStatus.IN_REVIEW);
        dossierRepository.save(dossier);

        // Move to Phase 7 (Service Technique)
        String commentaireComplet = "Assignation au Service Technique - " + request.getCommentaire();
        if (request.getTypeRealisationPrevue() != null && !request.getTypeRealisationPrevue().trim().isEmpty()) {
            commentaireComplet += " | Type: " + request.getTypeRealisationPrevue();
        }
        if (request.getObservationsSpecifiques() != null && !request.getObservationsSpecifiques().trim().isEmpty()) {
            commentaireComplet += " | Observations: " + request.getObservationsSpecifiques();
        }
        
        workflowService.moveToStep(dossier.getId(), 7L, user.getId(), commentaireComplet);

        auditService.logAction(user.getId(), "ASSIGN_SERVICE_TECHNIQUE", "Dossier", dossier.getId(),
                              dossier.getStatus().name(), "IN_REVIEW", 
                              "Assignation au Service Technique pour réalisation projet d'infrastructure - " + 
                              (request.getTypeRealisationPrevue() != null ? request.getTypeRealisationPrevue() : "Type non spécifié"));

        return ActionResponse.builder()
                .success(true)
                .message("Dossier assigné au Service Technique pour réalisation")
                .timestamp(LocalDateTime.now())
                .nextPhase("Phase Service Technique - Réalisation du projet d'infrastructure")
                .build();
    }

    @Transactional
    public FinalApprovalResponse processFinalApproval(FinalApprovalRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForFinalApproval(request.getDossierId());

        if (request.getApproved()) {
            return processFinalApproval(dossier, request, user);
        } else {
            return processFinalRejection(dossier, request, user);
        }
    }

    @Transactional
    private FinalApprovalResponse processFinalApproval(Dossier dossier, FinalApprovalRequest request, Utilisateur user) {
        try {
            // Update dossier status and details
            dossier.setStatus(Dossier.DossierStatus.AWAITING_FARMER);
            dossier.setDateApprobation(LocalDateTime.now());
            if (request.getMontantApprouve() != null) {
                dossier.setMontantSubvention(request.getMontantApprouve());
            }
            dossierRepository.save(dossier);

            // Generate fiche d'approbation
            GenerateFicheRequest ficheRequest = GenerateFicheRequest.builder()
                    .dossierId(dossier.getId())
                    .commentaireApprobation(request.getCommentaireApprobation())
                    .observationsCommission(request.getObservationsCommission())
                    .montantApprouve(request.getMontantApprouve() != null ? 
                        request.getMontantApprouve() : dossier.getMontantSubvention())
                    .conditionsSpecifiques(request.getConditionsSpecifiques())
                    .build();

            FicheApprobationResponse ficheResponse = ficheApprobationService.generateFicheApprobation(ficheRequest, user.getEmail());

            // Move to next phase (Phase 5 - Antenne Realization)
            workflowService.moveToStep(dossier.getId(), 5L, user.getId(), 
                    "Approbation finale - Fiche générée: " + ficheResponse.getNumeroFiche());

            auditService.logAction(user.getId(), "FINAL_APPROVAL", "Dossier", dossier.getId(),
                    "IN_REVIEW", "APPROVED_AWAITING_FARMER", 
                    "Approbation finale avec génération fiche: " + ficheResponse.getNumeroFiche());

            return FinalApprovalResponse.builder()
                    .success(true)
                    .message("Dossier approuvé avec succès - Fiche d'approbation générée")
                    .newStatut("APPROVED_AWAITING_FARMER")
                    .numeroFiche(ficheResponse.getNumeroFiche())
                    .dateAction(LocalDateTime.now())
                    .ficheGenerated(true)
                    .nextStep("Dossier envoyé à l'antenne pour phase de réalisation")
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'approbation finale du dossier {}: {}", dossier.getId(), e.getMessage());
            throw new RuntimeException("Erreur lors de l'approbation: " + e.getMessage());
        }
    }

    @Transactional
    private FinalApprovalResponse processFinalRejection(Dossier dossier, FinalApprovalRequest request, Utilisateur user) {
        try {
            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.REJECTED);
            dossierRepository.save(dossier);

            // TODO: Generate fiche de rejet here (similar to fiche d'approbation but for rejection)
            String ficheRejetNumber = generateFicheRejetNumber(dossier);

            // Move back to Antenne for archiving (but stays visible for 15 days)
            workflowService.moveToStep(dossier.getId(), 1L, user.getId(), 
                    "Rejet final - Fiche de rejet générée: " + ficheRejetNumber);

            auditService.logAction(user.getId(), "FINAL_REJECTION", "Dossier", dossier.getId(),
                    "IN_REVIEW", "REJECTED", 
                    "Rejet final - Motif: " + request.getMotifRejet());

            return FinalApprovalResponse.builder()
                    .success(true)
                    .message("Dossier rejeté - Fiche de rejet générée")
                    .newStatut("REJECTED")
                    .numeroFiche(ficheRejetNumber)
                    .dateAction(LocalDateTime.now())
                    .ficheGenerated(true)
                    .nextStep("Dossier rejeté - Visible à l'antenne pour 15 jours avant archivage")
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du rejet final du dossier {}: {}", dossier.getId(), e.getMessage());
            throw new RuntimeException("Erreur lors du rejet: " + e.getMessage());
        }
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

    private Dossier getDossierForFinalApproval(Long id) {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        // Check if dossier is in Phase 4 (final approval)
        var currentStep = workflowService.getCurrentStep(dossier.getId());
        if (currentStep == null || !currentStep.getIdEtape().equals(4L)) {
            throw new RuntimeException("Le dossier n'est pas en phase d'approbation finale");
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

    private boolean isInfrastructureProject(Dossier dossier) {
        // Check if the project is an infrastructure project (AMENAGEMENT HYDRO-AGRICOLE)
        return dossier.getSousRubrique() != null && 
               dossier.getSousRubrique().getRubrique() != null &&
               dossier.getSousRubrique().getRubrique().getId() == 3L; // AMENAGEMENT HYDRO-AGRICOLE ET AMELIORATION FONCIERE
    }

    private DossierSummaryDTO mapToSummaryDTO(Dossier dossier) {
        TimingDTO timing = workflowService.getTimingInfo(dossier.getId());
        CommissionFeedbackDTO commissionFeedback = getCommissionFeedback(dossier.getId());

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
                .sousRubriqueDesignation(dossier.getSousRubrique().getDesignation())
                .antenneDesignation(dossier.getAntenne().getDesignation())
                .cdaNom(dossier.getAntenne().getCda() != null ? dossier.getAntenne().getCda().getDescription() : "")
                .notesCount(0) // TODO: Count actual notes
                .availableActions(getActionsForCurrentState(dossier, dossier.getId()))
                // Commission feedback
                .commissionDecisionMade(commissionFeedback.getDecisionMade())
                .commissionApproved(commissionFeedback.getApproved())
                .commissionComments(commissionFeedback.getObservations())
                .commissionDecisionDate(commissionFeedback.getDateDecision())
                .commissionAgentName(commissionFeedback.getAgentCommissionName())
                .build();
    }

    private DossierDetailResponse mapToDetailDTO(Dossier dossier) {
        TimingDTO timing = workflowService.getTimingInfo(dossier.getId());
        List<PieceJointe> pieceJointes = pieceJointeRepository.findByDossierIdOrderByDateUploadDesc(dossier.getId());
        CommissionFeedbackDTO commissionFeedback = getCommissionFeedback(dossier.getId());

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
                .documents(mapToPieceJointeDTOs(pieceJointes))
                .availableActions(getActionsForCurrentState(dossier, dossier.getId()))
                .commissionFeedback(commissionFeedback)
                .build();
    }

    private CommissionFeedbackDTO getCommissionFeedback(Long dossierId) {
        // Get the latest terrain visit for this dossier
        List<VisiteTerrain> visites = visiteTerrainRepository.findByDossierId(dossierId);
        Optional<VisiteTerrain> latestVisit = visites.stream()
                .filter(v -> v.getDateCreation() != null)
                .max((v1, v2) -> v1.getDateCreation().compareTo(v2.getDateCreation()));
        
        if (latestVisit.isPresent()) {
            VisiteTerrain visit = latestVisit.get();
            return CommissionFeedbackDTO.builder()
                    .decisionMade(visit.getStatutVisite() == VisiteTerrain.StatutVisite.TERMINEE)
                    .approved(visit.getApprouve())
                    .observations(visit.getObservations())
                    .recommandations(visit.getRecommandations())
                    .conclusion(visit.getConclusion())
                    .dateVisite(visit.getDateVisite() != null ? visit.getDateVisite().atStartOfDay() : null)
                    .dateDecision(visit.getDateModification())
                    .agentCommissionName(visit.getUtilisateurCommission() != null ? 
                            visit.getUtilisateurCommission().getPrenom() + " " + visit.getUtilisateurCommission().getNom() : null)
                    .coordonneesGPS(visit.getCoordonneesGPS())
                    .conditionsMeteo(visit.getConditionsMeteo())
                    .dureeVisite(visit.getDureeVisite())
                    .photosUrls(List.of()) // TODO: Get actual photos from PieceJointe with type TERRAIN_PHOTO
                    .build();
        }

        return CommissionFeedbackDTO.builder()
                .decisionMade(false)
                .approved(null)
                .observations(null)
                .recommandations(null)
                .conclusion(null)
                .dateVisite(null)
                .dateDecision(null)
                .agentCommissionName(null)
                .coordonneesGPS(null)
                .conditionsMeteo(null)
                .dureeVisite(null)
                .photosUrls(List.of())
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

    private List<DocumentDTO> mapToPieceJointeDTOs(List<PieceJointe> pieceJointes) {
        return pieceJointes.stream()
                .map(pj -> DocumentDTO.builder()
                        .id(pj.getId())
                        .nomDocument(pj.getNomFichier())
                        .cheminFichier(pj.getCheminFichier())
                        .statut(pj.getStatus() != null ? pj.getStatus().name() : "PENDING")
                        .dateUpload(pj.getDateUpload())
                        .documentType(pj.getDocumentType() != null ? pj.getDocumentType().name() : null)
                        .title(pj.getTitle())
                        .description(pj.getDescription())
                        .formData(pj.getFormData())
                        .formConfig(null)
                        .isValidated(pj.getIsValidated())
                        .validationNotes(pj.getValidationNotes())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ActionDTO> getActionsForCurrentState(Dossier dossier, Long dossierId) {
        var currentStep = workflowService.getCurrentStep(dossier.getId());
        if (currentStep == null) return List.of();

        Long etapeId = currentStep.getIdEtape();
        boolean isInfraProject = isInfrastructureProject(dossier);
        
        return switch (etapeId.intValue()) {
            case 2 -> { // Phase 2: Initial GUC review
                List<ActionDTO> actions = new java.util.ArrayList<>();
                
                // Standard actions for all projects
                actions.add(ActionDTO.builder()
                        .action("assign-commission")
                        .label("Assigner à la Commission Terrain")
                        .endpoint("/api/agent-guc/dossiers/assign-commission/" + dossierId)
                        .method("POST")
                        .build());
                
                // Service Technique action only for infrastructure projects
                if (isInfraProject) {
                    actions.add(ActionDTO.builder()
                            .action("assign-service-technique")
                            .label("Assigner au Service Technique")
                            .endpoint("/api/agent-guc/dossiers/assign-service-technique/" + dossierId)
                            .method("POST")
                            .build());
                }
                
                // Common actions
                actions.add(ActionDTO.builder()
                        .action("return")
                        .label("Retourner à l'antenne")
                        .endpoint("/api/agent-guc/dossiers/return/" + dossierId)
                        .method("POST")
                        .build());
                
                actions.add(ActionDTO.builder()
                        .action("reject")
                        .label("Rejeter")
                        .endpoint("/api/agent-guc/dossiers/reject/" + dossierId)
                        .method("POST")
                        .build());
                
                yield actions;
            }
            case 4 -> List.of( // Phase 4: Final approval - ONLY final approval action
                    ActionDTO.builder()
                            .action("final-approval")
                            .label("Finaliser l'Approbation")
                            .endpoint("/api/agent-guc/dossiers/" + dossierId + "/final-approval")
                            .method("GET") // Navigate to final approval page
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

    private String generateFicheRejetNumber(Dossier dossier) {
        return "FR-" + java.time.LocalDate.now().getYear() + "-" + 
               String.format("%02d", java.time.LocalDate.now().getMonthValue()) + "-" + 
               String.format("%06d", dossier.getId());
    }
}