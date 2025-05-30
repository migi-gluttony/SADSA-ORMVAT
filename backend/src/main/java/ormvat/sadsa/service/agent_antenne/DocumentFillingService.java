package ormvat.sadsa.service.agent_antenne;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ormvat.sadsa.dto.agent_antenne.DocumentFillingDTOs.*;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;
import ormvat.sadsa.service.FileStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentFillingService {

    private final DossierRepository dossierRepository;
    private final DocumentRequisRepository documentRequisRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditTrailRepository auditTrailRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    /**
     * Get dossier with its documents requis and uploaded files
     */
    public DossierDocumentsResponse getDossierDocuments(Long dossierId, String userEmail) {
        try {
            // Get dossier
            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Verify user access (agent can only access dossiers from their antenne)
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            if (!dossier.getAntenne().getId().equals(utilisateur.getAntenne().getId()) && 
                !utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
                throw new RuntimeException("Accès non autorisé à ce dossier");
            }

            // Get documents requis for this sous-rubrique
            List<DocumentRequis> documentsRequis = documentRequisRepository
                    .findBySousRubriqueId(dossier.getSousRubrique().getId());

            // Build response
            DossierSummaryDTO dossierSummary = buildDossierSummary(dossier);
            List<DocumentRequisWithFilesDTO> documentsWithFiles = documentsRequis.stream()
                    .map(doc -> buildDocumentWithFiles(doc, dossierId))
                    .collect(Collectors.toList());
            DocumentStatisticsDTO statistics = calculateStatistics(documentsWithFiles);

            return DossierDocumentsResponse.builder()
                    .dossier(dossierSummary)
                    .documentsRequis(documentsWithFiles)
                    .statistics(statistics)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des documents du dossier: {}", dossierId, e);
            throw new RuntimeException("Erreur lors du chargement des documents: " + e.getMessage());
        }
    }

    /**
     * Upload a file for a specific document requis
     */
    @Transactional
    public UploadDocumentResponse uploadDocument(Long dossierId, Long documentRequisId, 
                                               MultipartFile file, String userEmail) {
        try {
            // Validate dossier and document requis
            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));
            
            DocumentRequis documentRequis = documentRequisRepository.findById(documentRequisId)
                    .orElseThrow(() -> new RuntimeException("Document requis non trouvé"));

            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Verify access
            if (!dossier.getAntenne().getId().equals(utilisateur.getAntenne().getId()) && 
                !utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
                throw new RuntimeException("Accès non autorisé");
            }

            // Create subdirectory for this dossier
            String subDirectory = fileStorageService.createDossierDirectory(dossierId);
            
            // Store the file
            String filePath = fileStorageService.storeFile(file, subDirectory);

            // Create PieceJointe record
            PieceJointe pieceJointe = new PieceJointe();
            pieceJointe.setNomFichier(file.getOriginalFilename());
            pieceJointe.setCheminFichier(filePath);
            pieceJointe.setTypeDocument(documentRequis.getNomDocument());
            pieceJointe.setStatus(PieceJointe.DocumentStatus.COMPLETE);
            pieceJointe.setDateUpload(LocalDateTime.now());
            pieceJointe.setDossier(dossier);
            pieceJointe.setDocumentRequis(documentRequis);
            pieceJointe.setUtilisateur(utilisateur);

            PieceJointe savedPieceJointe = pieceJointeRepository.save(pieceJointe);

            // Create audit trail
            createAuditTrail("UPLOAD_DOCUMENT", dossierId, 
                    "Upload document: " + file.getOriginalFilename() + " pour " + documentRequis.getNomDocument(), 
                    userEmail);

            log.info("Document uploadé avec succès - Dossier: {}, Fichier: {}, Utilisateur: {}", 
                    dossierId, file.getOriginalFilename(), userEmail);

            return UploadDocumentResponse.builder()
                    .pieceJointeId(savedPieceJointe.getId())
                    .nomFichier(savedPieceJointe.getNomFichier())
                    .cheminFichier(savedPieceJointe.getCheminFichier())
                    .success(true)
                    .message("Document uploadé avec succès")
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'upload du document", e);
            throw new RuntimeException("Erreur lors de l'upload: " + e.getMessage());
        }
    }

    /**
     * Save form data for a document requis
     */
    @Transactional
    public SaveFormDataResponse saveFormData(SaveFormDataRequest request, String userEmail) {
        try {
            // Validate dossier and document requis
            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));
            
            DocumentRequis documentRequis = documentRequisRepository.findById(request.getDocumentRequisId())
                    .orElseThrow(() -> new RuntimeException("Document requis non trouvé"));

            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Verify access
            if (!dossier.getAntenne().getId().equals(utilisateur.getAntenne().getId()) && 
                !utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
                throw new RuntimeException("Accès non autorisé");
            }

            // Find or create piece jointe for form data storage
            PieceJointe formDataPieceJointe = pieceJointeRepository
                    .findByDossierIdAndDocumentRequisId(request.getDossierId(), request.getDocumentRequisId())
                    .stream()
                    .filter(pj -> pj.getCheminDonneesFormulaire() != null)
                    .findFirst()
                    .orElse(new PieceJointe());

            // Convert form data to JSON and store it
            String formDataJson = objectMapper.writeValueAsString(request.getFormData());
            
            if (formDataPieceJointe.getId() == null) {
                // Create new form data record
                formDataPieceJointe.setNomFichier("form_data_" + documentRequis.getNomDocument() + ".json");
                formDataPieceJointe.setTypeDocument("FORM_DATA");
                formDataPieceJointe.setStatus(PieceJointe.DocumentStatus.COMPLETE);
                formDataPieceJointe.setDateUpload(LocalDateTime.now());
                formDataPieceJointe.setDossier(dossier);
                formDataPieceJointe.setDocumentRequis(documentRequis);
                formDataPieceJointe.setUtilisateur(utilisateur);
            }
            
            formDataPieceJointe.setCheminDonneesFormulaire(formDataJson);
            pieceJointeRepository.save(formDataPieceJointe);

            // Create audit trail
            createAuditTrail("SAVE_FORM_DATA", request.getDossierId(), 
                    "Sauvegarde données formulaire: " + documentRequis.getNomDocument(), userEmail);

            log.info("Données formulaire sauvegardées - Dossier: {}, Document: {}, Utilisateur: {}", 
                    request.getDossierId(), documentRequis.getNomDocument(), userEmail);

            return SaveFormDataResponse.builder()
                    .success(true)
                    .message("Données du formulaire sauvegardées avec succès")
                    .savedData(request.getFormData())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde des données du formulaire", e);
            throw new RuntimeException("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    /**
     * Delete an uploaded document
     */
    @Transactional
    public void deleteDocument(Long pieceJointeId, String userEmail) {
        try {
            PieceJointe pieceJointe = pieceJointeRepository.findById(pieceJointeId)
                    .orElseThrow(() -> new RuntimeException("Document non trouvé"));

            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Verify access
            if (!pieceJointe.getDossier().getAntenne().getId().equals(utilisateur.getAntenne().getId()) && 
                !utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
                throw new RuntimeException("Accès non autorisé");
            }

            String fileName = pieceJointe.getNomFichier();
            String filePath = pieceJointe.getCheminFichier();
            Long dossierId = pieceJointe.getDossier().getId();

            // Delete physical file if it exists
            if (filePath != null) {
                fileStorageService.deleteFile(filePath);
            }

            // Delete database record
            pieceJointeRepository.delete(pieceJointe);

            // Create audit trail
            createAuditTrail("DELETE_DOCUMENT", dossierId, 
                    "Suppression document: " + fileName, userEmail);

            log.info("Document supprimé - ID: {}, Fichier: {}, Utilisateur: {}", 
                    pieceJointeId, fileName, userEmail);

        } catch (Exception e) {
            log.error("Erreur lors de la suppression du document", e);
            throw new RuntimeException("Erreur lors de la suppression: " + e.getMessage());
        }
    }

    /**
     * Download an uploaded document
     */
    public ResponseEntity<Resource> downloadDocument(Long pieceJointeId, String userEmail) {
        try {
            PieceJointe pieceJointe = pieceJointeRepository.findById(pieceJointeId)
                    .orElseThrow(() -> new RuntimeException("Document non trouvé"));

            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Verify access
            if (!pieceJointe.getDossier().getAntenne().getId().equals(utilisateur.getAntenne().getId()) && 
                !utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
                throw new RuntimeException("Accès non autorisé");
            }

            if (pieceJointe.getCheminFichier() == null) {
                throw new RuntimeException("Aucun fichier associé à ce document");
            }

            // Load file as resource
            Resource resource = fileStorageService.loadFileAsResource(pieceJointe.getCheminFichier());
            
            // Determine content type
            String contentType = fileStorageService.getMimeType(pieceJointe.getCheminFichier());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Create audit trail
            createAuditTrail("DOWNLOAD_DOCUMENT", pieceJointe.getDossier().getId(), 
                    "Téléchargement document: " + pieceJointe.getNomFichier(), userEmail);

            log.info("Document téléchargé - ID: {}, Fichier: {}, Utilisateur: {}", 
                    pieceJointeId, pieceJointe.getNomFichier(), userEmail);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + pieceJointe.getNomFichier() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Erreur lors du téléchargement du document", e);
            throw new RuntimeException("Erreur lors du téléchargement: " + e.getMessage());
        }
    }

    // Private helper methods

    private DossierSummaryDTO buildDossierSummary(Dossier dossier) {
        return DossierSummaryDTO.builder()
                .id(dossier.getId())
                .numeroDossier(dossier.getNumeroDossier())
                .saba(dossier.getSaba())
                .reference(dossier.getReference())
                .status(dossier.getStatus().name())
                .dateCreation(dossier.getDateCreation())
                .agriculteurNom(dossier.getAgriculteur().getNom())
                .agriculteurPrenom(dossier.getAgriculteur().getPrenom())
                .agriculteurCin(dossier.getAgriculteur().getCin())
                .agriculteurTelephone(dossier.getAgriculteur().getTelephone())
                .rubriqueDesignation(dossier.getSousRubrique().getRubrique().getDesignation())
                .sousRubriqueDesignation(dossier.getSousRubrique().getDesignation())
                .antenneDesignation(dossier.getAntenne().getDesignation())
                .montantDemande(dossier.getMontantSubvention() != null ? dossier.getMontantSubvention().doubleValue() : null)
                .build();
    }

    private DocumentRequisWithFilesDTO buildDocumentWithFiles(DocumentRequis documentRequis, Long dossierId) {
        // Get uploaded files for this document
        List<PieceJointe> pieceJointes = pieceJointeRepository
                .findByDossierIdAndDocumentRequisId(dossierId, documentRequis.getId());

        List<PieceJointeDTO> fichiers = pieceJointes.stream()
                .filter(pj -> pj.getCheminFichier() != null) // Only actual files, not form data
                .map(this::buildPieceJointeDTO)
                .collect(Collectors.toList());

        // Parse form structure from JSON file if exists
        Map<String, Object> formStructure = parseFormStructure(documentRequis.getLocationFormulaire());

        // Get saved form data if exists
        Map<String, Object> formData = pieceJointes.stream()
                .filter(pj -> pj.getCheminDonneesFormulaire() != null)
                .findFirst()
                .map(pj -> parseFormData(pj.getCheminDonneesFormulaire()))
                .orElse(new HashMap<>());

        // Determine status
        String status = determineDocumentStatus(documentRequis, fichiers, formData);

        return DocumentRequisWithFilesDTO.builder()
                .id(documentRequis.getId())
                .nomDocument(documentRequis.getNomDocument())
                .description(documentRequis.getDescription())
                .obligatoire(documentRequis.getObligatoire())
                .locationFormulaire(documentRequis.getLocationFormulaire())
                .formStructure(formStructure)
                .fichiers(fichiers)
                .formData(formData)
                .status(status)
                .build();
    }

    private PieceJointeDTO buildPieceJointeDTO(PieceJointe pieceJointe) {
        return PieceJointeDTO.builder()
                .id(pieceJointe.getId())
                .nomFichier(pieceJointe.getNomFichier())
                .cheminFichier(pieceJointe.getCheminFichier())
                .typeDocument(pieceJointe.getTypeDocument())
                .status(pieceJointe.getStatus().name())
                .dateUpload(pieceJointe.getDateUpload())
                .utilisateurNom(pieceJointe.getUtilisateur().getNom() + " " + pieceJointe.getUtilisateur().getPrenom())
                .tailleFichier(calculateFileSize(pieceJointe.getCheminFichier()))
                .formatFichier(extractFileExtension(pieceJointe.getNomFichier()))
                .build();
    }

    private Map<String, Object> parseFormStructure(String jsonFilePath) {
        if (jsonFilePath == null || jsonFilePath.isEmpty()) {
            return new HashMap<>();
        }

        try {
            String jsonContent = Files.readString(Paths.get(fileStorageService.getStorageLocation().toString(), jsonFilePath));
            return objectMapper.readValue(jsonContent, Map.class);
        } catch (Exception e) {
            log.warn("Erreur lors du parsing de la structure de formulaire: {}", jsonFilePath, e);
            return new HashMap<>();
        }
    }

    private Map<String, Object> parseFormData(String formDataJson) {
        try {
            return objectMapper.readValue(formDataJson, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("Erreur lors du parsing des données de formulaire", e);
            return new HashMap<>();
        }
    }

    private String determineDocumentStatus(DocumentRequis documentRequis, List<PieceJointeDTO> fichiers, Map<String, Object> formData) {
        boolean hasFiles = !fichiers.isEmpty();
        boolean hasFormData = !formData.isEmpty();
        
        if (documentRequis.getObligatoire()) {
            if (hasFiles || hasFormData) {
                return "COMPLETE";
            } else {
                return "MISSING";
            }
        } else {
            return hasFiles || hasFormData ? "COMPLETE" : "PENDING";
        }
    }

    private DocumentStatisticsDTO calculateStatistics(List<DocumentRequisWithFilesDTO> documents) {
        int total = documents.size();
        int completed = (int) documents.stream().filter(doc -> "COMPLETE".equals(doc.getStatus())).count();
        int missing = (int) documents.stream().filter(doc -> "MISSING".equals(doc.getStatus())).count();
        int optional = (int) documents.stream().filter(doc -> !doc.getObligatoire()).count();
        
        double percentage = total > 0 ? (double) completed / total * 100 : 0;

        return DocumentStatisticsDTO.builder()
                .totalDocuments(total)
                .documentsCompletes(completed)
                .documentsManquants(missing)
                .documentsOptionnels(optional)
                .pourcentageCompletion(percentage)
                .build();
    }

    private Long calculateFileSize(String filePath) {
        if (filePath == null) return 0L;
        try {
            return Files.size(Paths.get(fileStorageService.getStorageLocation().toString(), filePath));
        } catch (IOException e) {
            return 0L;
        }
    }

    private String extractFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private void createAuditTrail(String action, Long dossierId, String description, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail).orElse(null);
            
            AuditTrail auditTrail = new AuditTrail();
            auditTrail.setAction(action);
            auditTrail.setEntite("Dossier");
            auditTrail.setEntiteId(dossierId);
            auditTrail.setDateAction(LocalDateTime.now());
            auditTrail.setUtilisateur(utilisateur);
            auditTrail.setDescription(description);
            
            auditTrailRepository.save(auditTrail);
        } catch (Exception e) {
            log.warn("Erreur lors de la création de l'audit trail", e);
        }
    }
}