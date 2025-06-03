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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FicheApprobationService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditTrailRepository auditTrailRepository;
    private final WorkflowService workflowService;

    /**
     * Generate fiche d'approbation when GUC makes final approval
     */
    @Transactional
    public FicheApprobationResponse generateFicheApprobation(GenerateFicheRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Verify user is GUC agent and can approve
            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_GUC)) {
                throw new RuntimeException("Seul un agent GUC peut générer une fiche d'approbation");
            }

            // Check current workflow stage
            WorkflowService.EtapeInfo etapeInfo = workflowService.getCurrentEtapeInfo(dossier);
            if (!"AP - Phase GUC".equals(etapeInfo.getDesignation()) || etapeInfo.getOrdre() != 4) {
                throw new RuntimeException("Le dossier n'est pas à l'étape finale d'approbation");
            }

            // Update dossier with approval details
            dossier.setStatus(Dossier.DossierStatus.APPROVED);
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
     * Mark fiche as retrieved by farmer and send to Antenne
     */
    @Transactional
    public FarmerRetrievalResponse markFicheRetrieved(FarmerRetrievalRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Verify CIN matches
            if (!dossier.getAgriculteur().getCin().equals(request.getFarmerCin())) {
                throw new RuntimeException("CIN de l'agriculteur ne correspond pas");
            }

            // Verify dossier is approved and waiting for retrieval
            if (!dossier.getStatus().equals(Dossier.DossierStatus.APPROVED)) {
                throw new RuntimeException("Le dossier n'est pas approuvé");
            }

            // Update dossier status to indicate farmer has retrieved fiche
            dossier.setStatus(Dossier.DossierStatus.COMPLETED); // Temporary status
            dossierRepository.save(dossier);

            // Now send to Antenne WITHOUT setting workflow delays
            sendToAntenneWithoutDelay(dossier, utilisateur, request.getCommentaire());

            // Create audit trail
            createAuditTrail("FICHE_RETIREE_FERMIER", dossier, utilisateur, 
                "Fiche récupérée par: " + request.getRetrievedBy() + ". " + request.getCommentaire());

            return FarmerRetrievalResponse.builder()
                    .success(true)
                    .message("Fiche récupérée avec succès")
                    .dateRetrieval(request.getDateRetrieval())
                    .nextStep("Dossier envoyé à l'antenne pour phase réalisation")
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

            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Check if user can access this dossier for printing
            if (!canPrintFiche(dossier, utilisateur)) {
                throw new RuntimeException("Accès non autorisé pour l'impression de cette fiche");
            }

            // Check if dossier has been approved
            if (!dossier.getStatus().equals(Dossier.DossierStatus.APPROVED) && 
                !dossier.getStatus().equals(Dossier.DossierStatus.COMPLETED)) {
                throw new RuntimeException("Le dossier n'est pas approuvé");
            }

            String numeroFiche = generateFicheNumber(dossier);
            
            // Build fiche response for printing
            GenerateFicheRequest mockRequest = GenerateFicheRequest.builder()
                    .dossierId(dossierId)
                    .montantApprouve(dossier.getMontantSubvention())
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
     * Check if dossier is waiting for farmer retrieval
     */
    public boolean isWaitingFarmerRetrieval(Long dossierId) {
        try {
            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            return dossier.getStatus().equals(Dossier.DossierStatus.APPROVED) && 
                   dossier.getDateApprobation() != null;
        } catch (Exception e) {
            return false;
        }
    }

    // Private helper methods

    private String generateFicheNumber(Dossier dossier) {
        LocalDate today = LocalDate.now();
        String year = String.valueOf(today.getYear());
        String month = String.format("%02d", today.getMonthValue());
        
        // Format: FA-YYYY-MM-DOSSIER_ID
        return String.format("FA-%s-%s-%06d", year, month, dossier.getId());
    }

    private FicheApprobationResponse buildFicheResponse(Dossier dossier, GenerateFicheRequest request, 
                                                       String numeroFiche, Utilisateur utilisateur) {
        
        LocalDateTime now = LocalDateTime.now();
        LocalDate validiteDate = now.toLocalDate().plusMonths(6); // Valid for 6 months
        
        return FicheApprobationResponse.builder()
                .numeroFiche(numeroFiche)
                .dateApprobation(dossier.getDateApprobation().toLocalDate())
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
                .agriculteurProvince(dossier.getAgriculteur().getCommuneRurale() != null && 
                        dossier.getAgriculteur().getCommuneRurale().getCercle() != null &&
                        dossier.getAgriculteur().getCommuneRurale().getCercle().getProvince() != null ? 
                        dossier.getAgriculteur().getCommuneRurale().getCercle().getProvince().getDesignation() : "")
                
                // Administrative info
                .antenneDesignation(dossier.getAntenne().getDesignation())
                .cdaNom(dossier.getAntenne().getCda() != null ? 
                        dossier.getAntenne().getCda().getDescription() : "")
                
                // Approval details
                .statutApprobation("APPROUVÉ")
                .commentaireApprobation(request.getCommentaireApprobation())
                .observationsCommission(request.getObservationsCommission())
                .conditionsSpecifiques(request.getConditionsSpecifiques())
                .validiteJusquau(validiteDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                
                // Signature info
                .agentGucNom(utilisateur.getPrenom() + " " + utilisateur.getNom())
                .agentGucSignature("Signature électronique - " + now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .responsableNom("Responsable ORMVAT")
                .responsableSignature("Signature autorisée")
                
                // Workflow info
                .etapeActuelle("Phase d'approbation terminée")
                .prochainEtape("En attente de récupération par l'agriculteur")
                .farmersRetrieved(false)
                .dateRetrievalFermier(null)
                .build();
    }

    private void sendToAntenneWithoutDelay(Dossier dossier, Utilisateur utilisateur, String commentaire) {
        try {
            // Move to RP - Phase Antenne but without setting workflow delays
            // This is a special transition that puts the dossier "on hold" at antenne
            
            // We need to manually update the workflow to indicate it's at antenne but waiting
            // for farmer to bring the fiche
            
            // For now, we'll use a custom status to indicate this state
            dossier.setStatus(Dossier.DossierStatus.PENDING_CORRECTION); // Reusing existing status
            dossierRepository.save(dossier);
            
            log.info("Dossier {} envoyé à l'antenne sans délai - En attente fiche récupérée", dossier.getId());
            
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi à l'antenne sans délai", e);
            throw new RuntimeException("Erreur lors de l'envoi à l'antenne: " + e.getMessage());
        }
    }

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