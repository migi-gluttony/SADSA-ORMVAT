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
public class AgentGUCDossierService {    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final VisiteTerrainRepository visiteTerrainRepository;
    private final VisiteImplementationRepository visiteImplementationRepository;
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
                Dossier.DossierStatus.IN_REVIEW,
                Dossier.DossierStatus.REALIZATION_IN_PROGRESS
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
    public ActionResponse sendToServiceTechnique(Long id, AssignCommissionRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForGUCAction(id);

        dossier.setStatus(Dossier.DossierStatus.REALIZATION_IN_PROGRESS);
        dossierRepository.save(dossier);

        // Move to Phase 7 (Service Technique)
        workflowService.moveToStep(dossier.getId(), 7L, user.getId(), 
                                  "Envoi au Service Technique - " + request.getCommentaire());

        auditService.logAction(user.getId(), "SEND_TO_SERVICE_TECHNIQUE", "Dossier", dossier.getId(),
                              dossier.getStatus().name(), "REALIZATION_IN_PROGRESS", 
                              "Envoi au Service Technique pour supervision de la réalisation");

        return ActionResponse.builder()
                .success(true)
                .message("Dossier envoyé au Service Technique pour supervision de la réalisation")
                .timestamp(LocalDateTime.now())
                .nextPhase("Phase 7: Service Technique")
                .newStatus("REALIZATION_IN_PROGRESS")
                .build();
    }

    @Transactional
    public FinalRealizationApprovalResponse processFinalRealizationApproval(FinalRealizationApprovalRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForFinalRealizationApproval(request.getDossierId());

        // Get Service Technique feedback
        ServiceTechniqueFeedbackDTO serviceTechniqueFeedback = getServiceTechniqueFeedback(dossier.getId());
        
        if (!serviceTechniqueFeedback.getDecisionMade()) {
            throw new RuntimeException("Le Service Technique n'a pas encore terminé son évaluation");
        }

        if (request.getApproved()) {
            return processFinalRealizationApproval(dossier, request, user, serviceTechniqueFeedback);
        } else {
            return processFinalRealizationRejection(dossier, request, user, serviceTechniqueFeedback);
        }
    }

    @Transactional
    private FinalRealizationApprovalResponse processFinalRealizationApproval(Dossier dossier, 
                                                                           FinalRealizationApprovalRequest request, 
                                                                           Utilisateur user,
                                                                           ServiceTechniqueFeedbackDTO serviceTechniqueFeedback) {
        try {
            // Update dossier status to completed
            dossier.setStatus(Dossier.DossierStatus.COMPLETED);
            dossierRepository.save(dossier);

            // Generate archiving number
            String numeroArchivage = generateArchiveNumber(dossier);

            // Create final audit trail
            String auditDetails = String.format(
                "Approbation finale de la réalisation - Service Technique: %s - Commentaire: %s - Numéro d'archivage: %s",
                serviceTechniqueFeedback.getApproved() ? "Approuvé" : "Avec réserves",
                request.getCommentaireApprobation(),
                numeroArchivage
            );

            auditService.logAction(user.getId(), "FINAL_REALIZATION_APPROVAL", "Dossier", dossier.getId(),
                                  "REALIZATION_IN_PROGRESS", "COMPLETED", auditDetails);

            log.info("Réalisation finalement approuvée pour dossier {} par {} - Numéro d'archivage: {}", 
                    dossier.getId(), user.getEmail(), numeroArchivage);

            return FinalRealizationApprovalResponse.builder()
                    .success(true)
                    .message("Réalisation approuvée avec succès - Dossier archivé")
                    .newStatut("COMPLETED")
                    .dateAction(LocalDateTime.now())
                    .dateArchivage(LocalDateTime.now())
                    .numeroArchivage(numeroArchivage)
                    .nextStep("Dossier archivé - Processus terminé")
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'approbation finale de la réalisation du dossier {}: {}", dossier.getId(), e.getMessage());
            throw new RuntimeException("Erreur lors de l'approbation finale: " + e.getMessage());
        }
    }

