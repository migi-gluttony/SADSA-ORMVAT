package ormvat.sadsa.controller.service_technique;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ormvat.sadsa.service.service_technique.ServiceTechniqueDossierService;
import ormvat.sadsa.service.agent_antenne.DocumentFillingService;
import ormvat.sadsa.dto.role_based.RoleBasedDossierDTOs.*;
import ormvat.sadsa.dto.service_technique.ImplementationVisitDTOs.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/service-technique")
@RequiredArgsConstructor
@Slf4j
public class ServiceTechniqueDossierController {

    private final ServiceTechniqueDossierService dossierService;
    private final DocumentFillingService documentFillingService; // For photo downloads

    // === DOSSIER MANAGEMENT ENDPOINTS ===

    @GetMapping("/dossiers")
    public ResponseEntity<DossierListResponse> getDossiersForImplementation(Authentication authentication) {
        try {
            DossierListResponse response = dossierService.getDossiersForImplementation(authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur récupération dossiers service technique: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/dossiers/detail/{id}")
    public ResponseEntity<DossierDetailResponse> getDossierDetail(@PathVariable Long id, Authentication authentication) {
        try {
            DossierDetailResponse response = dossierService.getDossierById(id, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur récupération détail dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/dossiers/verify/{id}")
    public ResponseEntity<ActionResponse> verifyImplementation(@PathVariable Long id,
                                                              @Valid @RequestBody VerificationRequest request,
                                                              Authentication authentication) {
        try {
            ActionResponse response = dossierService.verifyImplementation(id, request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur vérification implémentation dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/dossiers/complete/{id}")
    public ResponseEntity<ActionResponse> markComplete(@PathVariable Long id,
                                                      @Valid @RequestBody CompletionRequest request,
                                                      Authentication authentication) {
        try {
            ActionResponse response = dossierService.markComplete(id, request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur finalisation dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/dossiers/report-issues/{id}")
    public ResponseEntity<ActionResponse> reportIssues(@PathVariable Long id,
                                                      @Valid @RequestBody IssueRequest request,
                                                      Authentication authentication) {
        try {
            ActionResponse response = dossierService.reportIssues(id, request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur signalement problèmes dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    // === IMPLEMENTATION VISIT MANAGEMENT ENDPOINTS ===

    /**
     * Get dashboard summary for service technique agent
     */
    @GetMapping("/visits/dashboard")
    public ResponseEntity<ImplementationDashboardSummaryDTO> getDashboardSummary(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Récupération tableau de bord visites implémentation pour: {}", userEmail);

            ImplementationDashboardSummaryDTO summary = dossierService.getDashboardSummary(userEmail);
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération du tableau de bord", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get list of implementation visits with filtering and pagination
     */
    @GetMapping("/visits")
    public ResponseEntity<ImplementationVisitListResponse> getImplementationVisits(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String antenne,
            @RequestParam(required = false) String dateDebut,
            @RequestParam(required = false) String dateFin,
            @RequestParam(required = false) Boolean enRetard,
            @RequestParam(required = false) Boolean aCompleter,
            @RequestParam(required = false) Integer pourcentageMin,
            @RequestParam(required = false) Integer pourcentageMax,
            @RequestParam(defaultValue = "dateVisite") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Récupération des visites d'implémentation pour: {}, page: {}", userEmail, page);

            // Build filter request
            ImplementationVisitFilterRequest filterRequest = ImplementationVisitFilterRequest.builder()
                    .statut(statut)
                    .searchTerm(searchTerm)
                    .antenne(antenne)
                    .dateDebut(dateDebut != null ? java.time.LocalDate.parse(dateDebut) : null)
                    .dateFin(dateFin != null ? java.time.LocalDate.parse(dateFin) : null)
                    .enRetard(enRetard)
                    .aCompleter(aCompleter)
                    .pourcentageMin(pourcentageMin)
                    .pourcentageMax(pourcentageMax)
                    .sortBy(sortBy)
                    .sortDirection(sortDirection)
                    .page(page)
                    .size(size)
                    .build();

            ImplementationVisitListResponse response = dossierService.getImplementationVisits(filterRequest, userEmail);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des visites d'implémentation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get specific implementation visit details
     */
    @GetMapping("/visits/{visiteId}")
    public ResponseEntity<VisiteImplementationResponse> getImplementationVisitDetail(
            @PathVariable Long visiteId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Récupération détail visite implémentation {} pour: {}", visiteId, userEmail);

            VisiteImplementationResponse response = dossierService.getImplementationVisitDetail(visiteId, userEmail);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la récupération de la visite d'implémentation {}: {}", visiteId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Erreur technique lors de la récupération de la visite d'implémentation {}", visiteId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Schedule an implementation visit for a dossier
     */
    @PostMapping("/visits/schedule")
    public ResponseEntity<ImplementationVisitActionResponse> scheduleImplementationVisit(
            @Valid @RequestBody ScheduleImplementationVisitRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Programmation visite implémentation pour dossier {} par: {}", 
                    request.getDossierId(), userEmail);

            ImplementationVisitActionResponse response = dossierService.scheduleImplementationVisit(request, userEmail);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la programmation de la visite d'implémentation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ImplementationVisitActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de la programmation de la visite d'implémentation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ImplementationVisitActionResponse.builder()
                    .success(false)
                    .message("Erreur technique lors de la programmation")
                    .build()
            );
        }
    }

    /**
     * Complete an implementation visit with approval/rejection
     */
    @PostMapping("/visits/{visiteId}/complete")
    public ResponseEntity<ImplementationVisitActionResponse> completeImplementationVisit(
            @PathVariable Long visiteId,
            @Valid @RequestBody CompleteImplementationVisitRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            request.setVisiteId(visiteId);
            log.info("Finalisation visite implémentation {} par: {}", visiteId, userEmail);

            ImplementationVisitActionResponse response = dossierService.completeImplementationVisit(request, userEmail);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la finalisation de la visite d'implémentation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ImplementationVisitActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de la finalisation de la visite d'implémentation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ImplementationVisitActionResponse.builder()
                    .success(false)
                    .message("Erreur technique")
                    .build()
            );
        }
    }

    /**
     * Upload photos for implementation visit
     */
    @PostMapping("/visits/{visiteId}/photos")
    public ResponseEntity<ImplementationVisitActionResponse> uploadImplementationVisitPhotos(
            @PathVariable Long visiteId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) List<String> descriptions,
            @RequestParam(required = false) List<String> coordonnees,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Upload photos pour visite implémentation {} par: {}", visiteId, userEmail);

            ImplementationVisitActionResponse response = dossierService.uploadImplementationVisitPhotos(
                    visiteId, files, descriptions, coordonnees, userEmail);
            
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de l'upload des photos: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ImplementationVisitActionResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Erreur technique lors de l'upload des photos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ImplementationVisitActionResponse.builder()
                    .success(false)
                    .message("Erreur technique")
                    .build()
            );
        }
    }

    /**
     * Download a photo from implementation visit
     */
    @GetMapping("/visits/photos/{pieceJointeId}/download")
    public ResponseEntity<Resource> downloadImplementationVisitPhoto(
            @PathVariable Long pieceJointeId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Téléchargement photo implémentation {} par: {}", pieceJointeId, userEmail);

            // Use the document service for downloads
            return documentFillingService.downloadDocument(pieceJointeId, userEmail);

        } catch (Exception e) {
            log.error("Erreur lors du téléchargement de la photo d'implémentation: {}", e.getMessage());
            throw new RuntimeException("Erreur lors du téléchargement: " + e.getMessage());
        }
    }

    // Helper class for API responses  
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public static <T> ApiResponse<T> success(T data, String message) {
            return new ApiResponse<>(true, message, data);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public T getData() { return data; }
    }
}