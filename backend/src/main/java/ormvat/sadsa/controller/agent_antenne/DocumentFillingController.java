package ormvat.sadsa.controller.agent_antenne;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ormvat.sadsa.dto.agent_antenne.DocumentFillingDTOs.*;
import ormvat.sadsa.service.agent_antenne.DocumentFillingService;

import java.security.Principal;

@RestController
@RequestMapping("/api/agent_antenne/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentFillingController {

    private final DocumentFillingService documentFillingService;

    /**
     * Get all documents for a dossier with completion status
     */
    @GetMapping("/dossier/{dossierId}")
    public ResponseEntity<ApiResponse<DossierDocumentsResponse>> getDossierDocuments(
            @PathVariable Long dossierId,
            Principal principal) {
        try {
            DossierDocumentsResponse response = documentFillingService.getDossierDocuments(dossierId, principal.getName());
            return ResponseEntity.ok(ApiResponse.success(response, "Documents récupérés avec succès"));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des documents du dossier {}: {}", dossierId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Upload a scanned document file
     */
    @PostMapping("/upload-scanned/{dossierId}/{documentRequisId}")
    public ResponseEntity<ApiResponse<UploadDocumentResponse>> uploadScannedDocument(
            @PathVariable Long dossierId,
            @PathVariable Long documentRequisId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            Principal principal) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Aucun fichier sélectionné"));
            }

            if (title == null || title.trim().isEmpty()) {
                title = "Document scanné - " + file.getOriginalFilename();
            }

            UploadDocumentResponse response = documentFillingService.uploadScannedDocument(
                    dossierId, documentRequisId, file, title, description, principal.getName());
            
            return ResponseEntity.ok(ApiResponse.success(response, "Document uploadé avec succès"));
        } catch (Exception e) {
            log.error("Erreur lors de l'upload du document: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Save form data as JSON
     */
    @PostMapping("/save-form-data")
    public ResponseEntity<ApiResponse<SaveFormDataResponse>> saveFormData(
            @RequestBody SaveFormDataRequest request,
            Principal principal) {
        try {
            SaveFormDataResponse response = documentFillingService.saveFormData(request, principal.getName());
            return ResponseEntity.ok(ApiResponse.success(response, "Données du formulaire sauvegardées"));
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde des données: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Upload mixed document (file + form data)
     */
    @PostMapping("/upload-mixed/{dossierId}/{documentRequisId}")
    public ResponseEntity<ApiResponse<UploadDocumentResponse>> uploadMixedDocument(
            @PathVariable Long dossierId,
            @PathVariable Long documentRequisId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("formData") String formDataJson,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            Principal principal) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Aucun fichier sélectionné"));
            }

            // Parse form data JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> formDataMap = mapper.readValue(formDataJson, 
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});

            SaveMixedDocumentRequest request = SaveMixedDocumentRequest.builder()
                    .dossierId(dossierId)
                    .documentRequisId(documentRequisId)
                    .formData(formDataMap)
                    .title(title != null ? title : "Document mixte - " + file.getOriginalFilename())
                    .description(description)
                    .build();

            UploadDocumentResponse response = documentFillingService.saveMixedDocument(
                    dossierId, documentRequisId, file, request, principal.getName());
            
            return ResponseEntity.ok(ApiResponse.success(response, "Document mixte sauvegardé avec succès"));
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du document mixte: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Delete a document piece
     */
    @DeleteMapping("/piece/{pieceJointeId}")
    public ResponseEntity<ApiResponse<DeleteDocumentResponse>> deleteDocument(
            @PathVariable Long pieceJointeId,
            Principal principal) {
        try {
            DeleteDocumentResponse response = documentFillingService.deleteDocument(pieceJointeId, principal.getName());
            return ResponseEntity.ok(ApiResponse.success(response, "Document supprimé avec succès"));
        } catch (Exception e) {
            log.error("Erreur lors de la suppression du document: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    /**
     * Download a document file
     */
    @GetMapping("/download/{pieceJointeId}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long pieceJointeId,
            Principal principal) {
        try {
            return documentFillingService.downloadDocument(pieceJointeId, principal.getName());
        } catch (Exception e) {
            log.error("Erreur lors du téléchargement du document: {}", e.getMessage());
            throw new RuntimeException("Erreur lors du téléchargement: " + e.getMessage());
        }
    }

    /**
     * Get form data for a specific document (for editing)
     */
    @GetMapping("/form-data/{dossierId}/{documentRequisId}")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getFormData(
            @PathVariable Long dossierId,
            @PathVariable Long documentRequisId,
            Principal principal) {
        try {
            // This will be handled by the main getDossierDocuments endpoint
            // But can be useful for targeted form data retrieval
            DossierDocumentsResponse response = documentFillingService.getDossierDocuments(dossierId, principal.getName());
            
            java.util.Map<String, Object> formData = response.getDocumentsRequis().stream()
                    .filter(doc -> doc.getDocumentRequisId().equals(documentRequisId))
                    .flatMap(doc -> doc.getPieces().stream())
                    .filter(piece -> piece.getHasFormData())
                    .findFirst()
                    .map(PieceJointeDTO::getFormData)
                    .orElse(new java.util.HashMap<>());
            
            return ResponseEntity.ok(ApiResponse.success(formData, "Données du formulaire récupérées"));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des données du formulaire: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }
}