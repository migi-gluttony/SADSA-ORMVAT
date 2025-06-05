package ormvat.sadsa.service.agent_guc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ormvat.sadsa.dto.agent_guc.FicheApprobationDTOs.*;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;
import ormvat.sadsa.service.common.WorkflowService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class FicheApprobationService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditTrailRepository auditTrailRepository;
    private final WorkflowService workflowService;

    // ============================================================================
    // FICHE GENERATION - SIMPLIFIED WITH ID-BASED LOGIC
    // ============================================================================

    /**
     * Generate fiche d'approbation when GUC makes final approval
     */
    @Transactional
    public FicheApprobationResponse generateFicheApprobation(GenerateFicheRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = validateGUCUser(userEmail);
            Dossier dossier = getDossier(request.getDossierId());

            // Check current workflow stage - UPDATED to use ID-based logic
            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            if (etapeInfo.getEtapeId() != 4L) {
                throw new RuntimeException("Le dossier n'est pas à l'étape finale d'approbation (Phase 4). Etape actuelle: Phase " + etapeInfo.getEtapeId());
            }

            // Update dossier with approval details
            dossier.setStatus(Dossier.DossierStatus.APPROVED_AWAITING_FARMER);
            dossier.setDateApprobation(LocalDateTime.now());
            dossier.setMontantSubvention(request.getMontantApprouve());
            dossierRepository.save(dossier);

            // Generate fiche number
            String numeroFiche = generateFicheNumber(dossier);

            // Create audit trail
            createAuditTrail("GENERATION_FICHE_APPROBATION", dossier, utilisateur, 
                "Génération fiche d'approbation " + numeroFiche);

            // Build response with fiche details
            FicheApprobationResponse ficheResponse = buildFicheResponse(dossier, request, numeroFiche, utilisateur);

            log.info("Fiche d'approbation générée - Dossier: {}, Numéro fiche: {}", 
                    dossier.getId(), numeroFiche);

            return ficheResponse;

        } catch (Exception e) {
            log.error("Erreur lors de la génération de la fiche d'approbation", e);
            throw new RuntimeException("Erreur lors de la génération: " + e.getMessage());
        }
    }

    /**
     * Mark fiche as retrieved by farmer and prepare for realization
     */
    @Transactional
    public FarmerRetrievalResponse markFicheRetrieved(FarmerRetrievalRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = getDossier(request.getDossierId());

            // Verify CIN matches
            if (!dossier.getAgriculteur().getCin().equals(request.getFarmerCin())) {
                throw new RuntimeException("CIN de l'agriculteur ne correspond pas");
            }

            // Verify dossier is approved and waiting for retrieval
            if (!dossier.getStatus().equals(Dossier.DossierStatus.APPROVED_AWAITING_FARMER)) {
                throw new RuntimeException("Le dossier n'est pas en attente de récupération par l'agriculteur");
            }

            // Update dossier status to indicate farmer has retrieved fiche
            // This will be handled by the Agent Antenne when they initialize realization
            // For now, just mark it in audit trail

            // Create audit trail
            createAuditTrail("FICHE_RETIREE_FERMIER", dossier, utilisateur, 
                String.format("Fiche récupérée par: %s (CIN: %s). %s", 
                        request.getRetrievedBy(), request.getFarmerCin(), request.getCommentaire()));

            log.info("Fiche marquée comme récupérée - Dossier: {}, Par: {}", 
                    dossier.getId(), request.getRetrievedBy());

            return FarmerRetrievalResponse.builder()
                    .success(true)
                    .message("Fiche récupérée avec succès - Agriculteur doit se présenter à l'antenne pour initialiser la réalisation")
                    .dateRetrieval(request.getDateRetrieval())
                    .nextStep("L'agriculteur doit se présenter à l'antenne avec la fiche pour initialiser la réalisation")
                    .antenneDestination(dossier.getAntenne().getDesignation())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du marquage de récupération de la fiche", e);
            throw new RuntimeException("Erreur lors de la récupération: " + e.getMessage());
        }
    }

    /**
     * Get fiche details for printing
     */
    public FicheApprobationResponse getFicheForPrinting(Long dossierId, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = getDossier(dossierId);

            // Check if user can access this dossier for printing
            if (!canPrintFiche(dossier, utilisateur)) {
                throw new RuntimeException("Accès non autorisé pour l'impression de cette fiche");
            }

            // Check if dossier has been approved
            if (!dossier.getStatus().equals(Dossier.DossierStatus.APPROVED_AWAITING_FARMER) && 
                !dossier.getStatus().equals(Dossier.DossierStatus.REALIZATION_IN_PROGRESS) &&
                !dossier.getStatus().equals(Dossier.DossierStatus.COMPLETED)) {
                throw new RuntimeException("Le dossier n'est pas approuvé ou sa fiche n'est pas encore générée");
            }

            String numeroFiche = generateFicheNumber(dossier);
            
            // Build fiche response for printing
            GenerateFicheRequest mockRequest = GenerateFicheRequest.builder()
                    .dossierId(dossierId)
                    .montantApprouve(dossier.getMontantSubvention())
                    .commentaireApprobation("Fiche d'approbation pour impression")
                    .build();

            FicheApprobationResponse ficheResponse = buildFicheResponse(dossier, mockRequest, numeroFiche, utilisateur);

            // Create audit trail for printing
            createAuditTrail("IMPRESSION_FICHE_APPROBATION", dossier, utilisateur, 
                "Impression fiche d'approbation " + numeroFiche);

            return ficheResponse;

        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la fiche pour impression", e);
            throw new RuntimeException("Erreur lors de la récupération: " + e.getMessage());
        }
    }

    /**
     * Check if dossier is waiting for farmer retrieval - UPDATED for new workflow
     */
    public boolean isWaitingFarmerRetrieval(Long dossierId) {
        try {
            Dossier dossier = getDossier(dossierId);
            
            // A dossier is waiting for farmer retrieval if:
            // 1. Status is APPROVED_AWAITING_FARMER
            // 2. Has approval date
            return dossier.getStatus().equals(Dossier.DossierStatus.APPROVED_AWAITING_FARMER) && 
                   dossier.getDateApprobation() != null;
        } catch (Exception e) {
            log.warn("Error checking farmer retrieval status for dossier {}: {}", dossierId, e.getMessage());
            return false;
        }
    }

    // ============================================================================
    // HELPER METHODS - SIMPLIFIED
    // ============================================================================

    /**
     * Validate that user is GUC agent
     */
    private Utilisateur validateGUCUser(String userEmail) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_GUC)) {
            throw new RuntimeException("Seul un agent GUC peut effectuer cette action");
        }

        return utilisateur;
    }

    /**
     * Get dossier by ID
     */
    private Dossier getDossier(Long dossierId) {
        return dossierRepository.findById(dossierId)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));
    }

    /**
     * Generate fiche number
     */
    private String generateFicheNumber(Dossier dossier) {
        LocalDate today = LocalDate.now();
        String year = String.valueOf(today.getYear());
        String month = String.format("%02d", today.getMonthValue());
        
        // Format: FA-YYYY-MM-DOSSIER_ID
        return String.format("FA-%s-%s-%06d", year, month, dossier.getId());
    }

    /**
     * Build fiche response with all required information
     */
    private FicheApprobationResponse buildFicheResponse(Dossier dossier, GenerateFicheRequest request, 
                                                       String numeroFiche, Utilisateur utilisateur) {
        
        LocalDateTime now = LocalDateTime.now();
        LocalDate validiteDate = now.toLocalDate().plusMonths(6); // Valid for 6 months
        
        // Get current workflow info
        WorkflowService.EtapeInfo etapeInfo = null;
        try {
            etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
        } catch (Exception e) {
            log.warn("Cannot get etape info for fiche: {}", e.getMessage());
        }
        
        return FicheApprobationResponse.builder()
                .numeroFiche(numeroFiche)
                .dateApprobation(dossier.getDateApprobation() != null ? dossier.getDateApprobation().toLocalDate() : LocalDate.now())
                .dateGeneration(now)
                
                // Dossier info
                .numeroDossier(dossier.getNumeroDossier())
                .saba(dossier.getSaba())
                .reference(dossier.getReference())
                
                // Project info
                .rubriqueDesignation(dossier.getSousRubrique().getRubrique().getDesignation())
                .sousRubriqueDesignation(dossier.getSousRubrique().getDesignation())
                .montantDemande(dossier.getMontantSubvention()) // Original amount
                .montantApprouve(request.getMontantApprouve())
                
                // Farmer info
                .agriculteurNom(dossier.getAgriculteur().getNom())
                .agriculteurPrenom(dossier.getAgriculteur().getPrenom())
                .agriculteurCin(dossier.getAgriculteur().getCin())
                .agriculteurTelephone(dossier.getAgriculteur().getTelephone())
                .agriculteurCommune(dossier.getAgriculteur().getCommuneRurale() != null ? 
                        dossier.getAgriculteur().getCommuneRurale().getDesignation() : "")
                .agriculteurDouar(dossier.getAgriculteur().getDouar() != null ? 
                        dossier.getAgriculteur().getDouar().getDesignation() : "")
                .agriculteurProvince(getAgriculteurProvince(dossier))
                
                // Administrative info
                .antenneDesignation(dossier.getAntenne().getDesignation())
                .cdaNom(dossier.getAntenne().getCda() != null ? 
                        dossier.getAntenne().getCda().getDescription() : "")
                
                // Approval details
                .statutApprobation("APPROUVÉ")
                .commentaireApprobation(request.getCommentaireApprobation() != null ? request.getCommentaireApprobation() : "")
                .observationsCommission(request.getObservationsCommission() != null ? request.getObservationsCommission() : "")
                .conditionsSpecifiques(request.getConditionsSpecifiques() != null ? request.getConditionsSpecifiques() : "")
                .validiteJusquau(validiteDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                
                // Signature info
                .agentGucNom(utilisateur.getPrenom() + " " + utilisateur.getNom())
                .agentGucSignature("Signature électronique - " + now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .responsableNom("Responsable ORMVAT")
                .responsableSignature("Signature autorisée")
                
                // Workflow info - UPDATED to use ID-based logic
                .etapeActuelle(etapeInfo != null ? workflowService.getPhaseDisplayName(etapeInfo.getEtapeId()) : "Phase d'approbation terminée")
                .prochainEtape("En attente de récupération par l'agriculteur puis initialisation réalisation")
                .farmersRetrieved(false) // This would be tracked separately in real implementation
                .dateRetrievalFermier(null)
                .build();
    }

    /**
     * Get agriculteur province safely
     */
    private String getAgriculteurProvince(Dossier dossier) {
        try {
            if (dossier.getAgriculteur().getCommuneRurale() != null && 
                dossier.getAgriculteur().getCommuneRurale().getCercle() != null &&
                dossier.getAgriculteur().getCommuneRurale().getCercle().getProvince() != null) {
                return dossier.getAgriculteur().getCommuneRurale().getCercle().getProvince().getDesignation();
            }
        } catch (Exception e) {
            log.warn("Error getting province for agriculteur: {}", e.getMessage());
        }
        return "";
    }

    /**
     * Check if user can print fiche
     */
    private boolean canPrintFiche(Dossier dossier, Utilisateur utilisateur) {
        switch (utilisateur.getRole()) {
            case ADMIN:
                return true;
            case AGENT_GUC:
                return true; // GUC can always print approved fiches
            case AGENT_ANTENNE:
                // Antenne can print if it's their dossier
                return utilisateur.getAntenne() != null && 
                       dossier.getAntenne().getId().equals(utilisateur.getAntenne().getId());
            default:
                return false;
        }
    }

    /**
     * Create audit trail
     */
    private void createAuditTrail(String action, Dossier dossier, Utilisateur utilisateur, String description) {
        try {
            AuditTrail audit = new AuditTrail();
            audit.setAction(action);
            audit.setEntite("Dossier");
            audit.setEntiteId(dossier.getId());
            audit.setDateAction(LocalDateTime.now());
            audit.setUtilisateur(utilisateur);
            audit.setDescription(description);
            auditTrailRepository.save(audit);
        } catch (Exception e) {
            log.warn("Erreur lors de la création de l'audit trail", e);
        }
    }
}