    @Transactional
    private FinalRealizationApprovalResponse processFinalRealizationRejection(Dossier dossier, 
                                                                            FinalRealizationApprovalRequest request, 
                                                                            Utilisateur user,
                                                                            ServiceTechniqueFeedbackDTO serviceTechniqueFeedback) {
        try {
            // Update dossier status to rejected
            dossier.setStatus(Dossier.DossierStatus.REJECTED);
            dossierRepository.save(dossier);

            // Generate rejection archive number
            String numeroArchivage = generateRejectionArchiveNumber(dossier);

            // Create audit trail
            String auditDetails = String.format(
                "Rejet final de la réalisation - Motif: %s - Service Technique avait dit: %s - Numéro d'archivage: %s",
                request.getMotifRejet(),
                serviceTechniqueFeedback.getConclusion(),
                numeroArchivage
            );

            auditService.logAction(user.getId(), "FINAL_REALIZATION_REJECTION", "Dossier", dossier.getId(),
                                  "REALIZATION_IN_PROGRESS", "REJECTED", auditDetails);

            log.info("Réalisation finalement rejetée pour dossier {} par {} - Motif: {}", 
                    dossier.getId(), user.getEmail(), request.getMotifRejet());

            return FinalRealizationApprovalResponse.builder()
                    .success(true)
                    .message("Réalisation rejetée - Dossier archivé")
                    .newStatut("REJECTED")
                    .dateAction(LocalDateTime.now())
                    .dateArchivage(LocalDateTime.now())
                    .numeroArchivage(numeroArchivage)
                    .nextStep("Dossier rejeté et archivé")
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du rejet final de la réalisation du dossier {}: {}", dossier.getId(), e.getMessage());
            throw new RuntimeException("Erreur lors du rejet final: " + e.getMessage());
        }
    }

    // Legacy method for backward compatibility - now redirects to new method
    @Transactional
    public ActionResponse validateRealization(Long id, ApproveRequest request, String userEmail) {
        FinalRealizationApprovalRequest finalRequest = FinalRealizationApprovalRequest.builder()
                .dossierId(id)
                .approved(true)
                .commentaireApprobation(request.getCommentaire())
                .observationsFinales("Validation automatique via méthode legacy")
                .build();

        FinalRealizationApprovalResponse response = processFinalRealizationApproval(finalRequest, userEmail);

        return ActionResponse.builder()
                .success(response.getSuccess())
                .message(response.getMessage())
                .timestamp(response.getDateAction())
                .newStatus(response.getNewStatut())
                .nextPhase(response.getNextStep())
                .build();
    }

    @Transactional
    public FinalApprovalResponse processFinalApproval(FinalApprovalRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForFinalApproval(request.getDossierId());

        if (request.getApproved()) {
            return processFinalApprovalGranted(dossier, request, user);
        } else {
            return processFinalApprovalRejection(dossier, request, user);
        }
    }

