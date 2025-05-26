package ormvat.sadsa.controller.agent_antenne;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ormvat.sadsa.dto.agent_antenne.DossierManagementDTOs.*;
import ormvat.sadsa.service.agent_antenne.DossierManagementService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/agent_antenne/dossiers")
@RequiredArgsConstructor
@Slf4j
public class DossierManagementController {

    private final DossierManagementService dossierManagementService;

    /**
     * Get paginated list of dossiers for current user
     */
    @GetMapping
    public ResponseEntity<DossierListResponse> getDossiersList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateCreation") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Long sousRubriqueId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            
            DossierSearchRequest searchRequest = DossierSearchRequest.builder()
                    .page(page)
                    .size(size)
                    .sortBy(sortBy)
                    .sortDirection(sortDirection)
                    .searchTerm(searchTerm)
                    .statut(statut)
                    .sousRubriqueId(sousRubriqueId)
                    .build();

            DossierListResponse response = dossierManagementService.getDossiersList(userEmail, searchRequest);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting dossiers list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get detailed information about a specific dossier
     */
    @GetMapping("/{dossierId}")
    public ResponseEntity<DossierDetailResponse> getDossierDetail(
            @PathVariable Long dossierId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            DossierDetailResponse response = dossierManagementService.getDossierDetail(dossierId, userEmail);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.warn("Access denied or dossier not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error getting dossier detail: {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Submit form data with optional file uploads
     */
    @PostMapping("/{dossierId}/forms")
    public ResponseEntity<?> submitFormData(
            @PathVariable Long dossierId,
            @RequestParam String formId,
            @RequestParam(required = false) String formDataJson,
            @RequestParam(required = false) List<MultipartFile> files,
            @RequestParam(required = false) List<String> fileTitles,
            @RequestParam(required = false) List<Boolean> originalFlags,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            
            // Parse form data
            FormSubmissionRequest request = FormSubmissionRequest.builder()
                    .dossierId(dossierId)
                    .formId(formId)
                    .build();

            if (formDataJson != null && !formDataJson.isEmpty()) {
                // Parse JSON form data
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> formData = mapper.readValue(formDataJson, java.util.Map.class);
                request.setFormData(formData);
            }

            // Build file requests
            if (files != null && !files.isEmpty()) {
                List<FileUploadRequest> fileRequests = new java.util.ArrayList<>();
                for (int i = 0; i < files.size(); i++) {
                    String title = (fileTitles != null && i < fileTitles.size()) ? 
                        fileTitles.get(i) : files.get(i).getOriginalFilename();
                    boolean isOriginal = (originalFlags != null && i < originalFlags.size()) ? 
                        originalFlags.get(i) : false;
                    
                    fileRequests.add(FileUploadRequest.builder()
                            .fileName(files.get(i).getOriginalFilename())
                            .customTitle(title)
                            .isOriginalDocument(isOriginal)
                            .build());
                }
                request.setFiles(fileRequests);
            }

            dossierManagementService.submitFormData(request, files, userEmail);
            
            return ResponseEntity.ok().body(java.util.Map.of(
                "success", true,
                "message", "Formulaire soumis avec succès"
            ));
            
        } catch (RuntimeException e) {
            log.warn("Form submission error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error submitting form data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                "success", false,
                "message", "Erreur lors de la soumission du formulaire"
            ));
        }
    }

    /**
     * Delete a dossier
     */
    @DeleteMapping("/{dossierId}")
    public ResponseEntity<DossierActionResponse> deleteDossier(
            @PathVariable Long dossierId,
            @RequestParam(required = false) String comment,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            DossierActionResponse response = dossierManagementService.deleteDossier(
                dossierId, userEmail, comment);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Error deleting dossier: {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DossierActionResponse.builder()
                            .success(false)
                            .message("Erreur lors de la suppression")
                            .build());
        }
    }

    /**
     * Send dossier to GUC
     */
    @PostMapping("/{dossierId}/send-to-guc")
    public ResponseEntity<DossierActionResponse> sendDossierToGUC(
            @PathVariable Long dossierId,
            @RequestParam(required = false) String comment,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            DossierActionResponse response = dossierManagementService.sendDossierToGUC(
                dossierId, userEmail, comment);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Error sending dossier to GUC: {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DossierActionResponse.builder()
                            .success(false)
                            .message("Erreur lors de l'envoi")
                            .build());
        }
    }

    /**
     * Download a file
     */
    @GetMapping("/{dossierId}/files/{fileId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(
            @PathVariable Long dossierId,
            @PathVariable Long fileId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            // Implementation for file download
            // This would involve retrieving the file from storage and returning it
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"document.pdf\"")
                    .body(null); // Implement actual file retrieval
                    
        } catch (Exception e) {
            log.error("Error downloading file: {}", fileId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get form configuration for a specific dossier
     */
    @GetMapping("/{dossierId}/forms/{formId}")
    public ResponseEntity<java.util.Map<String, Object>> getFormConfiguration(
            @PathVariable Long dossierId,
            @PathVariable String formId,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            // Get the form configuration from the dossier detail
            DossierDetailResponse detail = dossierManagementService.getDossierDetail(dossierId, userEmail);
            
            java.util.Optional<FormConfigurationDTO> formConfig = detail.getAvailableForms().stream()
                    .filter(form -> form.getFormId().equals(formId))
                    .findFirst();
            
            if (formConfig.isPresent()) {
                return ResponseEntity.ok(formConfig.get().getFormConfig());
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("Error getting form configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}