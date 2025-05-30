package ormvat.sadsa.controller.agent_antenne;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ormvat.sadsa.dto.agent_antenne.DocumentFillingDTOs.*;
import ormvat.sadsa.service.agent_antenne.DocumentFillingService;

@RestController
@RequestMapping("/api/agent_antenne/dossiers/{dossierId}/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentFillingController {

    private final DocumentFillingService documentFillingService;

    /**
     * Get dossier details with required documents and uploaded files
     */
    @GetMapping
    public ResponseEntity<?> getDossierDocuments(
            @PathVariable Long dossierId,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Récupération des documents pour le dossier: {} par l'utilisateur: {}", dossierId, userEmail);
            
            DossierDocumentsResponse response = documentFillingService.getDossierDocuments(dossierId, userEmail);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Erreur lors de la récupération des documents du dossier: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur technique lors de la récupération des documents du dossier: {}", dossierId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur technique lors du chargement des documents"));
        }
    }

    /**
     * Upload a file for a specific document requis
     */
    @PostMapping("/{documentRequisId}/upload")
    public ResponseEntity<?> uploadDocument(
            @PathVariable Long dossierId,
            @PathVariable Long documentRequisId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Upload document pour dossier: {}, documentRequis: {}, fichier: {}, utilisateur: {}", 
                    dossierId, documentRequisId, file.getOriginalFilename(), userEmail);

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Fichier vide"));
            }

            // Validate file type (PDF or images)
            String contentType = file.getContentType();
            if (!isValidFileType(contentType)) {
                return ResponseEntity.badRequest().body(createErrorResponse(
                    "Type de fichier non autorisé. Seuls les PDF et images sont acceptés"));
            }

            // Check file size (10MB max)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(createErrorResponse(
                    "Fichier trop volumineux. Taille maximum: 10MB"));
            }

            UploadDocumentResponse response = documentFillingService.uploadDocument(
                    dossierId, documentRequisId, file, userEmail);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (RuntimeException e) {
            log.error("Erreur lors de l'upload du document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur technique lors de l'upload du document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur technique lors de l'upload"));
        }
    }

    /**
     * Save form data for a document requis
     */
    @PostMapping("/{documentRequisId}/form-data")
    public ResponseEntity<?> saveFormData(
            @PathVariable Long dossierId,
            @PathVariable Long documentRequisId,
            @RequestBody SaveFormDataRequest request,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Sauvegarde données formulaire pour dossier: {}, documentRequis: {}, utilisateur: {}", 
                    dossierId, documentRequisId, userEmail);

            // Set path parameters in request
            request.setDossierId(dossierId);
            request.setDocumentRequisId(documentRequisId);

            SaveFormDataResponse response = documentFillingService.saveFormData(request, userEmail);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Erreur lors de la sauvegarde des données du formulaire: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur technique lors de la sauvegarde des données du formulaire", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur technique lors de la sauvegarde"));
        }
    }

    /**
     * Delete an uploaded document
     */
    @DeleteMapping("/piece-jointe/{pieceJointeId}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable Long dossierId,
            @PathVariable Long pieceJointeId,
            @RequestParam(value = "motif", required = false) String motif,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Suppression document ID: {} pour dossier: {}, utilisateur: {}, motif: {}", 
                    pieceJointeId, dossierId, userEmail, motif);

            documentFillingService.deleteDocument(pieceJointeId, userEmail);
            
            return ResponseEntity.ok(createSuccessResponse("Document supprimé avec succès"));
            
        } catch (RuntimeException e) {
            log.error("Erreur lors de la suppression du document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur technique lors de la suppression du document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur technique lors de la suppression"));
        }
    }

    /**
     * Download an uploaded document
     */
    @GetMapping("/piece-jointe/{pieceJointeId}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long dossierId,
            @PathVariable Long pieceJointeId,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Téléchargement document ID: {} pour dossier: {}, utilisateur: {}", 
                    pieceJointeId, dossierId, userEmail);

            return documentFillingService.downloadDocument(pieceJointeId, userEmail);
            
        } catch (Exception e) {
            log.error("Erreur lors du téléchargement du document: {}", pieceJointeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Document Filling Service is running");
    }

    // Helper methods

    private boolean isValidFileType(String contentType) {
        if (contentType == null) return false;
        
        return contentType.equals("application/pdf") ||
               contentType.startsWith("image/") ||
               contentType.equals("application/octet-stream"); // For some PDF uploads
    }

    private Object createErrorResponse(String message) {
        return new ApiResponse(false, message);
    }

    private Object createSuccessResponse(String message) {
        return new ApiResponse(true, message);
    }

    // Inner class for API responses
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ApiResponse {
        private boolean success;
        private String message;
    }
}