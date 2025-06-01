package ormvat.sadsa.controller.agent_commission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ormvat.sadsa.dto.agent_commission.TerrainVisitDTOs.*;
import ormvat.sadsa.service.agent_commission.TerrainVisitService;

import jakarta.validation.Valid;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/agent_commission/terrain-visits")
@RequiredArgsConstructor
@Slf4j
public class TerrainVisitController {

    private final TerrainVisitService terrainVisitService;

    /**
     * Get dashboard summary for commission agent
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Récupération tableau de bord visites terrain pour: {}", userEmail);

            DashboardSummaryDTO summary = terrainVisitService.getDashboardSummary(userEmail);
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération du tableau de bord", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get list of terrain visits with filtering and pagination
     */
    @GetMapping
    public ResponseEntity<VisitListResponse> getTerrainVisits(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String rubrique,
            @RequestParam(required = false) String antenne,
            @RequestParam(required = false) String dateDebut,
            @RequestParam(required = false) String dateFin,
            @RequestParam(required = false) Boolean enRetard,
            @RequestParam(required = false) Boolean aCompleter,
            @RequestParam(defaultValue = "dateVisite") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Récupération des visites terrain pour: {}, page: {}", userEmail, page);

            // Build filter request
            VisitFilterRequest filterRequest = VisitFilterRequest.builder()
                    .statut(statut)
                    .searchTerm(searchTerm)
                    .rubrique(rubrique)
                    .antenne(antenne)
                    .dateDebut(dateDebut != null ? java.time.LocalDate.parse(dateDebut) : null)
                    .dateFin(dateFin != null ? java.time.LocalDate.parse(dateFin) : null)
                    .enRetard(enRetard)
                    .aCompleter(aCompleter)
                    .sortBy(sortBy)
                    .sortDirection(sortDirection)
                    .page(page)
                    .size(size)
                    .build();

            VisitListResponse response = terrainVisitService.getTerrainVisits(filterRequest, userEmail);
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
            log.info("Récupération détail visite terrain {} pour: {}", visiteId, userEmail);

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
            log.info("Programmation visite terrain pour dossier {} par: {}", 
                    request.getDossierId(), userEmail);

            TerrainVisitActionResponse response = terrainVisitService.scheduleTerrainVisit(request, userEmail);
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
     * Update visit notes (for auto-save functionality)
     */
    @PutMapping("/{visiteId}/notes")
    public ResponseEntity<TerrainVisitActionResponse> updateVisitNotes(
            @PathVariable Long visiteId,
            @Valid @RequestBody UpdateVisitNotesRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            request.setVisiteId(visiteId);

            TerrainVisitActionResponse response = terrainVisitService.updateVisitNotes(request, userEmail);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la mise à jour des notes: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                TerrainVisitActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de la mise à jour des notes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                TerrainVisitActionResponse.builder()
                    .success(false)
                    .message("Erreur technique")
                    .build()
            );
        }
    }

    /**
     * Complete a terrain visit with approval/rejection
     */
    @PostMapping("/{visiteId}/complete")
    public ResponseEntity<TerrainVisitActionResponse> completeTerrainVisit(
            @PathVariable Long visiteId,
            @Valid @RequestBody CompleteVisitRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            request.setVisiteId(visiteId);
            log.info("Finalisation visite terrain {} par: {}", visiteId, userEmail);

            TerrainVisitActionResponse response = terrainVisitService.completeTerrainVisit(request, userEmail);
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
                    .message("Erreur technique")
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
            log.info("Upload photos pour visite terrain {} par: {}", visiteId, userEmail);

            TerrainVisitActionResponse response = terrainVisitService.uploadVisitPhotos(
                    visiteId, files, descriptions, coordonnees, userEmail);
            
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
                    .message("Erreur technique")
                    .build()
            );
        }
    }

    /**
     * Delete a photo from terrain visit
     */
    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<TerrainVisitActionResponse> deleteVisitPhoto(
            @PathVariable Long photoId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Suppression photo {} par: {}", photoId, userEmail);

            TerrainVisitActionResponse response = terrainVisitService.deleteVisitPhoto(photoId, userEmail);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la suppression de la photo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                TerrainVisitActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de la suppression de la photo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                TerrainVisitActionResponse.builder()
                    .success(false)
                    .message("Erreur technique")
                    .build()
            );
        }
    }

    /**
     * Download a photo from terrain visit
     */
    @GetMapping("/photos/{photoId}/download")
    public ResponseEntity<Resource> downloadVisitPhoto(
            @PathVariable Long photoId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Téléchargement photo {} par: {}", photoId, userEmail);

            // This would need implementation in the service to get photo details
            // For now, return a placeholder implementation
            
            // Path photoPath = terrainVisitService.getPhotoPath(photoId, userEmail);
            // Resource resource = new UrlResource(photoPath.toUri());
            
            // Placeholder - in real implementation, get actual file path from service
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Erreur lors du téléchargement de la photo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get available dossiers for terrain visit scheduling
     */
    @GetMapping("/available-dossiers")
    public ResponseEntity<List<Object>> getAvailableDossiers(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Récupération dossiers disponibles pour visite terrain par: {}", userEmail);

            // This would be implemented in the service to get dossiers awaiting terrain visit
            // For now, return empty list
            return ResponseEntity.ok(List.of());

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des dossiers disponibles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}