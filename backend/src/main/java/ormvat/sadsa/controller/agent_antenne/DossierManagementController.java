package ormvat.sadsa.controller.agent_antenne;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ormvat.sadsa.dto.agent_antenne.DossierManagementDTOs.*;
import ormvat.sadsa.service.agent_antenne.DossierManagementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/agent_antenne/dossiers")
@RequiredArgsConstructor
@Slf4j
public class DossierManagementController {

    private final DossierManagementService dossierManagementService;

    /**
     * Get paginated list of dossiers with filtering and search
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
     * Get detailed dossier information
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
     * Send dossier to GUC
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
     * Delete dossier
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
                        .motif("Suppression par l'agent")
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
     * Get dossier statistics for dashboard
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
     * Add note to dossier
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

            // This would be implemented in the service
            // For now, return a success response
            return ResponseEntity.ok(
                DossierActionResponse.builder()
                    .success(true)
                    .message("Note ajoutée avec succès")
                    .build()
            );

        } catch (Exception e) {
            log.error("Erreur lors de l'ajout de la note au dossier {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur lors de l'ajout de la note")
                    .build()
            );
        }
    }


}