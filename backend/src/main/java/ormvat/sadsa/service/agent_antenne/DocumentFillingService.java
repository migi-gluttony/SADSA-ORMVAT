package ormvat.sadsa.service.agent_antenne;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import ormvat.sadsa.dto.agent_antenne.DocumentFillingDTOs.*;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
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
    private final ObjectMapper objectMapper;

    @Value("${app.upload.dir:./uploads/ormvat_sadsa}")
    private String uploadDir;

    private Path getStorageLocation() {
        Path storageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
        return storageLocation;
    }

    /**
     * Get dossier with its documents requis and uploaded files
     */
    public DossierDocumentsResponse getDossierDocuments(Long dossierId, String userEmail) {
        try {
            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            if (!canAccessDossier(dossier, utilisateur)) {
                throw new RuntimeException("Accès non autorisé à ce dossier");
            }

            List<DocumentRequis> documentsRequis = documentRequisRepository
                    .findBySousRubriqueId(dossier.getSousRubrique().getId());

            DossierSummaryDTO dossierSummary = buildDossierSummary(dossier, userEmail);
            List<DocumentRequisWithFilesDTO> documentsWithFiles = documentsRequis.stream()
                    .map(doc -> buildDocumentWithFiles(doc, dossierId))
                    .collect(Collectors.toList());
            DocumentStatisticsDTO statistics = calculateStatistics(documentsWithFiles);

            // Build navigation info
            NavigationInfoDTO navigationInfo = NavigationInfoDTO.builder()
                    .backUrl("/api/dossiers/" + dossierId)
                    .dossierDetailUrl("/api/dossiers/" + dossierId)
                    .dossierManagementUrl("/api/dossiers")
                    .showBackButton(true)
                    .currentStep("Document Filling")
                    .nextStep(canModifyDossier(dossier, utilisateur) ? "Submission" : "Review")
                    .build();

            return DossierDocumentsResponse.builder()
                    .dossier(dossierSummary)
                    .documentsRequis(documentsWithFiles)
                    .statistics(statistics)
                    .navigationInfo(navigationInfo)
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
            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));
            
            DocumentRequis documentRequis = documentRequisRepository.findById(documentRequisId)
                    .orElseThrow(() -> new RuntimeException("Document requis non trouvé"));

            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!canModifyDossier(dossier, utilisateur)) {
                throw new RuntimeException("Accès non autorisé");
            }

            String subDirectory = createDossierDirectory(dossierId);
            String filePath = storeFile(file, subDirectory);

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
     * Save form data for a document requis - Progressive saving (updates existing file)
     */
    @Transactional
    public SaveFormDataResponse saveFormData(SaveFormDataRequest request, String userEmail) {
        try {
            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));
            
            DocumentRequis documentRequis = documentRequisRepository.findById(request.getDocumentRequisId())
                    .orElseThrow(() -> new RuntimeException("Document requis non trouvé"));

            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!canModifyDossier(dossier, utilisateur)) {
                throw new RuntimeException("Accès non autorisé");
            }

            String subDirectory = createDossierDirectory(request.getDossierId());
            
            // Find existing form data piece jointe or create new one
            PieceJointe formDataPieceJointe = findOrCreateFormDataPieceJointe(
                    dossier, documentRequis, utilisateur);

            // Save/Update form data as JSON file (overwrite existing)
            String formDataFilePath = saveOrUpdateFormDataAsJsonFile(
                    request.getFormData(), subDirectory, 
                    documentRequis.getNomDocument(), 
                    request.getDossierId(), 
                    request.getDocumentRequisId(),
                    formDataPieceJointe.getCheminDonneesFormulaire());

            formDataPieceJointe.setCheminDonneesFormulaire(formDataFilePath);
            pieceJointeRepository.save(formDataPieceJointe);

            createAuditTrail("SAVE_FORM_DATA", request.getDossierId(), 
                    "Sauvegarde données formulaire: " + documentRequis.getNomDocument(), userEmail);

            log.info("Données formulaire sauvegardées - Dossier: {}, Document: {}, Fichier: {}, Utilisateur: {}", 
                    request.getDossierId(), documentRequis.getNomDocument(), formDataFilePath, userEmail);

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

            if (!canModifyDossier(pieceJointe.getDossier(), utilisateur)) {
                throw new RuntimeException("Accès non autorisé");
            }

            String fileName = pieceJointe.getNomFichier();
            String filePath = pieceJointe.getCheminFichier();
            String formDataPath = pieceJointe.getCheminDonneesFormulaire();
            Long dossierId = pieceJointe.getDossier().getId();

            if (filePath != null) {
                deleteFile(filePath);
            }
            if (formDataPath != null) {
                deleteFile(formDataPath);
            }

            pieceJointeRepository.delete(pieceJointe);

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

            if (!canAccessDossier(pieceJointe.getDossier(), utilisateur)) {
                throw new RuntimeException("Accès non autorisé");
            }

            if (pieceJointe.getCheminFichier() == null) {
                throw new RuntimeException("Aucun fichier associé à ce document");
            }

            Resource resource = loadFileAsResource(pieceJointe.getCheminFichier());
            
            String contentType = getMimeType(pieceJointe.getCheminFichier());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

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

    // Private helper methods for access control
    private boolean canAccessDossier(Dossier dossier, Utilisateur utilisateur) {
        switch (utilisateur.getRole()) {
            case ADMIN:
                return true;
            case AGENT_ANTENNE:
                return utilisateur.getAntenne() != null &&
                        dossier.getAntenne().getId().equals(utilisateur.getAntenne().getId());
            case AGENT_GUC:
                return !dossier.getStatus().equals(Dossier.DossierStatus.DRAFT);
            case AGENT_COMMISSION_TERRAIN:
                return dossier.getStatus().equals(Dossier.DossierStatus.SUBMITTED) ||
                        dossier.getStatus().equals(Dossier.DossierStatus.IN_REVIEW) ||
                        dossier.getStatus().equals(Dossier.DossierStatus.APPROVED) ||
                        dossier.getStatus().equals(Dossier.DossierStatus.REJECTED);
            default:
                return false;
        }
    }

    private boolean canModifyDossier(Dossier dossier, Utilisateur utilisateur) {
        if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_ANTENNE)) {
            return false;
        }
        
        if (!dossier.getAntenne().getId().equals(utilisateur.getAntenne().getId())) {
            return false;
        }
        
        return dossier.getStatus() == Dossier.DossierStatus.DRAFT ||
               dossier.getStatus() == Dossier.DossierStatus.RETURNED_FOR_COMPLETION;
    }

    private PieceJointe findOrCreateFormDataPieceJointe(Dossier dossier, DocumentRequis documentRequis, Utilisateur utilisateur) {
        return pieceJointeRepository
                .findByDossierIdAndDocumentRequisId(dossier.getId(), documentRequis.getId())
                .stream()
                .filter(pj -> pj.getCheminDonneesFormulaire() != null)
                .findFirst()
                .orElseGet(() -> {
                    PieceJointe newPieceJointe = new PieceJointe();
                    newPieceJointe.setNomFichier("form_data_" + documentRequis.getNomDocument() + ".json");
                    newPieceJointe.setTypeDocument("FORM_DATA");
                    newPieceJointe.setStatus(PieceJointe.DocumentStatus.COMPLETE);
                    newPieceJointe.setDateUpload(LocalDateTime.now());
                    newPieceJointe.setDossier(dossier);
                    newPieceJointe.setDocumentRequis(documentRequis);
                    newPieceJointe.setUtilisateur(utilisateur);
                    return pieceJointeRepository.save(newPieceJointe);
                });
    }

    private String saveOrUpdateFormDataAsJsonFile(Map<String, Object> formData, String subDirectory, 
                                        String documentName, Long dossierId, Long documentRequisId,
                                        String existingFilePath) {
        try {
            String fileName;
            
            // If existing file exists, use the same name, otherwise create new
            if (existingFilePath != null) {
                Path existingPath = Paths.get(existingFilePath);
                fileName = existingPath.getFileName().toString();
                
                // Delete old file first
                Path fullExistingPath = getStorageLocation().resolve(existingFilePath);
                Files.deleteIfExists(fullExistingPath);
            } else {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                fileName = "form_data_" + documentName.replaceAll("[^a-zA-Z0-9]", "_") + 
                        "_" + dossierId + "_" + documentRequisId + "_" + timestamp + ".json";
            }
            
            Path targetLocation = getStorageLocation().resolve(subDirectory);
            Files.createDirectories(targetLocation);
            
            String jsonContent = objectMapper.writeValueAsString(formData);
            
            Path filePath = targetLocation.resolve(fileName);
            Files.write(filePath, jsonContent.getBytes());
            
            return subDirectory + "/" + fileName;
            
        } catch (IOException ex) {
            throw new RuntimeException("Could not save form data as JSON file. Please try again!", ex);
        }
    }

    // File storage methods
    private String storeFile(MultipartFile file, String subDirectory) {
        try {
            String fileName = generateUniqueFileName(file.getOriginalFilename());
            
            Path targetLocation = getStorageLocation().resolve(subDirectory);
            Files.createDirectories(targetLocation);
            
            Path filePath = targetLocation.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            return subDirectory + "/" + fileName;
            
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + file.getOriginalFilename() + ". Please try again!", ex);
        }
    }

    private String createDossierDirectory(Long dossierId) {
        String subDirectory = "dossiers/dossier_" + dossierId;
        try {
            Path targetLocation = getStorageLocation().resolve(subDirectory);
            Files.createDirectories(targetLocation);
            return subDirectory;
        } catch (IOException ex) {
            throw new RuntimeException("Could not create dossier directory for dossier " + dossierId, ex);
        }
    }

    private Resource loadFileAsResource(String filePath) {
        try {
            Path file = getStorageLocation().resolve(filePath).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + filePath);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + filePath, ex);
        }
    }

    private void deleteFile(String filePath) {
        try {
            Path file = getStorageLocation().resolve(filePath).normalize();
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            log.warn("Could not delete file " + filePath, ex);
        }
    }

    private String getMimeType(String filePath) {
        try {
            Path file = getStorageLocation().resolve(filePath).normalize();
            return Files.probeContentType(file);
        } catch (IOException ex) {
            return null;
        }
    }

    private String generateUniqueFileName(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String randomSuffix = String.valueOf((int) (Math.random() * 10000));
        
        if (originalFilename != null && originalFilename.contains(".")) {
            String nameWithoutExtension = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            return nameWithoutExtension + "_" + timestamp + "_" + randomSuffix + extension;
        } else {
            return "file_" + timestamp + "_" + randomSuffix;
        }
    }

    // Build response methods
    private DossierSummaryDTO buildDossierSummary(Dossier dossier, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            boolean canEdit = canModifyDossier(dossier, utilisateur);
            boolean canSubmit = canEdit && dossier.getStatus() == Dossier.DossierStatus.DRAFT;
            
            String statusMessage = getStatusMessage(dossier.getStatus());
            String nextAction = getNextAction(dossier.getStatus(), canEdit);

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
                    .canEdit(canEdit)
                    .canSubmit(canSubmit)
                    .statusMessage(statusMessage)
                    .nextAction(nextAction)
                    .build();
        } catch (Exception e) {
            log.warn("Erreur lors de la construction du résumé du dossier", e);
            // Return basic info without user-specific data
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
                    .canEdit(false)
                    .canSubmit(false)
                    .statusMessage("Consultation uniquement")
                    .nextAction("Aucune action disponible")
                    .build();
        }
    }

    private String getStatusMessage(Dossier.DossierStatus status) {
        switch (status) {
            case DRAFT:
                return "Dossier en cours de création - Remplissez les documents requis";
            case SUBMITTED:
                return "Dossier soumis au GUC - En attente de traitement";
            case IN_REVIEW:
                return "Dossier en cours d'examen";
            case RETURNED_FOR_COMPLETION:
                return "Dossier retourné - Compléments requis";
            case APPROVED:
                return "Dossier approuvé";
            case REJECTED:
                return "Dossier rejeté";
            default:
                return "Statut: " + status.name();
        }
    }

    private String getNextAction(Dossier.DossierStatus status, boolean canEdit) {
        if (!canEdit) {
            return "Consultation uniquement";
        }
        
        switch (status) {
            case DRAFT:
                return "Remplir les documents et soumettre";
            case RETURNED_FOR_COMPLETION:
                return "Compléter les documents manquants";
            default:
                return "Aucune action disponible";
        }
    }

    private DocumentRequisWithFilesDTO buildDocumentWithFiles(DocumentRequis documentRequis, Long dossierId) {
        List<PieceJointe> pieceJointes = pieceJointeRepository
                .findByDossierIdAndDocumentRequisId(dossierId, documentRequis.getId());

        // Group files by type
        List<PieceJointeGroupDTO> fichierGroups = new ArrayList<>();
        
        // Group 1: Uploaded files
        List<PieceJointeDTO> uploadedFiles = pieceJointes.stream()
                .filter(pj -> pj.getCheminFichier() != null)
                .map(this::buildPieceJointeDTO)
                .collect(Collectors.toList());
        
        if (!uploadedFiles.isEmpty()) {
            PieceJointeGroupDTO filesGroup = PieceJointeGroupDTO.builder()
                    .groupName("Documents téléchargés")
                    .groupType("FILES")
                    .files(uploadedFiles)
                    .totalFiles(uploadedFiles.size())
                    .totalSize(uploadedFiles.stream().mapToLong(f -> f.getTailleFichier() != null ? f.getTailleFichier() : 0L).sum())
                    .lastUpdated(uploadedFiles.stream().map(PieceJointeDTO::getDateUpload).max(LocalDateTime::compareTo).orElse(null))
                    .isComplete(true)
                    .groupStatus("COMPLETE")
                    .build();
            fichierGroups.add(filesGroup);
        }

        // Group 2: Form data
        Map<String, Object> formData = pieceJointes.stream()
                .filter(pj -> pj.getCheminDonneesFormulaire() != null)
                .findFirst()
                .map(pj -> parseFormDataFromFile(pj.getCheminDonneesFormulaire()))
                .orElse(new HashMap<>());
        
        if (!formData.isEmpty()) {
            PieceJointeDTO formDataFile = PieceJointeDTO.builder()
                    .id(-1L) // Virtual ID for form data
                    .nomFichier("Données du formulaire")
                    .typeDocument("FORM_DATA")
                    .status("COMPLETE")
                    .dateUpload(pieceJointes.stream()
                            .filter(pj -> pj.getCheminDonneesFormulaire() != null)
                            .findFirst()
                            .map(PieceJointe::getDateUpload)
                            .orElse(LocalDateTime.now()))
                    .utilisateurNom("Système")
                    .tailleFichier(0L)
                    .formatFichier("JSON")
                    .displayName("Formulaire rempli")
                    .canDelete(false)
                    .canDownload(false)
                    .build();
            
            PieceJointeGroupDTO formGroup = PieceJointeGroupDTO.builder()
                    .groupName("Données de formulaire")
                    .groupType("FORM_DATA")
                    .files(List.of(formDataFile))
                    .totalFiles(1)
                    .totalSize(0L)
                    .lastUpdated(formDataFile.getDateUpload())
                    .isComplete(true)
                    .groupStatus("COMPLETE")
                    .build();
            fichierGroups.add(formGroup);
        }

        Map<String, Object> formStructure = parseFormStructure(documentRequis.getLocationFormulaire());
        String status = determineDocumentStatus(documentRequis, uploadedFiles, formData);

        // Create progress info
        DocumentProgressDTO progress = DocumentProgressDTO.builder()
                .isRequired(documentRequis.getObligatoire())
                .isComplete("COMPLETE".equals(status))
                .hasFiles(!uploadedFiles.isEmpty())
                .hasFormData(!formData.isEmpty())
                .completionPercentage("COMPLETE".equals(status) ? 100.0 : 0.0)
                .nextStep(status.equals("COMPLETE") ? "Completed" : "Upload documents or fill form")
                .missingElements(status.equals("MISSING") ? List.of("Documents ou formulaire requis") : List.of())
                .completedElements(status.equals("COMPLETE") ? List.of("Documents téléchargés ou formulaire rempli") : List.of())
                .build();

        return DocumentRequisWithFilesDTO.builder()
                .id(documentRequis.getId())
                .nomDocument(documentRequis.getNomDocument())
                .description(documentRequis.getDescription())
                .obligatoire(documentRequis.getObligatoire())
                .locationFormulaire(documentRequis.getLocationFormulaire())
                .formStructure(formStructure)
                .fichierGroups(fichierGroups)
                .formData(formData)
                .status(status)
                .progress(progress)
                .documentCategory("REQUIRED")
                .order(1)
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
                .displayName(pieceJointe.getNomFichier())
                .description("Document téléchargé")
                .canDelete(true) // Will be determined by business logic
                .canDownload(true)
                .thumbnailUrl(null) // To be implemented for images
                .build();
    }

    private Map<String, Object> parseFormStructure(String jsonFilePath) {
        if (jsonFilePath == null || jsonFilePath.isEmpty()) {
            return new HashMap<>();
        }

        try {
            String jsonContent = Files.readString(Paths.get(getStorageLocation().toString(), jsonFilePath));
            return objectMapper.readValue(jsonContent, Map.class);
        } catch (Exception e) {
            log.warn("Erreur lors du parsing de la structure de formulaire: {}", jsonFilePath, e);
            return new HashMap<>();
        }
    }

    private Map<String, Object> parseFormDataFromFile(String formDataFilePath) {
        try {
            String jsonContent = Files.readString(Paths.get(getStorageLocation().toString(), formDataFilePath));
            return objectMapper.readValue(jsonContent, Map.class);
        } catch (Exception e) {
            log.warn("Erreur lors du parsing des données de formulaire depuis le fichier: {}", formDataFilePath, e);
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

        // Calculate file statistics
        int totalFiles = documents.stream()
                .mapToInt(doc -> doc.getFichierGroups().stream()
                        .mapToInt(group -> group.getTotalFiles() != null ? group.getTotalFiles() : 0)
                        .sum())
                .sum();

        long totalFileSize = documents.stream()
                .mapToLong(doc -> doc.getFichierGroups().stream()
                        .mapToLong(group -> group.getTotalSize() != null ? group.getTotalSize() : 0L)
                        .sum())
                .sum();

        int formsCompleted = (int) documents.stream()
                .filter(doc -> doc.getFormData() != null && !doc.getFormData().isEmpty())
                .count();

        int formsTotal = documents.size(); // All documents can potentially have forms

        return DocumentStatisticsDTO.builder()
                .totalDocuments(total)
                .documentsCompletes(completed)
                .documentsManquants(missing)
                .documentsOptionnels(optional)
                .pourcentageCompletion(percentage)
                .totalFiles(totalFiles)
                .totalFileSize(totalFileSize)
                .formsCompleted(formsCompleted)
                .formsTotal(formsTotal)
                .build();
    }

    private Long calculateFileSize(String filePath) {
        if (filePath == null) return 0L;
        try {
            return Files.size(Paths.get(getStorageLocation().toString(), filePath));
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