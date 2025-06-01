package ormvat.sadsa.controller.agent_antenne;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ormvat.sadsa.dto.agent_antenne.DossierCreationDTOs.*;
import ormvat.sadsa.service.agent_antenne.DossierCreationService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/agent_antenne/dossiers")
@RequiredArgsConstructor
@Slf4j
public class DossierCreationController {

    private final DossierCreationService dossierService;

    /**
     * Get all initialization data needed for dossier creation
     */
    @GetMapping("/initialization-data")
    public ResponseEntity<InitializationDataResponse> getInitializationData(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Récupération des données d'initialisation pour l'utilisateur: {}", userEmail);
            
            InitializationDataResponse response = dossierService.getInitializationData(userEmail);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Erreur lors de la récupération des données d'initialisation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des données d'initialisation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if farmer exists by CIN
     */
    @GetMapping("/check-farmer/{cin}")
    public ResponseEntity<AgriculteurCheckResponse> checkFarmerExists(@PathVariable String cin) {
        try {
            log.info("Vérification de l'existence de l'agriculteur avec CIN: {}", cin);
            
            AgriculteurCheckResponse response = dossierService.checkFarmerExists(cin);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors de la vérification de l'agriculteur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search project types (sous-rubriques)
     */
    @GetMapping("/search-project-types")
    public ResponseEntity<List<SimplifiedSousRubriqueDTO>> searchProjectTypes(
            @RequestParam String searchTerm) {
        try {
            log.info("Recherche de types de projet avec terme: {}", searchTerm);
            
            List<SimplifiedSousRubriqueDTO> results = dossierService.searchProjectTypes(searchTerm);
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            log.error("Erreur lors de la recherche de types de projet", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new dossier
     */
    @PostMapping("/create")
    public ResponseEntity<?> createDossier(
            @Valid @RequestBody CreateDossierRequest request,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Création d'un nouveau dossier par l'utilisateur: {}, SABA: {}",
                    userEmail, request.getDossier().getSaba());

            CreateDossierResponse response = dossierService.createDossier(request, userEmail);

            log.info("Dossier créé avec succès - ID: {}, Numéro: {}",
                    response.getDossierId(), response.getNumeroDossier());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Données invalides pour la création du dossier: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Erreur métier lors de la création du dossier: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur technique lors de la création du dossier", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur technique lors de la création du dossier"));
        }
    }

    /**
     * Update an existing dossier (only if DRAFT or RETURNED_FOR_COMPLETION)
     */
    @PutMapping("/{dossierId}")
    public ResponseEntity<?> updateDossier(
            @PathVariable Long dossierId,
            @Valid @RequestBody UpdateDossierRequest request,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Mise à jour du dossier {} par l'utilisateur: {}", dossierId, userEmail);

            request.setDossierId(dossierId);
            UpdateDossierResponse response = dossierService.updateDossier(request, userEmail);

            log.info("Dossier {} mis à jour avec succès", dossierId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Données invalides pour la mise à jour du dossier: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Erreur métier lors de la mise à jour du dossier: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur technique lors de la mise à jour du dossier", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur technique lors de la mise à jour du dossier"));
        }
    }

    /**
     * Get dossier for editing
     */
    @GetMapping("/{dossierId}/edit")
    public ResponseEntity<DossierEditResponse> getDossierForEdit(
            @PathVariable Long dossierId,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Récupération du dossier {} pour édition par l'utilisateur: {}", dossierId, userEmail);
            
            DossierEditResponse response = dossierService.getDossierForEdit(dossierId, userEmail);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Erreur lors de la récupération du dossier pour édition: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            log.error("Erreur technique lors de la récupération du dossier pour édition", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Get cercles by province
     */
    @GetMapping("/cercles/{provinceId}")
    public ResponseEntity<List<GeographicDTO>> getCercles(@PathVariable Long provinceId) {
        try {
            List<GeographicDTO> cercles = dossierService.getCerclesByProvince(provinceId);
            return ResponseEntity.ok(cercles);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des cercles pour la province: {}", provinceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get communes by cercle
     */
    @GetMapping("/communes/{cercleId}")
    public ResponseEntity<List<GeographicDTO>> getCommunes(@PathVariable Long cercleId) {
        try {
            List<GeographicDTO> communes = dossierService.getCommunesByCircle(cercleId);
            return ResponseEntity.ok(communes);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des communes pour le cercle: {}", cercleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get douars by commune
     */
    @GetMapping("/douars/{communeId}")
    public ResponseEntity<List<GeographicDTO>> getDouars(@PathVariable Long communeId) {
        try {
            List<GeographicDTO> douars = dossierService.getDouarsByCommune(communeId);
            return ResponseEntity.ok(douars);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des douars pour la commune: {}", communeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint for the dossier creation service
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Dossier Creation Service is running");
    }

    // Helper method to create consistent error responses
    private Object createErrorResponse(String message) {
        return new ErrorResponse(false, message);
    }

    // Inner class for error responses
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ErrorResponse {
        private boolean success;
        private String message;
    }
}