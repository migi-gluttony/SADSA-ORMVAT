package ormvat.sadsa.controller.agent_guc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import ormvat.sadsa.dto.agent_guc.FicheApprobationDTOs.*;
import ormvat.sadsa.dto.role_based.RoleBasedDossierDTOs.*;
import ormvat.sadsa.service.agent_guc.FicheApprobationService;
import ormvat.sadsa.service.agent_guc.AgentGUCDossierService;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/fiche-approbation")
@RequiredArgsConstructor
@Slf4j
public class FicheApprobationController {    private final FicheApprobationService ficheApprobationService;
    private final AgentGUCDossierService agentGUCDossierService;    /**
     * Make final approval decision (Phase 4) - UPDATED to use AgentGUCDossierService methods
     */
    @PostMapping("/final-approval")
    public ResponseEntity<FinalApprovalResponse> makeFinalApprovalDecision(
            @Valid @RequestBody FinalApprovalRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Décision finale d'approbation pour dossier {} par {}", request.getDossierId(), userEmail);

            FinalApprovalResponse response;

            if (request.getApproved()) {
                // Use approveDossier for approval case
                ApproveRequest approveRequest = ApproveRequest.builder()
                    .commentaire(request.getCommentaireApprobation() != null ? 
                        request.getCommentaireApprobation() : "Approbation finale")
                    .build();
                
                ActionResponse actionResponse = agentGUCDossierService.approveDossier(
                    request.getDossierId(), approveRequest, userEmail);
                
                // Build fiche-specific response
                response = FinalApprovalResponse.builder()
                        .success(actionResponse.getSuccess())
                        .message(actionResponse.getMessage())
                        .newStatut("APPROVED_AWAITING_FARMER")
                        .dateAction(actionResponse.getTimestamp())
                        .ficheGenerated(true)
                        .nextStep("En attente de récupération par l'agriculteur")
                        .build();
            } else {
                // Use rejectDossier for rejection case
                RejectRequest rejectRequest = RejectRequest.builder()
                        .motif(request.getMotifRejet() != null ? request.getMotifRejet() : "Rejet final")
                        .commentaire("Rejet en phase finale d'approbation")
                        .build();
                
                ActionResponse actionResponse = agentGUCDossierService.rejectDossier(
                    request.getDossierId(), rejectRequest, userEmail);
                
                // Build rejection response
                response = FinalApprovalResponse.builder()
                        .success(actionResponse.getSuccess())
                        .message(actionResponse.getMessage())
                        .newStatut("REJECTED")
                        .dateAction(actionResponse.getTimestamp())
                        .ficheGenerated(false)
                        .nextStep("Dossier rejeté définitivement")
                        .build();
            }
            
            log.info("Décision finale prise avec succès pour dossier {}", request.getDossierId());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la décision finale pour dossier {}: {}", request.getDossierId(), e.getMessage());
            return ResponseEntity.badRequest().body(
                FinalApprovalResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de la décision finale pour dossier {}", request.getDossierId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                FinalApprovalResponse.builder()
                    .success(false)
                    .message("Erreur technique")
                    .build()
            );
        }
    }

    /**
     * Generate fiche d'approbation (integrated with Phase 4 approval)
     */
    @PostMapping("/generate")
    public ResponseEntity<FicheApprobationResponse> generateFiche(
            @Valid @RequestBody GenerateFicheRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Génération fiche d'approbation pour dossier {} par {}", request.getDossierId(), userEmail);

            FicheApprobationResponse response = ficheApprobationService.generateFicheApprobation(request, userEmail);
            
            log.info("Fiche d'approbation générée avec succès - Numéro: {}", response.getNumeroFiche());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la génération de fiche pour dossier {}: {}", request.getDossierId(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur technique lors de la génération de fiche pour dossier {}", request.getDossierId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Mark fiche as retrieved by farmer (transitions from halt state to realization)
     */
    @PostMapping("/mark-retrieved")
    public ResponseEntity<FarmerRetrievalResponse> markFicheRetrieved(
            @Valid @RequestBody FarmerRetrievalRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Marquage récupération fiche pour dossier {} par {}", request.getDossierId(), userEmail);

            FarmerRetrievalResponse response = ficheApprobationService.markFicheRetrieved(request, userEmail);
            
            log.info("Fiche marquée comme récupérée pour dossier {}", request.getDossierId());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors du marquage de récupération pour dossier {}: {}", request.getDossierId(), e.getMessage());
            return ResponseEntity.badRequest().body(
                FarmerRetrievalResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors du marquage de récupération pour dossier {}", request.getDossierId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                FarmerRetrievalResponse.builder()
                    .success(false)
                    .message("Erreur technique")
                    .build()
            );
        }
    }

    /**
     * Get fiche for printing (for Agent GUC and Agent Antenne)
     */
    @GetMapping("/{dossierId}/print")
    public ResponseEntity<FicheApprobationResponse> getFicheForPrinting(
            @PathVariable Long dossierId,
            @RequestParam(defaultValue = "PDF") String format,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Récupération fiche pour impression - Dossier: {}, Format: {}, User: {}", dossierId, format, userEmail);

            FicheApprobationResponse response = ficheApprobationService.getFicheForPrinting(dossierId, userEmail);
            
            log.info("Fiche récupérée pour impression - Numéro: {}", response.getNumeroFiche());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la récupération pour impression dossier {}: {}", dossierId, e.getMessage());
            
            if (e.getMessage().contains("non trouvé")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("non autorisé")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            log.error("Erreur technique lors de la récupération pour impression dossier {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate PDF fiche for download (for Agent GUC and Agent Antenne)
     */
    @GetMapping("/{dossierId}/download-pdf")
    public ResponseEntity<byte[]> downloadFichePdf(
            @PathVariable Long dossierId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Téléchargement PDF fiche pour dossier {} par {}", dossierId, userEmail);

            // Get fiche data
            FicheApprobationResponse ficheData = ficheApprobationService.getFicheForPrinting(dossierId, userEmail);
            
            // For now, return a simple response - in production, you would generate actual PDF
            String ficheContent = generateFicheContent(ficheData);
            byte[] pdfBytes = ficheContent.getBytes();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                    "fiche_approbation_" + ficheData.getNumeroFiche() + ".pdf");
            headers.setContentLength(pdfBytes.length);

            log.info("PDF fiche généré pour téléchargement - Numéro: {}", ficheData.getNumeroFiche());
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            log.error("Erreur lors du téléchargement PDF pour dossier {}: {}", dossierId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur technique lors du téléchargement PDF pour dossier {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if dossier is waiting for farmer retrieval (halt state check)
     */
    @GetMapping("/{dossierId}/status")
    public ResponseEntity<Map<String, Object>> getFicheStatus(
            @PathVariable Long dossierId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Vérification statut fiche pour dossier {} par {}", dossierId, userEmail);

            boolean waitingRetrieval = ficheApprobationService.isWaitingFarmerRetrieval(dossierId);
            
            Map<String, Object> status = Map.of(
                    "dossierId", dossierId,
                    "waitingFarmerRetrieval", waitingRetrieval,
                    "canPrint", true,
                    "lastCheck", java.time.LocalDateTime.now(),
                    "inHaltState", waitingRetrieval,
                    "statusMessage", waitingRetrieval ? 
                        "En attente de récupération par l'agriculteur" : 
                        "Fiche non disponible ou déjà récupérée"
            );

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("Erreur lors de la vérification du statut pour dossier {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Helper method to generate fiche content (placeholder for PDF generation)
    private String generateFicheContent(FicheApprobationResponse fiche) {
        StringBuilder content = new StringBuilder();
        content.append("=== FICHE D'APPROBATION ===\n\n");
        content.append("Numéro: ").append(fiche.getNumeroFiche()).append("\n");
        content.append("Date d'approbation: ").append(fiche.getDateApprobation()).append("\n\n");
        
        content.append("=== INFORMATIONS AGRICULTEUR ===\n");
        content.append("Nom: ").append(fiche.getAgriculteurNom()).append("\n");
        content.append("Prénom: ").append(fiche.getAgriculteurPrenom()).append("\n");
        content.append("CIN: ").append(fiche.getAgriculteurCin()).append("\n");
        content.append("Téléphone: ").append(fiche.getAgriculteurTelephone()).append("\n\n");
        
        content.append("=== INFORMATIONS PROJET ===\n");
        content.append("Rubrique: ").append(fiche.getRubriqueDesignation()).append("\n");
        content.append("Sous-rubrique: ").append(fiche.getSousRubriqueDesignation()).append("\n");
        content.append("Montant approuvé: ").append(fiche.getMontantApprouve()).append(" DH\n\n");
        
        content.append("=== APPROBATION ===\n");
        content.append("Statut: ").append(fiche.getStatutApprobation()).append("\n");
        content.append("Commentaire: ").append(fiche.getCommentaireApprobation()).append("\n");
        content.append("Valable jusqu'au: ").append(fiche.getValiditeJusquau()).append("\n\n");
        
        content.append("=== SIGNATURE ===\n");
        content.append("Agent GUC: ").append(fiche.getAgentGucNom()).append("\n");
        content.append("Signature: ").append(fiche.getAgentGucSignature()).append("\n");
        
        return content.toString();
    }
}