package ormvat.sadsa.service.agent_antenne;

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentFillingService {

    private final DossierRepository dossierRepository;
    private final DocumentRequisRepository documentRequisRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final UtilisateurRepository utilisateurRepository;
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
     * Get dossier with its documents requis and pieces jointes
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

            DossierSummaryDTO dossierSummary = buildDossierSummary(dossier, utilisateur);
            List<DocumentWithPiecesDTO> documentsWithPieces = documentsRequis.stream()
                    .map(doc -> buildDocumentWithPieces(doc, dossierId))
                    .collect(Collectors.toList());
            DocumentStatisticsDTO statistics = calculateStatistics(documentsWithPieces);

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
                    .documentsRequis(documentsWithPieces)
                    .statistics(statistics)
                    .navigationInfo(navigationInfo)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des documents du dossier: {}", dossierId, e);
            throw new RuntimeException("Erreur lors du chargement des documents: " + e.getMessage());
        }
    }

    /**
     * Upload a scanned document file
     */
    @Transactional
    public UploadDocumentResponse uploadScannedDocument(Long dossierId, Long documentRequisId, 
                                                       MultipartFile file, String title, 
                                                       String description, String userEmail) {
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

            String filePath = storeFile(file, dossierId, "documents");

            PieceJointe pieceJointe = new PieceJointe();
            pieceJointe.setDossier(dossier);
            pieceJointe.setDocumentRequis(documentRequis);
            pieceJointe.setUtilisateur(utilisateur);
            pieceJointe.setDocumentType(PieceJointe.DocumentType.SCANNED_DOCUMENT);
            pieceJointe.setNomFichier(file.getOriginalFilename());
            pieceJointe.setCheminFichier(filePath);
            pieceJointe.setTitle(title);
            pieceJointe.setDescription(description);
            pieceJointe.setStatus(PieceJointe.DocumentStatus.COMPLETE);
            pieceJointe.setDateUpload(LocalDateTime.now());
            pieceJointe.setIsRequired(documentRequis.getObligatoire());

            PieceJointe savedPieceJointe = pieceJointeRepository.save(pieceJointe);

            log.info("Document scanné uploadé avec succès - Dossier: {}, Fichier: {}, Utilisateur: {}", 
                    dossierId, file.getOriginalFilename(), userEmail);

            PieceJointeDTO pieceDTO = mapToPieceJointeDTO(savedPieceJointe);
            DocumentProgressDTO progress = calculateDocumentProgress(documentRequisId, dossierId);

            return UploadDocumentResponse.builder()
                    .success(true)
                    .message("Document uploadé avec succès")
                    .uploadedPiece(pieceDTO)
                    .updatedProgress(progress)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'upload du document", e);
            throw new RuntimeException("Erreur lors de l'upload: " + e.getMessage());
        }
    }

    /**
     * Save form data directly as JSON
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

            // Check if form data already exists for this document
            Optional<PieceJointe> existingPiece = pieceJointeRepository
                    .findExistingFormData(request.getDossierId(), request.getDocumentRequisId());

            PieceJointe pieceJointe;
            boolean isUpdate = false;

            if (existingPiece.isPresent()) {
                // Update existing form data
                pieceJointe = existingPiece.get();
                pieceJointe.setFormDataFromMap(request.getFormData());
                if (request.getTitle() != null) {
                    pieceJointe.setTitle(request.getTitle());
                }
                if (request.getDescription() != null) {
                    pieceJointe.setDescription(request.getDescription());
                }
                pieceJointe.setStatus(PieceJointe.DocumentStatus.COMPLETE);
                isUpdate = true;
                log.info("Updated existing form data for dossier {} document {}", 
                        request.getDossierId(), request.getDocumentRequisId());
            } else {
                // Create new form data entry
                pieceJointe = new PieceJointe();
                pieceJointe.setDossier(dossier);
                pieceJointe.setDocumentRequis(documentRequis);
                pieceJointe.setUtilisateur(utilisateur);
                pieceJointe.setDocumentType(PieceJointe.DocumentType.FORM_DATA);
                pieceJointe.setFormDataFromMap(request.getFormData());
                pieceJointe.setTitle(request.getTitle() != null ? request.getTitle() : "Données formulaire - " + documentRequis.getNomDocument());
                pieceJointe.setDescription(request.getDescription() != null ? request.getDescription() : "Données saisies par l'agent");
                pieceJointe.setStatus(PieceJointe.DocumentStatus.COMPLETE);
                pieceJointe.setDateUpload(LocalDateTime.now());
                pieceJointe.setIsRequired(documentRequis.getObligatoire());
                log.info("Created new form data for dossier {} document {}", 
                        request.getDossierId(), request.getDocumentRequisId());
            }

            PieceJointe savedPieceJointe = pieceJointeRepository.save(pieceJointe);

            PieceJointeDTO pieceDTO = mapToPieceJointeDTO(savedPieceJointe);
            DocumentProgressDTO progress = calculateDocumentProgress(request.getDocumentRequisId(), request.getDossierId());

            return SaveFormDataResponse.builder()
                    .success(true)
                    .message(isUpdate ? "Données du formulaire mises à jour avec succès" : "Données du formulaire sauvegardées avec succès")
                    .savedPiece(pieceDTO)
                    .updatedProgress(progress)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde des données du formulaire", e);
            throw new RuntimeException("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    /**
     * Save mixed document (file + form data)
     */
    @Transactional
    public UploadDocumentResponse saveMixedDocument(Long dossierId, Long documentRequisId,
                                                   MultipartFile file, SaveMixedDocumentRequest request, 
                                                   String userEmail) {
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

            String filePath = storeFile(file, dossierId, "documents");

            PieceJointe pieceJointe = new PieceJointe();
            pieceJointe.setDossier(dossier);
            pieceJointe.setDocumentRequis(documentRequis);
            pieceJointe.setUtilisateur(utilisateur);
            pieceJointe.setDocumentType(PieceJointe.DocumentType.MIXED);
            pieceJointe.setNomFichier(file.getOriginalFilename());
            pieceJointe.setCheminFichier(filePath);
            pieceJointe.setFormDataFromMap(request.getFormData());
            pieceJointe.setTitle(request.getTitle());
            pieceJointe.setDescription(request.getDescription());
            pieceJointe.setStatus(PieceJointe.DocumentStatus.COMPLETE);
            pieceJointe.setDateUpload(LocalDateTime.now());
            pieceJointe.setIsRequired(documentRequis.getObligatoire());

            PieceJointe savedPieceJointe = pieceJointeRepository.save(pieceJointe);

            log.info("Document mixte sauvegardé avec succès - Dossier: {}, Fichier: {}, Utilisateur: {}", 
                    dossierId, file.getOriginalFilename(), userEmail);

            PieceJointeDTO pieceDTO = mapToPieceJointeDTO(savedPieceJointe);
            DocumentProgressDTO progress = calculateDocumentProgress(documentRequisId, dossierId);

            return UploadDocumentResponse.builder()
                    .success(true)
                    .message("Document mixte sauvegardé avec succès")
                    .uploadedPiece(pieceDTO)
                    .updatedProgress(progress)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du document mixte", e);
            throw new RuntimeException("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    /**
     * Delete a piece jointe
     */
    @Transactional
    public DeleteDocumentResponse deleteDocument(Long pieceJointeId, String userEmail) {
        try {
            PieceJointe pieceJointe = pieceJointeRepository.findById(pieceJointeId)
                    .orElseThrow(() -> new RuntimeException("Document non trouvé"));

            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!canModifyDossier(pieceJointe.getDossier(), utilisateur)) {
                throw new RuntimeException("Accès non autorisé");
            }

            Long documentRequisId = pieceJointe.getDocumentRequis() != null ? 
                    pieceJointe.getDocumentRequis().getId() : null;
            Long dossierId = pieceJointe.getDossier().getId();
            String fileName = pieceJointe.getNomFichier();
            String filePath = pieceJointe.getCheminFichier();

            // Delete physical file if exists
            if (filePath != null) {
                deleteFile(filePath);
            }

            pieceJointeRepository.delete(pieceJointe);

            log.info("Document supprimé - ID: {}, Fichier: {}, Utilisateur: {}", 
                    pieceJointeId, fileName, userEmail);

            DocumentProgressDTO progress = documentRequisId != null ? 
                    calculateDocumentProgress(documentRequisId, dossierId) : null;

            return DeleteDocumentResponse.builder()
                    .success(true)
                    .message("Document supprimé avec succès")
                    .updatedProgress(progress)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la suppression du document", e);
            throw new RuntimeException("Erreur lors de la suppression: " + e.getMessage());
        }
    }

    /**
     * Download a document
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

    // Helper methods
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

    private DossierSummaryDTO buildDossierSummary(Dossier dossier, Utilisateur utilisateur) {
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

    private DocumentWithPiecesDTO buildDocumentWithPieces(DocumentRequis documentRequis, Long dossierId) {
        List<PieceJointe> pieces = pieceJointeRepository
                .findByDossierIdAndDocumentRequisId(dossierId, documentRequis.getId());

        List<PieceJointeDTO> pieceDTOs = pieces.stream()
                .map(this::mapToPieceJointeDTO)
                .collect(Collectors.toList());

        Map<String, Object> formStructure = parseFormStructure(documentRequis.getLocationFormulaire());
        String status = determineDocumentStatus(documentRequis, pieces);
        DocumentProgressDTO progress = calculateDocumentProgress(documentRequis.getId(), dossierId);

        return DocumentWithPiecesDTO.builder()
                .documentRequisId(documentRequis.getId())
                .nomDocument(documentRequis.getNomDocument())
                .description(documentRequis.getDescription())
                .obligatoire(documentRequis.getObligatoire())
                .locationFormulaire(documentRequis.getLocationFormulaire())
                .pieces(pieceDTOs)
                .status(status)
                .progress(progress)
                .formStructure(formStructure)
                .build();
    }

    private PieceJointeDTO mapToPieceJointeDTO(PieceJointe pieceJointe) {
        return PieceJointeDTO.builder()
                .id(pieceJointe.getId())
                .nomFichier(pieceJointe.getNomFichier())
                .cheminFichier(pieceJointe.getCheminFichier())
                .documentType(pieceJointe.getDocumentType())
                .title(pieceJointe.getTitle())
                .description(pieceJointe.getDescription())
                .status(pieceJointe.getStatus())
                .isRequired(pieceJointe.getIsRequired())
                .dateUpload(pieceJointe.getDateUpload())
                .lastEdited(pieceJointe.getLastEdited())
                .utilisateurNom(pieceJointe.getUtilisateur().getNom() + " " + pieceJointe.getUtilisateur().getPrenom())
                .formData(pieceJointe.getFormDataAsMap())
                .hasFile(pieceJointe.hasFile())
                .hasFormData(pieceJointe.hasFormData())
                .canEdit(true) // Will be determined by business logic
                .canDelete(true)
                .canDownload(pieceJointe.hasFile())
                .build();
    }

    private Map<String, Object> parseFormStructure(String jsonFilePath) {
        if (jsonFilePath == null || jsonFilePath.isEmpty()) {
            return new HashMap<>();
        }
        try {
            String jsonContent = Files.readString(Paths.get(getStorageLocation().toString(), jsonFilePath));
            return objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Erreur lors du parsing de la structure de formulaire: {}", jsonFilePath, e);
            return new HashMap<>();
        }
    }

    private String determineDocumentStatus(DocumentRequis documentRequis, List<PieceJointe> pieces) {
        boolean hasCompletePieces = pieces.stream()
                .anyMatch(p -> p.getStatus() == PieceJointe.DocumentStatus.COMPLETE);
        
        if (documentRequis.getObligatoire()) {
            return hasCompletePieces ? "COMPLETE" : "MISSING";
        } else {
            return hasCompletePieces ? "COMPLETE" : "OPTIONAL";
        }
    }

    private DocumentProgressDTO calculateDocumentProgress(Long documentRequisId, Long dossierId) {
        List<PieceJointe> pieces = pieceJointeRepository
                .findByDossierIdAndDocumentRequisId(dossierId, documentRequisId);

        DocumentRequis documentRequis = documentRequisRepository.findById(documentRequisId).orElse(null);
        if (documentRequis == null) {
            return DocumentProgressDTO.builder().build();
        }

        boolean hasFiles = pieces.stream().anyMatch(PieceJointe::hasFile);
        boolean hasFormData = pieces.stream().anyMatch(PieceJointe::hasFormData);
        boolean isComplete = pieces.stream().anyMatch(p -> p.getStatus() == PieceJointe.DocumentStatus.COMPLETE);

        List<String> completedElements = new ArrayList<>();
        List<String> missingElements = new ArrayList<>();

        if (hasFiles) {
            completedElements.add("Documents téléchargés");
        }
        if (hasFormData) {
            completedElements.add("Données formulaire saisies");
        }

        if (documentRequis.getObligatoire() && !isComplete) {
            missingElements.add("Document ou données requis");
        }

        double completionPercentage = isComplete ? 100.0 : (hasFiles || hasFormData ? 50.0 : 0.0);

        return DocumentProgressDTO.builder()
                .isRequired(documentRequis.getObligatoire())
                .isComplete(isComplete)
                .hasFiles(hasFiles)
                .hasFormData(hasFormData)
                .completionPercentage(completionPercentage)
                .nextStep(isComplete ? "Terminé" : "Télécharger document ou remplir formulaire")
                .missingElements(missingElements)
                .completedElements(completedElements)
                .build();
    }

    private DocumentStatisticsDTO calculateStatistics(List<DocumentWithPiecesDTO> documents) {
        int total = documents.size();
        int completed = (int) documents.stream().filter(doc -> "COMPLETE".equals(doc.getStatus())).count();
        int missing = (int) documents.stream().filter(doc -> "MISSING".equals(doc.getStatus())).count();
        int optional = (int) documents.stream().filter(doc -> !doc.getObligatoire()).count();
        
        double percentage = total > 0 ? (double) completed / total * 100 : 0;

        int totalPieces = documents.stream().mapToInt(doc -> doc.getPieces().size()).sum();
        int piecesWithFiles = documents.stream()
                .mapToInt(doc -> (int) doc.getPieces().stream().filter(PieceJointeDTO::getHasFile).count())
                .sum();
        int piecesWithFormData = documents.stream()
                .mapToInt(doc -> (int) doc.getPieces().stream().filter(PieceJointeDTO::getHasFormData).count())
                .sum();

        return DocumentStatisticsDTO.builder()
                .totalDocuments(total)
                .documentsCompletes(completed)
                .documentsManquants(missing)
                .documentsOptionnels(optional)
                .pourcentageCompletion(percentage)
                .totalPieces(totalPieces)
                .piecesWithFiles(piecesWithFiles)
                .piecesWithFormData(piecesWithFormData)
                .build();
    }

    // File handling methods
    private String storeFile(MultipartFile file, Long dossierId, String subFolder) {
        try {
            String fileName = generateUniqueFileName(file.getOriginalFilename());
            String subDirectory = "dossiers/dossier_" + dossierId + "/" + subFolder;
            
            Path targetLocation = getStorageLocation().resolve(subDirectory);
            Files.createDirectories(targetLocation);
            
            Path filePath = targetLocation.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            return subDirectory + "/" + fileName;
            
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + file.getOriginalFilename(), ex);
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
}