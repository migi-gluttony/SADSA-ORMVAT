package ormvat.sadsa.controller.agent_antenne;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ormvat.sadsa.dto.agent_antenne.DossierCreationDTOs.*;
import ormvat.sadsa.service.agent_antenne.DossierCreationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/agent_antenne/dossiers")
@RequiredArgsConstructor
@Slf4j
public class DossierCreationController {

    private final DossierCreationService dossierCreationService;

    /**
     * Récupère toutes les rubriques avec leurs sous-rubriques et documents requis
     */
    @GetMapping("/rubriques")
    public ResponseEntity<RubriquesResponse> getRubriques() {
        try {
            log.info("Récupération des rubriques et sous-rubriques");
            RubriquesResponse response = dossierCreationService.getRubriquesWithDocuments();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des rubriques", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère la liste des CDAs disponibles
     */
    @GetMapping("/cdas")
    public ResponseEntity<List<CDAInfoDTO>> getAvailableCDAs() {
        try {
            log.info("Récupération des CDAs disponibles");
            List<CDAInfoDTO> cdas = dossierCreationService.getAvailableCDAs();
            return ResponseEntity.ok(cdas);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des CDAs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Valide les données d'un dossier avant soumission
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateDossier(@Valid @RequestBody CreateDossierRequest request) {
        try {
            log.info("Validation du dossier SABA: {}", request.getDossier().getSaba());

            DossierValidationDTO validation = dossierCreationService.validateDossier(request);

            if (validation.isValid()) {
                return ResponseEntity.ok(validation);
            } else {
                return ResponseEntity.badRequest().body(
                        ValidationErrorResponse.builder()
                                .fieldErrors(validation.getSuggestions())
                                .globalErrors(validation.getMissingFields())
                                .message("Données invalides")
                                .build());
            }
        } catch (Exception e) {
            log.error("Erreur lors de la validation du dossier", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la validation: " + e.getMessage());
        }
    }

    /**
     * Génère un aperçu/résumé du dossier avant soumission finale
     */
    @PostMapping("/preview")
    public ResponseEntity<DossierSummaryDTO> previewDossier(
            @Valid @RequestBody CreateDossierRequest request,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Génération de l'aperçu du dossier SABA: {}", request.getDossier().getSaba());

            DossierSummaryDTO summary = dossierCreationService.generateDossierSummary(request, userEmail);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Erreur lors de la génération de l'aperçu du dossier", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Crée un nouveau dossier
     */
    @PostMapping("/create")
    public ResponseEntity<?> createDossier(
            @Valid @RequestBody CreateDossierRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            String userEmail = authentication.getName();
            log.info("Création d'un nouveau dossier par l'utilisateur: {}, SABA: {}",
                    userEmail, request.getDossier().getSaba());

            // Enrichir la requête avec des métadonnées
            request.setUserAgent(httpRequest.getHeader("User-Agent"));
            request.setIpAddress(getClientIpAddress(httpRequest));

            // Validation préalable
            DossierValidationDTO validation = dossierCreationService.validateDossier(request);
            if (!validation.isValid()) {
                return ResponseEntity.badRequest().body(
                        ValidationErrorResponse.builder()
                                .fieldErrors(validation.getSuggestions())
                                .globalErrors(validation.getMissingFields())
                                .message("Impossible de créer le dossier - données invalides")
                                .build());
            }

            // Création du dossier
            CreateDossierResponse response = dossierCreationService.createDossier(request, userEmail);

            log.info("Dossier créé avec succès - ID: {}, Numéro: {}",
                    response.getDossierId(), response.getNumeroDossier());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Données invalides pour la création du dossier: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ValidationErrorResponse.builder()
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Erreur lors de la création du dossier", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la création du dossier: " + e.getMessage());
        }
    }

    /**
     * Sauvegarde un dossier en brouillon
     */
    @PostMapping("/save-draft")
    public ResponseEntity<?> saveDraft(
            @Valid @RequestBody CreateDossierRequest request,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Sauvegarde en brouillon par l'utilisateur: {}", userEmail);

            // TODO: Implémenter la sauvegarde en brouillon
            // Pour l'instant, on retourne une réponse simple

            return ResponseEntity.ok().body(
                    CreateDossierResponse.builder()
                            .message("Brouillon sauvegardé avec succès")
                            .statut(StatutDossier.BROUILLON.name())
                            .build());
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du brouillon", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    /**
     * Génère un récépissé pour un dossier existant
     */
    @GetMapping("/{dossierId}/recepisse")
    public ResponseEntity<RecepisseDossierDTO> generateRecepisse(
            @PathVariable Long dossierId,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Génération de récépissé pour le dossier: {} par l'utilisateur: {}",
                    dossierId, userEmail);

            // TODO: Implémenter la génération de récépissé pour dossier existant
            // Pour l'instant, on retourne une erreur

            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            log.error("Erreur lors de la génération du récépissé", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les statistiques de création de dossiers pour l'agent
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getDossierCreationStats(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Récupération des statistiques pour l'utilisateur: {}", userEmail);

            // TODO: Implémenter les statistiques
            // Pour l'instant, on retourne des données fictives

            return ResponseEntity.ok().body(
                    "Statistiques de création de dossiers - À implémenter");
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Recherche des agriculteurs existants par CIN
     */
    @GetMapping("/agriculteurs/search")
    public ResponseEntity<?> searchAgriculteur(@RequestParam String cin) {
        try {
            log.info("Recherche agriculteur par CIN: {}", cin);

            // TODO: Implémenter la recherche d'agriculteur
            // Pour l'instant, on retourne une réponse vide

            return ResponseEntity.ok().body("Recherche agriculteur - À implémenter");
        } catch (Exception e) {
            log.error("Erreur lors de la recherche de l'agriculteur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Vérifie l'unicité d'un numéro SABA
     */
    @GetMapping("/check-saba")
    public ResponseEntity<?> checkSabaUniqueness(@RequestParam String saba) {
        try {
            log.info("Vérification unicité SABA: {}", saba);

            // Simple validation via le service
            CreateDossierRequest tempRequest = CreateDossierRequest.builder()
                    .dossier(DossierInfoDTO.builder().saba(saba).build())
                    .agriculteur(AgriculteurInfoDTO.builder().cin("temp").nom("temp").prenom("temp").build())
                    .build();

            DossierValidationDTO validation = dossierCreationService.validateDossier(tempRequest);

            boolean isUnique = !validation.getInvalidFields().contains("Numéro SABA déjà utilisé");

            return ResponseEntity.ok().body(
                    Map.of(
                            "isUnique", isUnique,
                            "message", isUnique ? "SABA disponible" : "SABA déjà utilisé"));
        } catch (Exception e) {
            log.error("Erreur lors de la vérification du SABA", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Méthodes utilitaires

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null || xForwardedForHeader.isEmpty()) {
            return request.getRemoteAddr();
        } else {
            return xForwardedForHeader.split(",")[0].trim();
        }
    }

    // Classe interne pour les réponses simples
    private static class Map {
        public static java.util.Map<String, Object> of(String key1, Object value1, String key2, Object value2) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put(key1, value1);
            map.put(key2, value2);
            return map;
        }
    }

    @GetMapping("/generate-saba")
    public ResponseEntity<java.util.Map<String, Object>> generateSaba() {
        try {
            String saba = dossierCreationService.generateSabaNumber();
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("saba", saba);
            response.put("generated", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user-cda")
    public ResponseEntity<CDAInfoDTO> getUserCDA(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            CDAInfoDTO userCDA = dossierCreationService.getUserCDA(userEmail);
            return ResponseEntity.ok(userCDA);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du CDA utilisateur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/provinces")
    public ResponseEntity<List<GeographicDTO>> getProvinces() {
        return ResponseEntity.ok(dossierCreationService.getProvinces());
    }

    @GetMapping("/cercles/{provinceId}")
    public ResponseEntity<List<GeographicDTO>> getCercles(@PathVariable Long provinceId) {
        return ResponseEntity.ok(dossierCreationService.getCerclesByProvince(provinceId));
    }

    @GetMapping("/communes/{cercleId}")
    public ResponseEntity<List<GeographicDTO>> getCommunes(@PathVariable Long cercleId) {
        return ResponseEntity.ok(dossierCreationService.getCommunesByCircle(cercleId));
    }

    @GetMapping("/douars/{communeId}")
    public ResponseEntity<List<GeographicDTO>> getDouars(@PathVariable Long communeId) {
        return ResponseEntity.ok(dossierCreationService.getDouarsByCommune(communeId));
    }
}