    @Transactional
    private FinalApprovalResponse processFinalApprovalGranted(Dossier dossier, FinalApprovalRequest request, Utilisateur user) {
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
    private FinalApprovalResponse processFinalApprovalRejection(Dossier dossier, FinalApprovalRequest request, Utilisateur user) {
        try {
            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.REJECTED);
            dossierRepository.save(dossier);

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

    private Dossier getDossierForFinalRealizationApproval(Long id) {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        // Check if dossier is in Phase 8 (final realization approval)
        var currentStep = workflowService.getCurrentStep(dossier.getId());
        if (currentStep == null || !currentStep.getIdEtape().equals(8L)) {
            throw new RuntimeException("Le dossier n'est pas en phase d'approbation finale de la réalisation");
        }

        return dossier;
    }

    private boolean canGUCActOn(Dossier dossier) {
        return dossier.getStatus() == Dossier.DossierStatus.SUBMITTED ||
               dossier.getStatus() == Dossier.DossierStatus.IN_REVIEW ||
               dossier.getStatus() == Dossier.DossierStatus.AWAITING_FARMER ||
               dossier.getStatus() == Dossier.DossierStatus.REALIZATION_IN_PROGRESS;
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
        CommissionFeedbackDTO commissionFeedback = getCommissionFeedback(dossier.getId());
        ServiceTechniqueFeedbackDTO serviceTechniqueFeedback = getServiceTechniqueFeedback(dossier.getId());

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
                .notesCount(0)
                .availableActions(getActionsForCurrentState(dossier, dossier.getId()))
                // Commission feedback
                .commissionDecisionMade(commissionFeedback.getDecisionMade())
                .commissionApproved(commissionFeedback.getApproved())
                .commissionComments(commissionFeedback.getObservations())
                .commissionDecisionDate(commissionFeedback.getDateDecision())
                .commissionAgentName(commissionFeedback.getAgentCommissionName())
                // Service Technique feedback
                .serviceTechniqueDecisionMade(serviceTechniqueFeedback.getDecisionMade())
                .serviceTechniqueApproved(serviceTechniqueFeedback.getApproved())
                .serviceTechniqueComments(serviceTechniqueFeedback.getObservations())
                .serviceTechniqueDecisionDate(serviceTechniqueFeedback.getDateDecision())
                .serviceTechniqueAgentName(serviceTechniqueFeedback.getAgentServiceTechniqueName())
                .pourcentageAvancement(serviceTechniqueFeedback.getPourcentageAvancement())
                .build();
    }

    private DossierDetailResponse mapToDetailDTO(Dossier dossier) {
        TimingDTO timing = workflowService.getTimingInfo(dossier.getId());
        List<PieceJointe> pieceJointes = pieceJointeRepository.findByDossierIdOrderByDateUploadDesc(dossier.getId());
        CommissionFeedbackDTO commissionFeedback = getCommissionFeedback(dossier.getId());
        ServiceTechniqueFeedbackDTO serviceTechniqueFeedback = getServiceTechniqueFeedback(dossier.getId());

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
                .serviceTechniqueFeedback(serviceTechniqueFeedback)
                .build();
    }

    private CommissionFeedbackDTO getCommissionFeedback(Long dossierId) {
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
                    .photosUrls(List.of()) 
                    .build();
        }

        return CommissionFeedbackDTO.builder()
                .decisionMade(false)
                .approved(null)
                .build();
    }

    private ServiceTechniqueFeedbackDTO getServiceTechniqueFeedback(Long dossierId) {
        List<VisiteImplementation> visites = visiteImplementationRepository.findByDossierIdOrderByDateCreationDesc(dossierId);
        Optional<VisiteImplementation> latestVisit = visites.stream()
                .filter(v -> v.getDateCreation() != null)
                .findFirst();
        
        if (latestVisit.isPresent()) {
            VisiteImplementation visit = latestVisit.get();
            return ServiceTechniqueFeedbackDTO.builder()
                    .decisionMade(visit.getStatutVisite() == VisiteImplementation.StatutVisite.TERMINEE)                    .approved(visit.getApprouve())
                    .observations(visit.getObservations())
                    .recommandations(visit.getRecommandations())
                    .conclusion(visit.getConclusion())
                    .problemesDetectes(visit.getProblemes_detectes())
                    .actionsCorrectives(visit.getActions_correctives())
                    .dateVisite(visit.getDateVisite())
                    .dateConstat(visit.getDateConstat())
                    .dateDecision(visit.getDateModification())
                    .agentServiceTechniqueName(visit.getUtilisateurServiceTechnique() != null ? 
                            visit.getUtilisateurServiceTechnique().getPrenom() + " " + visit.getUtilisateurServiceTechnique().getNom() : null)
                    .coordonneesGPS(visit.getCoordonneesGPS())
                    .dureeVisite(visit.getDureeVisite())
                    .pourcentageAvancement(visit.getPourcentageAvancement())
                    .photosUrls(List.of()) // TODO: Get actual photos from PieceJointe with type TERRAIN_PHOTO
                    .statutVisite(visit.getStatutVisite() != null ? visit.getStatutVisite().name() : null)
                    .build();
        }

        return ServiceTechniqueFeedbackDTO.builder()
                .decisionMade(false)
                .approved(null)
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
            case 4 -> List.of( // Phase 4: Final approval - ONLY final approval action
                    ActionDTO.builder()
                            .action("final-approval")
                            .label("Finaliser l'Approbation")
                            .endpoint("/api/agent-guc/dossiers/" + dossierId + "/final-approval")
                            .method("GET") // Navigate to final approval page
                            .build()
            );
            case 6 -> List.of( // Phase 6: Realization GUC phase - Send to Service Technique
                    ActionDTO.builder()
                            .action("send-to-service-technique")
                            .label("Envoyer au Service Technique")
                            .endpoint("/api/agent-guc/dossiers/send-to-service-technique/" + dossierId)
                            .method("POST")
                            .build()
            );
            case 8 -> List.of( // Phase 8: Final realization validation
                    ActionDTO.builder()
                            .action("final-realization-approval")
                            .label("Approuver Réalisation Finale")
                            .endpoint("/api/agent-guc/dossiers/" + dossierId + "/final-realization-approval")
                            .method("GET") // Navigate to final realization approval page
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

    private String generateArchiveNumber(Dossier dossier) {
        return "ARCH-" + java.time.LocalDate.now().getYear() + "-" + 
               String.format("%02d", java.time.LocalDate.now().getMonthValue()) + "-" + 
               String.format("%06d", dossier.getId());
    }

    private String generateRejectionArchiveNumber(Dossier dossier) {
        return "ARCH-REJ-" + java.time.LocalDate.now().getYear() + "-" + 
               String.format("%02d", java.time.LocalDate.now().getMonthValue()) + "-" + 
               String.format("%06d", dossier.getId());
    }
}