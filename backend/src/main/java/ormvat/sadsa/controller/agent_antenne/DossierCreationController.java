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