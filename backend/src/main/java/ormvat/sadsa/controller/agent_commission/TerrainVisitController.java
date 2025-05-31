package ormvat.sadsa.controller.agent_commission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ormvat.sadsa.dto.agent_commission.TerrainVisitDTOs.*;
import ormvat.sadsa.service.agent_commission.TerrainVisitService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/agent_commission/terrain-visits")
@RequiredArgsConstructor
@Slf4j
public class TerrainVisitController {

    private final TerrainVisitService terrainVisitService;

    /**
     * Get list of terrain visits for the commission agent
     */
    @GetMapping
    public ResponseEntity<VisitListResponse> getTerrainVisits(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String searchTerm,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Récupération des visites terrain pour l'utilisateur: {}", userEmail);

            VisitListResponse response = terrainVisitService.getTerrainVisits(
                    page, size, statut, searchTerm, userEmail);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des visites terrain", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get specific terrain visit details
     */
    @GetMapping("/{visiteId}")
    public ResponseEntity<VisiteTerrainResponse> getTerrainVisitDetail(
            @PathVariable Long visiteId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Récupération détail visite terrain {} pour l'utilisateur: {}", visiteId, userEmail);

            VisiteTerrainResponse response = terrainVisitService.getTerrainVisitDetail(visiteId, userEmail);
            
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la récupération de la visite terrain {}: {}", visiteId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Erreur technique lors de la récupération de la visite terrain {}", visiteId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Schedule a terrain visit for a dossier
     */
    @PostMapping("/schedule")
    public ResponseEntity<TerrainVisitActionResponse> scheduleTerrainVisit(
            @Valid @RequestBody ScheduleVisitRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Programmation visite terrain pour le dossier {} par l'utilisateur: {}", 
                    request.getDossierId(), userEmail);

            TerrainVisitActionResponse response = terrainVisitService.scheduleTerrainVisit(request, userEmail);
            
            log.info("Visite terrain programmée avec succès pour le dossier {}", request.getDossierId());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la programmation de la visite terrain: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                TerrainVisitActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de la programmation de la visite terrain", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                TerrainVisitActionResponse.builder()
                    .success(false)
                    .message("Erreur technique lors de la programmation")
                    .build()
            );
        }
    }

    /**
     * Complete a terrain visit
     */
    @PostMapping("/complete")
    public ResponseEntity<TerrainVisitActionResponse> completeTerrainVisit(
            @Valid @RequestBody CompleteVisitRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Finalisation visite terrain {} par l'utilisateur: {}", 
                    request.getVisiteId(), userEmail);

            TerrainVisitActionResponse response = terrainVisitService.completeTerrainVisit(request, userEmail);
            
            log.info("Visite terrain finalisée avec succès: {}", request.getVisiteId());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la finalisation de la visite terrain: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                TerrainVisitActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de la finalisation de la visite terrain", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                TerrainVisitActionResponse.builder()
                    .success(false)
                    .message("Erreur technique lors de la finalisation")
                    .build()
            );
        }
    }

    /**
     * Upload photos for a terrain visit
     */
    @PostMapping("/{visiteId}/photos")
    public ResponseEntity<TerrainVisitActionResponse> uploadVisitPhotos(
            @PathVariable Long visiteId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) List<String> descriptions,
            @RequestParam(required = false) List<String> coordonnees,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Upload photos pour visite terrain {} par l'utilisateur: {}", visiteId, userEmail);

            TerrainVisitActionResponse response = terrainVisitService.uploadVisitPhotos(
                    visiteId, files, descriptions, coordonnees, userEmail);
            
            log.info("Photos uploadées avec succès pour la visite terrain {}", visiteId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de l'upload des photos: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                TerrainVisitActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de l'upload des photos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                TerrainVisitActionResponse.builder()
                    .success(false)
                    .message("Erreur technique lors de l'upload")
                    .build()
            );
        }
    }

    /**
     * Update terrain visit details
     */
    @PutMapping("/{visiteId}")
    public ResponseEntity<TerrainVisitActionResponse> updateTerrainVisit(
            @PathVariable Long visiteId,
            @Valid @RequestBody ScheduleVisitRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Mise à jour visite terrain {} par l'utilisateur: {}", visiteId, userEmail);

            TerrainVisitActionResponse response = terrainVisitService.updateTerrainVisit(
                    visiteId, request, userEmail);
            
            log.info("Visite terrain mise à jour avec succès: {}", visiteId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la mise à jour de la visite terrain: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                TerrainVisitActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de la mise à jour de la visite terrain", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                TerrainVisitActionResponse.builder()
                    .success(false)
                    .message("Erreur technique lors de la mise à jour")
                    .build()
            );
        }
    }
}