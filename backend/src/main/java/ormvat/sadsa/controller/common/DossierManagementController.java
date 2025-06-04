package ormvat.sadsa.controller.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ormvat.sadsa.dto.common.DossierCommonDTOs.*;
import ormvat.sadsa.dto.agent_antenne.DossierAntenneActionDTOs.*;
import ormvat.sadsa.dto.agent_guc.DossierGUCActionDTOs.*;
import ormvat.sadsa.service.common.DossierManagementService;
import ormvat.sadsa.service.agent_guc.DossierGUCService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/dossiers")
@RequiredArgsConstructor
@Slf4j
public class DossierManagementController {

    private final DossierManagementService dossierManagementService;
    private final DossierGUCService dossierGUCService;

    /**
     * Get paginated list of dossiers with filtering and search (workflow-based)
     */
    @GetMapping
    public ResponseEntity<DossierListResponse> getDossiersList(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Long sousRubriqueId,
            @RequestParam(required = false) String emplacement,
            @RequestParam(required = false) String dateDebutCreation,
            @RequestParam(required = false) String dateFinCreation,
            @RequestParam(required = false) Boolean enRetard,
            @RequestParam(required = false) String priorite,
            @RequestParam(required = false) Long antenneId,
            @RequestParam(defaultValue = "dateCreation") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Récupération des dossiers pour l'utilisateur: {}, page: {}, size: {}", userEmail, page, size);

            DossierFilterRequest filterRequest = DossierFilterRequest.builder()
                    .searchTerm(searchTerm)
                    .statut(statut)
                    .sousRubriqueId(sousRubriqueId)
                    .enRetard(enRetard)
                    .priorite(priorite)
                    .antenneId(antenneId)
                    .sortBy(sortBy)
                    .sortDirection(sortDirection)
                    .page(page)
                    .size(size)
                    .build();

            DossierListResponse response = dossierManagementService.getDossiersList(filterRequest, userEmail);
            
            log.info("Dossiers récupérés avec succès - Total: {}, Page: {}", response.getTotalCount(), page);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la récupération des dossiers: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur technique lors de la récupération des dossiers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get detailed dossier information with workflow data
     */
    @GetMapping("/{dossierId}")
    public ResponseEntity<DossierDetailResponse> getDossierDetail(
            @PathVariable Long dossierId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Récupération du détail du dossier {} pour l'utilisateur: {}", dossierId, userEmail);

            DossierDetailResponse response = dossierManagementService.getDossierDetail(dossierId, userEmail);
            
            log.info("Détail du dossier {} récupéré avec succès", dossierId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la récupération du dossier {}: {}", dossierId, e.getMessage());
            
            if (e.getMessage().contains("non trouvé")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("non autorisé")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            log.error("Erreur technique lors de la récupération du dossier {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get workflow information for dossier
     */
    @GetMapping("/{dossierId}/workflow")
    public ResponseEntity<DossierManagementService.WorkflowInfoResponse> getWorkflowInfo(
            @PathVariable Long dossierId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Récupération des informations de workflow pour le dossier {} par {}", dossierId, userEmail);

            DossierManagementService.WorkflowInfoResponse response = dossierManagementService.getWorkflowInfo(dossierId, userEmail);
            
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la récupération du workflow {}: {}", dossierId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur technique lors de la récupération du workflow {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== AGENT ANTENNE ACTIONS =====

    /**
     * Send dossier to GUC (Agent Antenne action)
     */
    @PostMapping("/{dossierId}/send-to-guc")
    public ResponseEntity<DossierActionResponse> sendDossierToGUC(
            @PathVariable Long dossierId,
            @RequestBody(required = false) SendToGUCRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Envoi du dossier {} au GUC par l'utilisateur: {}", dossierId, userEmail);

            if (request == null) {
                request = SendToGUCRequest.builder()
                        .dossierId(dossierId)
                        .commentaire("Envoi au GUC")
                        .build();
            } else {
                request.setDossierId(dossierId);
            }

            DossierActionResponse response = dossierManagementService.sendDossierToGUC(request, userEmail);
            
            log.info("Dossier {} envoyé au GUC avec succès", dossierId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de l'envoi du dossier {} au GUC: {}", dossierId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DossierActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de l'envoi du dossier {} au GUC", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur technique lors de l'envoi")
                    .build()
            );
        }
    }

    /**
     * LEGACY: Start realization phase (Agent Antenne action) - Kept for compatibility
     */
    @PostMapping("/{dossierId}/start-realization")
    public ResponseEntity<DossierActionResponse> startRealizationPhase(
            @PathVariable Long dossierId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Démarrage de la réalisation pour le dossier {} par {}", dossierId, userEmail);

            DossierActionResponse response = dossierManagementService.startRealizationPhase(dossierId, userEmail);
            
            log.info("Phase de réalisation démarrée pour le dossier {}", dossierId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors du démarrage de la réalisation du dossier {}: {}", dossierId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DossierActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors du démarrage de la réalisation du dossier {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur technique")
                    .build()
            );
        }
    }

    /**
     * NEW: Initialize realization phase (Agent Antenne action) - Clearer endpoint
     */
    @PostMapping("/{dossierId}/initialize-realization")
    public ResponseEntity<DossierActionResponse> initializeRealizationPhase(
            @PathVariable Long dossierId,
            @RequestParam(required = false) String ficheApprobationReference,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Initialisation de la réalisation pour le dossier {} par {} avec référence fiche: {}", 
                    dossierId, userEmail, ficheApprobationReference);

            // TODO: Add fiche d'approbation validation if needed
            // validateFicheApprobation(dossierId, ficheApprobationReference);

            DossierActionResponse response = dossierManagementService.initializeRealizationPhase(dossierId, userEmail);
            
            log.info("Phase de réalisation initialisée pour le dossier {}", dossierId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de l'initialisation de la réalisation du dossier {}: {}", dossierId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DossierActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de l'initialisation de la réalisation du dossier {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur technique")
                    .build()
            );
        }
    }

    // ===== AGENT GUC ACTIONS - UPDATED to use DossierGUCService =====

    /**
     * Send dossier to Commission (Agent GUC action) - Phase 2 → 3
     */
    @PostMapping("/{dossierId}/send-to-commission")
    public ResponseEntity<DossierActionResponse> sendDossierToCommission(
            @PathVariable Long dossierId,
            @Valid @RequestBody SendToCommissionRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Envoi du dossier {} à la commission par l'utilisateur: {}", dossierId, userEmail);

            request.setDossierId(dossierId);

            // Use updated GUC service
            DossierActionResponse response = dossierGUCService.sendDossierToCommission(request, userEmail);
            
            log.info("Dossier {} envoyé à la commission avec succès", dossierId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de l'envoi du dossier {} à la commission: {}", dossierId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DossierActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de l'envoi du dossier {} à la commission", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur technique lors de l'envoi")
                    .build()
            );
        }
    }

    /**
     * Return dossier to Antenne (Agent GUC action) - Phase 2/4 → 1
     */
    @PostMapping("/{dossierId}/return-to-antenne")
    public ResponseEntity<DossierActionResponse> returnDossierToAntenne(
            @PathVariable Long dossierId,
            @Valid @RequestBody ReturnToAntenneRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Retour du dossier {} à l'antenne par l'utilisateur: {}", dossierId, userEmail);

            request.setDossierId(dossierId);

            // Use updated GUC service
            DossierActionResponse response = dossierGUCService.returnDossierToAntenne(request, userEmail);
            
            log.info("Dossier {} retourné à l'antenne avec succès", dossierId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors du retour du dossier {} à l'antenne: {}", dossierId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DossierActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors du retour du dossier {} à l'antenne", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur technique lors du retour")
                    .build()
            );
        }
    }

    /**
     * Reject dossier (Agent GUC action) - Phase 2/4
     */
    @PostMapping("/{dossierId}/reject")
    public ResponseEntity<DossierActionResponse> rejectDossier(
            @PathVariable Long dossierId,
            @Valid @RequestBody RejectDossierRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Rejet du dossier {} par l'utilisateur: {}", dossierId, userEmail);

            request.setDossierId(dossierId);

            // Use updated GUC service
            DossierActionResponse response = dossierGUCService.rejectDossier(request, userEmail);
            
            log.info("Dossier {} rejeté avec succès", dossierId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors du rejet du dossier {}: {}", dossierId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DossierActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors du rejet du dossier {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur technique lors du rejet")
                    .build()
            );
        }
    }

    /**
     * Approve dossier (Agent GUC action) - Phase 4 Legacy endpoint
     */
    @PostMapping("/{dossierId}/approve")
    public ResponseEntity<DossierActionResponse> approveDossier(
            @PathVariable Long dossierId,
            @RequestParam(required = false, defaultValue = "") String commentaire,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Approbation du dossier {} par l'utilisateur: {}", dossierId, userEmail);

            // Use updated GUC service (legacy method)
            DossierActionResponse response = dossierGUCService.approveDossier(dossierId, userEmail, commentaire);
            
            log.info("Dossier {} approuvé avec succès", dossierId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de l'approbation du dossier {}: {}", dossierId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DossierActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de l'approbation du dossier {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur technique")
                    .build()
            );
        }
    }

    /**
     * NEW: Process realization review (Agent GUC action) - Phase 6 → 7
     */
    @PostMapping("/{dossierId}/process-realization-review")
    public ResponseEntity<DossierActionResponse> processRealizationReview(
            @PathVariable Long dossierId,
            @RequestParam(required = false, defaultValue = "") String commentaire,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Révision réalisation du dossier {} par l'utilisateur: {}", dossierId, userEmail);

            DossierActionResponse response = dossierGUCService.processRealizationReview(dossierId, userEmail, commentaire);
            
            log.info("Révision réalisation du dossier {} terminée avec succès", dossierId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la révision réalisation du dossier {}: {}", dossierId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DossierActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de la révision réalisation du dossier {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur technique")
                    .build()
            );
        }
    }

    /**
     * NEW: Finalize realization (Agent GUC action) - Phase 8
     */
    @PostMapping("/{dossierId}/finalize-realization")
    public ResponseEntity<DossierActionResponse> finalizeRealization(
            @PathVariable Long dossierId,
            @RequestParam(required = false, defaultValue = "") String commentaire,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Finalisation réalisation du dossier {} par l'utilisateur: {}", dossierId, userEmail);

            DossierActionResponse response = dossierGUCService.finalizeRealization(dossierId, userEmail, commentaire);
            
            log.info("Finalisation réalisation du dossier {} terminée avec succès", dossierId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la finalisation réalisation du dossier {}: {}", dossierId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DossierActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de la finalisation réalisation du dossier {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur technique")
                    .build()
            );
        }
    }

    // ===== COMMISSION ACTIONS =====

    /**
     * Approve terrain (Commission action)
     */
    @PostMapping("/{dossierId}/approve-terrain")
    public ResponseEntity<DossierActionResponse> approveTerrain(
            @PathVariable Long dossierId,
            @RequestParam(required = false, defaultValue = "") String commentaire,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Approbation du terrain pour le dossier {} par {}", dossierId, userEmail);

            DossierActionResponse response = dossierManagementService.approveTerrain(dossierId, userEmail, commentaire);
            
            log.info("Terrain approuvé pour le dossier {}", dossierId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de l'approbation du terrain {}: {}", dossierId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DossierActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de l'approbation du terrain {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur technique")
                    .build()
            );
        }
    }

    /**
     * Reject terrain (Commission action)
     */
    @PostMapping("/{dossierId}/reject-terrain")
    public ResponseEntity<DossierActionResponse> rejectTerrain(
            @PathVariable Long dossierId,
            @RequestParam String motif,
            @RequestParam(required = false, defaultValue = "") String commentaire,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Rejet du terrain pour le dossier {} par {}", dossierId, userEmail);

            DossierActionResponse response = dossierManagementService.rejectTerrain(dossierId, userEmail, motif, commentaire);
            
            log.info("Terrain rejeté pour le dossier {}", dossierId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors du rejet du terrain {}: {}", dossierId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DossierActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors du rejet du terrain {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur technique")
                    .build()
            );
        }
    }

    /**
     * Return to GUC from Commission
     */
    @PostMapping("/{dossierId}/return-to-guc")
    public ResponseEntity<DossierActionResponse> returnToGUCFromCommission(
            @PathVariable Long dossierId,
            @RequestParam String motif,
            @RequestParam(required = false, defaultValue = "") String commentaire,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Retour au GUC depuis commission pour le dossier {} par {}", dossierId, userEmail);

            DossierActionResponse response = dossierManagementService.returnToGUCFromCommission(dossierId, userEmail, motif, commentaire);
            
            log.info("Dossier {} retourné au GUC depuis commission", dossierId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors du retour au GUC {}: {}", dossierId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DossierActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors du retour au GUC {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur technique")
                    .build()
            );
        }
    }

    // ===== COMMON ACTIONS =====

    /**
     * Delete dossier (role-based)
     */
    @DeleteMapping("/{dossierId}")
    public ResponseEntity<DossierActionResponse> deleteDossier(
            @PathVariable Long dossierId,
            @RequestBody(required = false) DeleteDossierRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Suppression du dossier {} par l'utilisateur: {}", dossierId, userEmail);

            if (request == null) {
                request = DeleteDossierRequest.builder()
                        .dossierId(dossierId)
                        .motif("Suppression par l'utilisateur")
                        .build();
            } else {
                request.setDossierId(dossierId);
            }

            DossierActionResponse response = dossierManagementService.deleteDossier(request, userEmail);
            
            log.info("Dossier {} supprimé avec succès", dossierId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la suppression du dossier {}: {}", dossierId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DossierActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de la suppression du dossier {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur technique lors de la suppression")
                    .build()
            );
        }
    }

    /**
     * Add note to dossier (all roles)
     */
    @PostMapping("/{dossierId}/notes")
    public ResponseEntity<DossierActionResponse> addNote(
            @PathVariable Long dossierId,
            @Valid @RequestBody AddNoteRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Ajout d'une note au dossier {} par l'utilisateur: {}", dossierId, userEmail);

            request.setDossierId(dossierId);

            DossierActionResponse response = dossierManagementService.addNote(request, userEmail);
            
            log.info("Note ajoutée avec succès au dossier {}", dossierId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de l'ajout de la note au dossier {}: {}", dossierId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DossierActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de l'ajout de la note au dossier {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur lors de l'ajout de la note")
                    .build()
            );
        }
    }

    /**
     * Process generic workflow action
     */
    @PostMapping("/{dossierId}/workflow/{action}")
    public ResponseEntity<DossierActionResponse> processWorkflowAction(
            @PathVariable Long dossierId,
            @PathVariable String action,
            @RequestBody(required = false) Map<String, Object> parameters,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Action workflow {} sur le dossier {} par {}", action, dossierId, userEmail);

            if (parameters == null) {
                parameters = Map.of();
            }

            DossierActionResponse response = dossierManagementService.processWorkflowAction(dossierId, action, parameters, userEmail);
            
            log.info("Action workflow {} exécutée avec succès sur le dossier {}", action, dossierId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de l'action workflow {} sur dossier {}: {}", action, dossierId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DossierActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de l'action workflow {} sur dossier {}", action, dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur technique")
                    .build()
            );
        }
    }

    // ===== UTILITY ENDPOINTS =====

    /**
     * Get all notes for a dossier (all roles)
     */
    @GetMapping("/{dossierId}/notes")
    public ResponseEntity<List<NoteDTO>> getDossierNotes(
            @PathVariable Long dossierId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Récupération des notes du dossier {} pour l'utilisateur: {}", dossierId, userEmail);

            DossierDetailResponse dossierDetail = dossierManagementService.getDossierDetail(dossierId, userEmail);
            
            return ResponseEntity.ok(dossierDetail.getNotes());

        } catch (RuntimeException e) {
            log.error("Erreur lors de la récupération des notes du dossier {}: {}", dossierId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur technique lors de la récupération des notes du dossier {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get dossier statistics for dashboard (workflow-based)
     */
    @GetMapping("/stats")
    public ResponseEntity<DossierStatisticsDTO> getDossierStatistics(
            @RequestParam(defaultValue = "30d") String period,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Récupération des statistiques des dossiers pour l'utilisateur: {}", userEmail);

            DossierFilterRequest filterRequest = DossierFilterRequest.builder()
                    .page(0)
                    .size(Integer.MAX_VALUE)
                    .build();

            DossierListResponse response = dossierManagementService.getDossiersList(filterRequest, userEmail);
            
            return ResponseEntity.ok(response.getStatistics());

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get available actions for current user and specific dossier
     */
    @GetMapping("/{dossierId}/actions")
    public ResponseEntity<List<AvailableActionDTO>> getAvailableActions(
            @PathVariable Long dossierId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Récupération des actions disponibles pour le dossier {} et l'utilisateur: {}", dossierId, userEmail);

            DossierDetailResponse response = dossierManagementService.getDossierDetail(dossierId, userEmail);
            
            return ResponseEntity.ok(response.getAvailableActions());

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des actions disponibles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Export dossiers to CSV (uses existing implementation)
     */
    @GetMapping("/export/csv")
    public ResponseEntity<Resource> exportDossiersToCsv(
            @RequestParam(defaultValue = "all") String exportType,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Long sousRubriqueId,
            @RequestParam(required = false) Long antenneId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Export CSV des dossiers pour l'utilisateur: {}, type: {}", userEmail, exportType);

            // TODO: Use existing CSV export service implementation
            throw new RuntimeException("Cette fonctionnalité utilise l'implémentation d'export existante");
            
        } catch (RuntimeException e) {
            log.error("Erreur lors de l'export CSV: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur technique lors de l'export CSV", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}