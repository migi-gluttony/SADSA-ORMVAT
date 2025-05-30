package ormvat.sadsa.service.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import ormvat.sadsa.dto.admin.DocumentRequisDTOs.*;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentRequisManagementService {

    private final DocumentRequisRepository documentRequisRepository;
    private final SousRubriqueRepository sousRubriqueRepository;
    private final RubriqueRepository rubriqueRepository;
    private final AuditTrailRepository auditTrailRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Value("${app.upload.dir:./uploads/ormvat_sadsa}")
    private String uploadDir;

    private final long maxFileSize = 10 * 1024 * 1024; // 10MB max for JSON files

    /**
     * Get all rubriques with their sous-rubriques and documents
     */
    public DocumentRequisManagementResponse getAllRubriquesWithDocuments() {
        try {
            List<Rubrique> rubriques = rubriqueRepository.findAll();
            
            List<RubriqueWithDocumentsResponse> rubriqueResponses = rubriques.stream()
                    .map(this::mapToRubriqueWithDocuments)
                    .collect(Collectors.toList());

            // Calculate statistics
            long totalDocuments = documentRequisRepository.count();
            long totalObligatoires = documentRequisRepository.findAll().stream()
                    .mapToLong(doc -> Boolean.TRUE.equals(doc.getObligatoire()) ? 1 : 0)
                    .sum();
            long totalOptionnels = totalDocuments - totalObligatoires;

            return DocumentRequisManagementResponse.builder()
                    .rubriques(rubriqueResponses)
                    .totalDocuments(totalDocuments)
                    .totalObligatoires(totalObligatoires)
                    .totalOptionnels(totalOptionnels)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des rubriques avec documents", e);
            throw new RuntimeException("Erreur lors du chargement des données");
        }
    }

    /**
     * Create a new document requis
     */
    @Transactional
    public DocumentRequisResponse createDocumentRequis(CreateDocumentRequisRequest request, MultipartFile jsonFile, String userEmail) {
        try {
            // Validate sous-rubrique exists
            SousRubrique sousRubrique = sousRubriqueRepository.findById(request.getSousRubriqueId())
                    .orElseThrow(() -> new RuntimeException("Sous-rubrique non trouvée"));

            // Handle JSON file upload if provided
            String jsonFilePath = null;
            if (jsonFile != null && !jsonFile.isEmpty()) {
                jsonFilePath = saveJsonFile(jsonFile, request.getNomDocument());
            }

            // Create document requis
            DocumentRequis documentRequis = new DocumentRequis();
            documentRequis.setNomDocument(request.getNomDocument());
            documentRequis.setDescription(request.getDescription());
            documentRequis.setObligatoire(request.getObligatoire());
            documentRequis.setLocationFormulaire(jsonFilePath);
            documentRequis.setSousRubrique(sousRubrique);

            DocumentRequis savedDocument = documentRequisRepository.save(documentRequis);

            // Create audit trail
            createAuditTrail("CREATION_DOCUMENT_REQUIS", savedDocument.getId(), 
                    "Création du document requis: " + savedDocument.getNomDocument() + 
                    (jsonFilePath != null ? " avec fichier JSON: " + jsonFilePath : ""), userEmail);

            log.info("Document requis créé avec succès - ID: {}, Nom: {}, Fichier JSON: {}", 
                    savedDocument.getId(), savedDocument.getNomDocument(), jsonFilePath);

            return mapToDocumentRequisResponse(savedDocument);

        } catch (Exception e) {
            log.error("Erreur lors de la création du document requis", e);
            throw new RuntimeException("Erreur lors de la création du document: " + e.getMessage());
        }
    }

    /**
     * Update an existing document requis
     */
    @Transactional
    public DocumentRequisResponse updateDocumentRequis(UpdateDocumentRequisRequest request, MultipartFile jsonFile, String userEmail) {
        try {
            DocumentRequis existingDocument = documentRequisRepository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("Document requis non trouvé"));

            // Store old values for audit
            String oldValues = String.format("Nom: %s, Description: %s, Obligatoire: %s, Fichier: %s", 
                    existingDocument.getNomDocument(), 
                    existingDocument.getDescription(),
                    existingDocument.getObligatoire(),
                    existingDocument.getLocationFormulaire());

            // Handle JSON file upload if provided
            String jsonFilePath = existingDocument.getLocationFormulaire(); // Keep existing if no new file
            if (jsonFile != null && !jsonFile.isEmpty()) {
                // Delete old file if it exists
                if (existingDocument.getLocationFormulaire() != null) {
                    deleteFile(existingDocument.getLocationFormulaire());
                }
                // Save new file
                jsonFilePath = saveJsonFile(jsonFile, request.getNomDocument());
            }

            // Update fields
            existingDocument.setNomDocument(request.getNomDocument());
            existingDocument.setDescription(request.getDescription());
            existingDocument.setObligatoire(request.getObligatoire());
            existingDocument.setLocationFormulaire(jsonFilePath);

            DocumentRequis savedDocument = documentRequisRepository.save(existingDocument);

            // Create audit trail
            String newValues = String.format("Nom: %s, Description: %s, Obligatoire: %s, Fichier: %s", 
                    savedDocument.getNomDocument(), 
                    savedDocument.getDescription(),
                    savedDocument.getObligatoire(),
                    savedDocument.getLocationFormulaire());

            createAuditTrailWithValues("MODIFICATION_DOCUMENT_REQUIS", savedDocument.getId(), 
                    "Modification du document requis: " + savedDocument.getNomDocument(), 
                    oldValues, newValues, userEmail);

            log.info("Document requis mis à jour avec succès - ID: {}, Fichier JSON: {}", 
                    savedDocument.getId(), jsonFilePath);

            return mapToDocumentRequisResponse(savedDocument);

        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du document requis", e);
            throw new RuntimeException("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    /**
     * Delete a document requis
     */
    @Transactional
    public void deleteDocumentRequis(Long documentId, String userEmail) {
        try {
            DocumentRequis document = documentRequisRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document requis non trouvé"));

            String documentName = document.getNomDocument();
            String filePath = document.getLocationFormulaire();
            
            // Delete associated JSON file if it exists
            if (filePath != null) {
                deleteFile(filePath);
            }
            
            documentRequisRepository.delete(document);

            // Create audit trail
            createAuditTrail("SUPPRESSION_DOCUMENT_REQUIS", documentId, 
                    "Suppression du document requis: " + documentName + 
                    (filePath != null ? " et fichier JSON: " + filePath : ""), userEmail);

            log.info("Document requis supprimé avec succès - ID: {}, Nom: {}, Fichier: {}", 
                    documentId, documentName, filePath);

        } catch (Exception e) {
            log.error("Erreur lors de la suppression du document requis", e);
            throw new RuntimeException("Erreur lors de la suppression: " + e.getMessage());
        }
    }

    /**
     * Get documents for a specific sous-rubrique
     */
    public List<DocumentRequisResponse> getDocumentsBySousRubrique(Long sousRubriqueId) {
        try {
            List<DocumentRequis> documents = documentRequisRepository.findBySousRubriqueId(sousRubriqueId);
            return documents.stream()
                    .map(this::mapToDocumentRequisResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des documents pour la sous-rubrique: {}", sousRubriqueId, e);
            throw new RuntimeException("Erreur lors du chargement des documents");
        }
    }

    /**
     * Download JSON file for a document
     */
    public ResponseEntity<Resource> downloadJsonFile(Long documentId) {
        try {
            DocumentRequis document = documentRequisRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document requis non trouvé"));

            if (document.getLocationFormulaire() == null) {
                throw new RuntimeException("Aucun fichier JSON associé à ce document");
            }

            Resource resource = loadFileAsResource(document.getLocationFormulaire());
            
            // Determine file name for download
            String fileName = document.getNomDocument().replaceAll("[^a-zA-Z0-9]", "_").toLowerCase() + "_form.json";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Erreur lors du téléchargement du fichier JSON pour le document: {}", documentId, e);
            throw new RuntimeException("Erreur lors du téléchargement: " + e.getMessage());
        }
    }

    // Private helper methods for file operations

    /**
     * Save uploaded JSON file to storage
     */
    private String saveJsonFile(MultipartFile jsonFile, String documentName) {
        try {
            // Validate JSON file
            validateJsonFile(jsonFile);

            // Create upload directories
            initializeUploadDirectories();

            // Generate unique filename
            String filename = generateUniqueFilename(documentName);
            
            // Define target path
            Path subDirectory = Paths.get(uploadDir, "documents-requis", "forms");
            Path targetPath = subDirectory.resolve(filename);
            
            // Save file
            Files.copy(jsonFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            String relativePath = "documents-requis/forms/" + filename;
            log.info("Fichier JSON sauvegardé: {}", targetPath);
            
            return relativePath;

        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du fichier JSON", e);
            throw new RuntimeException("Erreur lors de la sauvegarde du fichier JSON: " + e.getMessage());
        }
    }

    /**
     * Load file as Resource
     */
    private Resource loadFileAsResource(String relativePath) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(relativePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Fichier non trouvé: " + relativePath);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Fichier non trouvé: " + relativePath, ex);
        }
    }

    /**
     * Delete a file
     */
    private boolean deleteFile(String relativePath) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(relativePath);
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Fichier supprimé: {}", filePath);
            }
            return deleted;
        } catch (IOException ex) {
            log.error("Erreur lors de la suppression du fichier: {}", relativePath, ex);
            return false;
        }
    }

    /**
     * Validate JSON file (type, size, content)
     */
    private void validateJsonFile(MultipartFile jsonFile) {
        // Check if file is empty
        if (jsonFile.isEmpty()) {
            throw new RuntimeException("Le fichier JSON est vide");
        }

        // Check file size
        if (jsonFile.getSize() > maxFileSize) {
            throw new RuntimeException("Le fichier est trop volumineux (maximum " + formatFileSize(maxFileSize) + ")");
        }

        // Check file extension
        String originalFilename = jsonFile.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".json")) {
            throw new RuntimeException("Seuls les fichiers JSON (.json) sont acceptés");
        }

        // Validate JSON content
        try {
            String content = new String(jsonFile.getBytes());
            ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(content); // This will throw an exception if JSON is invalid
            
            // Additional check for empty JSON
            if (content.trim().isEmpty()) {
                throw new RuntimeException("Le fichier JSON est vide");
            }
            
        } catch (IOException e) {
            throw new RuntimeException("Le contenu du fichier n'est pas un JSON valide: " + e.getMessage());
        }
    }

    /**
     * Initialize upload directories
     */
    private void initializeUploadDirectories() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            Path documentsPath = uploadPath.resolve("documents-requis").resolve("forms");
            Files.createDirectories(documentsPath);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer les répertoires de stockage: " + e.getMessage());
        }
    }

    /**
     * Generate unique filename for JSON files
     */
    private String generateUniqueFilename(String documentName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String sanitizedName = documentName.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        // Add random component to ensure uniqueness
        String randomSuffix = String.valueOf(System.nanoTime() % 10000);
        return sanitizedName + "_" + timestamp + "_" + randomSuffix + ".json";
    }

    /**
     * Format file size for human readability
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    // Mapping and audit helper methods (unchanged)

    private RubriqueWithDocumentsResponse mapToRubriqueWithDocuments(Rubrique rubrique) {
        List<SousRubriqueWithDocumentsDTO> sousRubriques = rubrique.getSousRubriques().stream()
                .map(sr -> {
                    List<DocumentRequis> documents = documentRequisRepository.findBySousRubriqueId(sr.getId());
                    List<DocumentRequisResponse> documentResponses = documents.stream()
                            .map(this::mapToDocumentRequisResponse)
                            .collect(Collectors.toList());

                    return SousRubriqueWithDocumentsDTO.builder()
                            .id(sr.getId())
                            .designation(sr.getDesignation())
                            .description(sr.getDescription())
                            .documentsRequis(documentResponses)
                            .build();
                })
                .collect(Collectors.toList());

        return RubriqueWithDocumentsResponse.builder()
                .id(rubrique.getId())
                .designation(rubrique.getDesignation())
                .description(rubrique.getDescription())
                .sousRubriques(sousRubriques)
                .build();
    }

    private DocumentRequisResponse mapToDocumentRequisResponse(DocumentRequis document) {
        return DocumentRequisResponse.builder()
                .id(document.getId())
                .nomDocument(document.getNomDocument())
                .description(document.getDescription())
                .obligatoire(document.getObligatoire())
                .locationFormulaire(document.getLocationFormulaire())
                .sousRubriqueId(document.getSousRubrique().getId())
                .sousRubriqueDesignation(document.getSousRubrique().getDesignation())
                .rubriqueDesignation(document.getSousRubrique().getRubrique().getDesignation())
                .build();
    }

    private void createAuditTrail(String action, Long entityId, String description, String userEmail) {
        createAuditTrailWithValues(action, entityId, description, null, null, userEmail);
    }

    private void createAuditTrailWithValues(String action, Long entityId, String description, 
                                         String oldValues, String newValues, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail).orElse(null);

            AuditTrail auditTrail = new AuditTrail();
            auditTrail.setAction(action);
            auditTrail.setEntite("DocumentRequis");
            auditTrail.setEntiteId(entityId);
            auditTrail.setValeurAvant(oldValues);
            auditTrail.setValeurApres(newValues);
            auditTrail.setDateAction(LocalDateTime.now());
            auditTrail.setUtilisateur(utilisateur);
            auditTrail.setDescription(description);

            auditTrailRepository.save(auditTrail);
        } catch (Exception e) {
            log.warn("Erreur lors de la création de l'audit trail", e);
        }
    }
}