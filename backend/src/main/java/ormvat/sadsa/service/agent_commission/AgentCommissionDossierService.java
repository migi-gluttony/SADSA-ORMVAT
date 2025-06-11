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
    private final VisiteTerrainRepository visiteTerrainRepository;
    private final WorkflowService workflowService;
    private final AuditService auditService;

    public DossierListResponse getDossiersForInspection(String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        
        // Get dossiers in commission phase (phase 3) or dossiers that have been processed by this commission agent
        List<Dossier> dossiers = dossierRepository.findByStatusIn(List.of(
                Dossier.DossierStatus.IN_REVIEW,
                Dossier.DossierStatus.APPROVED,
                Dossier.DossierStatus.REJECTED,
                Dossier.DossierStatus.COMPLETED
        )).stream()
                .filter(dossier -> isAccessibleByCommission(dossier, user))
                .collect(Collectors.toList());
        
        List<DossierSummaryDTO> summaries = dossiers.stream()
                .map(dossier -> mapToSummaryDTO(dossier, user))
                .collect(Collectors.toList());

        return DossierListResponse.builder()
                .dossiers(summaries)
                .build();
    }

    public DossierListResponse getMyAssignedDossiers(String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        
        // Get dossiers currently in commission phase (phase 3) that are assigned to this agent's team
        List<Dossier> dossiers = dossierRepository.findByStatus(Dossier.DossierStatus.IN_REVIEW)
                .stream()
                .filter(dossier -> isInCommissionPhase(dossier) && isAssignedToTeam(dossier, user))
                .collect(Collectors.toList());
        
        List<DossierSummaryDTO> summaries = dossiers.stream()
                .map(dossier -> mapToSummaryDTO(dossier, user))
                .collect(Collectors.toList());

        return DossierListResponse.builder()
                .dossiers(summaries)
                .build();
    }

    public DossierDetailResponse getDossierById(Long id, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        if (!isAccessibleByCommission(dossier, user)) {
            throw new RuntimeException("Dossier non accessible par la Commission");
        }

        return mapToDetailDTO(dossier, user);
    }

    @Transactional
    public ActionResponse approveTerrainInspection(Long id, TerrainApprovalRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForCommissionAction(id, user);

        // Find the active terrain visit for this dossier and agent
        VisiteTerrain visite = findActiveVisiteForDossier(dossier, user);
        if (visite == null) {
            throw new RuntimeException("Aucune visite terrain active trouvée pour ce dossier");
        }

        // Complete the visit with approval
        visite.setDateConstat(java.time.LocalDate.now());
        visite.setApprouve(true);
        visite.setObservations(request.getObservations());
        visite.setStatutVisite(VisiteTerrain.StatutVisite.TERMINEE);
        visiteTerrainRepository.save(visite);

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
        Dossier dossier = getDossierForCommissionAction(id, user);

        // Find the active terrain visit for this dossier and agent
        VisiteTerrain visite = findActiveVisiteForDossier(dossier, user);
        if (visite == null) {
            throw new RuntimeException("Aucune visite terrain active trouvée pour ce dossier");
        }

        // Complete the visit with rejection
        visite.setDateConstat(java.time.LocalDate.now());
        visite.setApprouve(false);
        visite.setObservations(request.getObservations());
        visite.setStatutVisite(VisiteTerrain.StatutVisite.TERMINEE);
        visiteTerrainRepository.save(visite);

        // Reject the dossier
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
        Dossier dossier = getDossierForCommissionAction(id, user);

        // Create new terrain visit
        VisiteTerrain visite = new VisiteTerrain();
        visite.setDossier(dossier);
        visite.setUtilisateurCommission(user);
        visite.setDateVisite(request.getDateVisite().toLocalDate());
        visite.setObservations(request.getCommentaire());
        visite.setStatutVisite(VisiteTerrain.StatutVisite.PROGRAMMEE);
        visiteTerrainRepository.save(visite);

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
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        if (!isAccessibleByCommission(dossier, user)) {
            return List.of();
        }

        return getActionsForCommissionPhase(dossierId, user);
    }

    // Helper methods
    private Utilisateur getUserByEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private Dossier getDossierForCommissionAction(Long id, Utilisateur user) {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        if (!isAccessibleByCommission(dossier, user)) {
            throw new RuntimeException("Action Commission non autorisée sur ce dossier");
        }

        return dossier;
    }

    private boolean isAccessibleByCommission(Dossier dossier, Utilisateur user) {
        // Check if dossier is in or has passed commission phase
        if (dossier.getStatus() != Dossier.DossierStatus.IN_REVIEW && 
            dossier.getStatus() != Dossier.DossierStatus.APPROVED &&
            dossier.getStatus() != Dossier.DossierStatus.REJECTED &&
            dossier.getStatus() != Dossier.DossierStatus.COMPLETED) {
            return false;
        }

        // Check team assignment if user has a specific team
        return isAssignedToTeam(dossier, user);
    }

    private boolean isInCommissionPhase(Dossier dossier) {
        if (dossier.getStatus() != Dossier.DossierStatus.IN_REVIEW) return false;
        
        // Check if current workflow step is Phase 3 (Commission)
        var currentStep = workflowService.getCurrentStep(dossier.getId());
        if (currentStep == null) return false;
        
        return currentStep.getIdEtape() == 3L;
    }

    private boolean isAssignedToTeam(Dossier dossier, Utilisateur user) {
        // If user has no specific team, can access all dossiers
        if (user.getEquipeCommission() == null) return true;

        // Check if dossier's project type matches user's team
        Long rubriqueId = dossier.getSousRubrique().getRubrique().getId();
        Utilisateur.EquipeCommission requiredTeam = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);
        
        return requiredTeam.equals(user.getEquipeCommission());
    }

    private VisiteTerrain findActiveVisiteForDossier(Dossier dossier, Utilisateur user) {
        return visiteTerrainRepository.findByDossierId(dossier.getId())
                .stream()
                .filter(v -> v.getUtilisateurCommission().getId().equals(user.getId()))
                .filter(v -> v.getDateConstat() == null)
                .findFirst()
                .orElse(null);
    }    private DossierSummaryDTO mapToSummaryDTO(Dossier dossier, Utilisateur user) {
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
                .availableActions(getActionsForCommissionPhase(dossier.getId(), user))
                .build();
    }

    private DossierDetailResponse mapToDetailDTO(Dossier dossier, Utilisateur user) {
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
                .availableActions(getActionsForCommissionPhase(dossier.getId(), user))
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

    private List<ActionDTO> getActionsForCommissionPhase(Long dossierId, Utilisateur user) {
        Dossier dossier = dossierRepository.findById(dossierId).orElse(null);
        if (dossier == null) return List.of();

        // Only show actions if user can access this dossier
        if (!isAccessibleByCommission(dossier, user)) return List.of();

        // Check if dossier is in commission phase and user's team matches
        if (isInCommissionPhase(dossier) && isAssignedToTeam(dossier, user)) {
            VisiteTerrain activeVisit = findActiveVisiteForDossier(dossier, user);
            
            if (activeVisit == null) {
                // No visit scheduled yet
                return List.of(
                        ActionDTO.builder()
                                .action("schedule-visit")
                                .label("Programmer Visite")
                                .endpoint("/api/agent-commission/dossiers/schedule-visit/" + dossierId)
                                .method("POST")
                                .build()
                );
            } else if (activeVisit.getDateConstat() == null) {
                // Visit scheduled but not completed
                return List.of(
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

        return List.of();
    }
}