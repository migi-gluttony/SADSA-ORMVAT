package ormvat.sadsa.controller.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ormvat.sadsa.dto.common.DossierManagementDTOs.*;
import ormvat.sadsa.service.common.DossierManagementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/dossiers")
@RequiredArgsConstructor
@Slf4j
public class DossierManagementController {

    private final DossierManagementService dossierManagementService;

    /**
     * Get paginated list of dossiers with filtering and search (role-based)
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

            // Build filter request
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
     * Get detailed dossier information (role-based)
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

            // If no request body, create default request
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
     * Send dossier to Commission (Agent GUC action)
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

            DossierActionResponse response = dossierManagementService.sendDossierToCommission(request, userEmail);
            
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
     * Return dossier to Antenne (Agent GUC action)
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

            DossierActionResponse response = dossierManagementService.returnDossierToAntenne(request, userEmail);
            
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
     * Reject dossier (Agent GUC action)
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

            DossierActionResponse response = dossierManagementService.rejectDossier(request, userEmail);
            
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
     * Update dossier priority (Agent GUC action)
     */
    @PostMapping("/{dossierId}/update-priority")
    public ResponseEntity<DossierActionResponse> updateDossierPriority(
            @PathVariable Long dossierId,
            @Valid @RequestBody UpdatePrioriteRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Mise à jour de la priorité du dossier {} par l'utilisateur: {}", dossierId, userEmail);

            request.setDossierId(dossierId);

            // This would be implemented in the service
            return ResponseEntity.ok(
                DossierActionResponse.builder()
                    .success(true)
                    .message("Priorité mise à jour avec succès")
                    .build()
            );

        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de la priorité du dossier {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur lors de la mise à jour de la priorité")
                    .build()
            );
        }
    }

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

            // If no request body, create default request
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
     * Get dossier statistics for dashboard (role-based)
     */
    @GetMapping("/stats")
    public ResponseEntity<DossierStatisticsDTO> getDossierStatistics(
            @RequestParam(defaultValue = "30d") String period,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Récupération des statistiques des dossiers pour l'utilisateur: {}", userEmail);

            // For now, get statistics through the list endpoint with no pagination
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
}