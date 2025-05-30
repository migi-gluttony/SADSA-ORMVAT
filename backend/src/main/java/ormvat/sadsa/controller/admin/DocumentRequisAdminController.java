package ormvat.sadsa.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ormvat.sadsa.dto.admin.DocumentRequisDTOs.*;
import ormvat.sadsa.service.admin.DocumentRequisManagementService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/documents-requis")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class DocumentRequisAdminController {

    private final DocumentRequisManagementService documentService;

    /**
     * Get all rubriques with their documents
     */
    @GetMapping
    public ResponseEntity<DocumentRequisManagementResponse> getAllRubriquesWithDocuments() {
        try {
            log.info("Récupération de toutes les rubriques avec leurs documents");
            DocumentRequisManagementResponse response = documentService.getAllRubriquesWithDocuments();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des rubriques avec documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get documents for a specific sous-rubrique
     */
    @GetMapping("/sous-rubrique/{sousRubriqueId}")
    public ResponseEntity<List<DocumentRequisResponse>> getDocumentsBySousRubrique(
            @PathVariable Long sousRubriqueId) {
        try {
            List<DocumentRequisResponse> documents = documentService.getDocumentsBySousRubrique(sousRubriqueId);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des documents pour la sous-rubrique: {}", sousRubriqueId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new document requis
     */
    @PostMapping
    public ResponseEntity<?> createDocumentRequis(
            @RequestParam("nomDocument") String nomDocument,
            @RequestParam("description") String description,
            @RequestParam("obligatoire") Boolean obligatoire,
            @RequestParam("sousRubriqueId") Long sousRubriqueId,
            @RequestParam(value = "proprietes", required = false) String proprietes,
            @RequestParam(value = "jsonFile", required = false) MultipartFile jsonFile,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Création d'un nouveau document requis par l'utilisateur: {}, Document: {}", 
                    userEmail, nomDocument);

            // Build request from form parameters
            CreateDocumentRequisRequest request = CreateDocumentRequisRequest.builder()
                    .nomDocument(nomDocument)
                    .description(description)
                    .obligatoire(obligatoire)
                    .sousRubriqueId(sousRubriqueId)
                    .build();

            // Parse proprietes JSON if provided
            if (proprietes != null && !proprietes.trim().isEmpty()) {
                try {
                    // Simple JSON validation - could use ObjectMapper for more robust parsing
                    request.setProprietes(java.util.Map.of("raw", proprietes));
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(createErrorResponse("Format JSON invalide pour les propriétés"));
                }
            }

            DocumentRequisResponse response = documentService.createDocumentRequis(request, jsonFile, userEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la création du document requis: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur technique lors de la création du document requis", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur technique lors de la création du document"));
        }
    }

    /**
     * Update an existing document requis
     */
    @PutMapping("/{documentId}")
    public ResponseEntity<?> updateDocumentRequis(
            @PathVariable Long documentId,
            @RequestParam("nomDocument") String nomDocument,
            @RequestParam("description") String description,
            @RequestParam("obligatoire") Boolean obligatoire,
            @RequestParam(value = "proprietes", required = false) String proprietes,
            @RequestParam(value = "jsonFile", required = false) MultipartFile jsonFile,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            
            log.info("Mise à jour du document requis ID: {} par l'utilisateur: {}", 
                    documentId, userEmail);

            // Build request from form parameters
            UpdateDocumentRequisRequest request = UpdateDocumentRequisRequest.builder()
                    .id(documentId)
                    .nomDocument(nomDocument)
                    .description(description)
                    .obligatoire(obligatoire)
                    .build();

            // Parse proprietes JSON if provided
            if (proprietes != null && !proprietes.trim().isEmpty()) {
                try {
                    // Simple JSON validation - could use ObjectMapper for more robust parsing
                    request.setProprietes(java.util.Map.of("raw", proprietes));
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(createErrorResponse("Format JSON invalide pour les propriétés"));
                }
            }

            DocumentRequisResponse response = documentService.updateDocumentRequis(request, jsonFile, userEmail);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la mise à jour du document requis: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur technique lors de la mise à jour du document requis", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur technique lors de la mise à jour"));
        }
    }

    /**
     * Delete a document requis
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deleteDocumentRequis(
            @PathVariable Long documentId,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Suppression du document requis ID: {} par l'utilisateur: {}", 
                    documentId, userEmail);

            documentService.deleteDocumentRequis(documentId, userEmail);
            
            return ResponseEntity.ok(createSuccessResponse("Document requis supprimé avec succès"));

        } catch (RuntimeException e) {
            log.error("Erreur lors de la suppression du document requis: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur technique lors de la suppression du document requis", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erreur technique lors de la suppression"));
        }
    }

    /**
     * Download JSON file for a document requis
     */
    @GetMapping("/{documentId}/download-json")
    public ResponseEntity<Resource> downloadJsonFile(@PathVariable Long documentId) {
        try {
            return documentService.downloadJsonFile(documentId);
        } catch (Exception e) {
            log.error("Erreur lors du téléchargement du fichier JSON pour le document: {}", documentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("DocumentRequis Admin Service is running");
    }

    // Helper methods for consistent error responses
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