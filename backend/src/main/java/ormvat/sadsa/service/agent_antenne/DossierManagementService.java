package ormvat.sadsa.service.agent_antenne;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ormvat.sadsa.dto.agent_antenne.DossierManagementDTOs.*;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;
import ormvat.sadsa.service.FileStorageService;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DossierManagementService {

    private final DossierRepository dossierRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final JoursFerieRepository joursFerieRepository;
    private final FormConfigurationService formConfigService;
    private final TraceRepository traceRepository;
    private final HistoriqueRepository historiqueRepository;
    private final EmplacementRepository emplacementRepository;
    private final EtapeRepository etapeRepository;
    private final SousRubriqueRepository sousRubriqueRepository;
    private final FileStorageService fileStorageService;

    /**
     * Get paginated list of dossiers for current user's CDA with advanced filtering
     */
    public DossierListResponse getDossiersList(String userEmail, DossierSearchRequest searchRequest) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            CDA userCDA = utilisateur.getCda();
            if (userCDA == null) {
                throw new RuntimeException("Utilisateur non associé à un CDA");
            }

            // Build dynamic specifications for filtering
            Specification<Dossier> spec = buildDossierSpecification(searchRequest, userCDA.getId());

            Pageable pageable = PageRequest.of(
                    searchRequest.getPage(),
                    searchRequest.getSize(),
                    Sort.by(Sort.Direction.fromString(searchRequest.getSortDirection()), 
                           searchRequest.getSortBy())
            );

            Page<Dossier> dossiersPage = dossierRepository.findAll(spec, pageable);

            List<DossierSummaryDTO> dossierSummaries = dossiersPage.getContent().stream()
                    .map(this::mapToDossierSummary)
                    .collect(Collectors.toList());

            return DossierListResponse.builder()
                    .dossiers(dossierSummaries)
                    .totalCount((int) dossiersPage.getTotalElements())
                    .pageNumber(searchRequest.getPage())
                    .pageSize(searchRequest.getSize())
                    .currentUserCDA(userCDA.getDescription())
                    .build();

        } catch (Exception e) {
            log.error("Error getting dossiers list for user: {}", userEmail, e);
            throw new RuntimeException("Erreur lors de la récupération des dossiers: " + e.getMessage());
        }
    }

    /**
     * Get detailed information about a specific dossier
     */
    public DossierDetailResponse getDossierDetail(Long dossierId, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Verify user has access to this dossier
            if (!dossier.getCda().getId().equals(utilisateur.getCda().getId())) {
                throw new RuntimeException("Accès non autorisé à ce dossier");
            }

            // Get form configurations
            List<Map<String, Object>> formConfigs = formConfigService.getFormsForSousRubrique(
                    dossier.getSousRubrique().getId());

            List<FormConfigurationDTO> availableForms = formConfigs.stream()
                    .map(config -> mapToFormConfiguration(config, dossierId))
                    .collect(Collectors.toList());

            // Get uploaded files
            List<PieceJointe> pieceJointes = pieceJointeRepository.findByDossierId(dossierId);
            List<PieceJointeDTO> pieceJointeDTOs = pieceJointes.stream()
                    .map(this::mapToPieceJointeDTO)
                    .collect(Collectors.toList());

            // Calculate days remaining
            int joursRestants = calculateRemainingDays(dossier);

            return DossierDetailResponse.builder()
                    .dossier(mapToDossierInfo(dossier))
                    .agriculteur(mapToAgriculteurInfo(dossier.getAgriculteur()))
                    .availableForms(availableForms)
                    .pieceJointes(pieceJointeDTOs)
                    .joursRestants(joursRestants)
                    .peutEtreModifie(canBeModified(dossier))
                    .peutEtreSupprime(canBeDeleted(dossier))
                    .peutEtreEnvoye(canBeSent(dossier))
                    .validationErrors(validateDossier(dossier))
                    .build();

        } catch (Exception e) {
            log.error("Error getting dossier detail: {}", dossierId, e);
            throw new RuntimeException("Erreur lors de la récupération du dossier: " + e.getMessage());
        }
    }

    /**
     * Submit form data with file uploads using FileStorageService
     */
    @Transactional
    public void submitFormData(FormSubmissionRequest request, List<MultipartFile> files, String userEmail) {
        try {
            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Verify permissions
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!dossier.getCda().getId().equals(utilisateur.getCda().getId()) || !canBeModified(dossier)) {
                throw new RuntimeException("Modification non autorisée");
            }

            // Create dossier directory
            String dossierDirectory = fileStorageService.createDossierDirectory(dossier.getId());

            // Save files if provided
            if (files != null && !files.isEmpty()) {
                for (int i = 0; i < files.size(); i++) {
                    MultipartFile file = files.get(i);
                    String customTitle = i < request.getFiles().size() ? 
                        request.getFiles().get(i).getCustomTitle() : file.getOriginalFilename();
                    boolean isOriginal = i < request.getFiles().size() ? 
                        request.getFiles().get(i).isOriginalDocument() : false;

                    saveFileForDossier(dossier, file, request.getFormId(), customTitle, isOriginal, 
                                     request.getFormData(), dossierDirectory);
                }
            } else if (request.getFormData() != null && !request.getFormData().isEmpty()) {
                // Save form data without files
                saveFormDataOnly(dossier, request.getFormId(), request.getFormData());
            }

            // Create trace
            createTrace(dossier, utilisateur, "FORM_SUBMITTED", "Formulaire " + request.getFormId() + " soumis");

        } catch (Exception e) {
            log.error("Error submitting form data", e);
            throw new RuntimeException("Erreur lors de la soumission du formulaire: " + e.getMessage());
        }
    }

    /**
     * Delete a dossier and its associated files
     */
    @Transactional
    public DossierActionResponse deleteDossier(Long dossierId, String userEmail, String comment) {
        try {
            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!canBeDeleted(dossier) || !dossier.getCda().getId().equals(utilisateur.getCda().getId())) {
                throw new RuntimeException("Suppression non autorisée");
            }

            // Create trace before deletion
            createTrace(dossier, utilisateur, "DOSSIER_DELETED", comment);

            // Delete associated files
            List<PieceJointe> pieceJointes = pieceJointeRepository.findByDossierId(dossierId);
            for (PieceJointe pj : pieceJointes) {
                if (pj.getCheminDocumentOriginal() != null) {
                    fileStorageService.deleteFile(pj.getCheminDocumentOriginal());
                }
            }

            // Delete related data
            pieceJointeRepository.deleteAll(pieceJointes);
            historiqueRepository.deleteAll(historiqueRepository.findByDossierId(dossierId));
            traceRepository.deleteAll(traceRepository.findByDossierId(dossierId));

            // Delete dossier
            dossierRepository.delete(dossier);

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier supprimé avec succès")
                    .actionDate(LocalDate.now())
                    .build();

        } catch (Exception e) {
            log.error("Error deleting dossier: {}", dossierId, e);
            return DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur lors de la suppression: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Send dossier to GUC
     */
    @Transactional
    public DossierActionResponse sendDossierToGUC(Long dossierId, String userEmail, String comment) {
        try {
            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!canBeSent(dossier) || !dossier.getCda().getId().equals(utilisateur.getCda().getId())) {
                throw new RuntimeException("Envoi non autorisé");
            }

            // Validate dossier completion
            List<String> validationErrors = validateDossier(dossier);
            if (!validationErrors.isEmpty()) {
                throw new RuntimeException("Dossier incomplet: " + String.join(", ", validationErrors));
            }

            // Update dossier status
            Etape gucEtape = etapeRepository.findByDesignation("Phase GUC");
            if (gucEtape == null) {
                throw new RuntimeException("Étape GUC non trouvée");
            }

            dossier.setEtapeActuelle(gucEtape);
            dossierRepository.save(dossier);

            // Update historique
            updateHistoriqueForTransition(dossier, "Phase GUC");

            // Create trace
            createTrace(dossier, utilisateur, "SENT_TO_GUC", comment);

            return DossierActionResponse.builder()
                    .success(true)
                    .message("Dossier envoyé au GUC avec succès")
                    .newStatus("ENVOYE_GUC")
                    .actionDate(LocalDate.now())
                    .build();

        } catch (Exception e) {
            log.error("Error sending dossier to GUC: {}", dossierId, e);
            return DossierActionResponse.builder()
                    .success(false)
                    .message("Erreur lors de l'envoi: " + e.getMessage())
                    .build();
        }
    }

    // Private helper methods

    private Specification<Dossier> buildDossierSpecification(DossierSearchRequest searchRequest, Long cdaId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by CDA
            predicates.add(criteriaBuilder.equal(root.get("cda").get("id"), cdaId));

            // Search term filter (SABA, reference, agriculteur name, CIN)
            if (searchRequest.getSearchTerm() != null && !searchRequest.getSearchTerm().trim().isEmpty()) {
                String searchTerm = "%" + searchRequest.getSearchTerm().toLowerCase() + "%";
                Predicate searchPredicate = criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("saba")), searchTerm),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("reference")), searchTerm),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("agriculteur").get("nom")), searchTerm),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("agriculteur").get("prenom")), searchTerm),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("agriculteur").get("cin")), searchTerm)
                );
                predicates.add(searchPredicate);
            }

            // Status filter
            if (searchRequest.getStatut() != null && !searchRequest.getStatut().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("etapeActuelle").get("designation"), searchRequest.getStatut()));
            }

            // Sous-rubrique filter
            if (searchRequest.getSousRubriqueId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("sousRubrique").get("id"), searchRequest.getSousRubriqueId()));
            }

            // Date range filter
            if (searchRequest.getDateFrom() != null || searchRequest.getDateTo() != null) {
                // This would require joining with Historique to get creation date
                // For now, we'll skip this complex filter
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private DossierSummaryDTO mapToDossierSummary(Dossier dossier) {
        int joursRestants = calculateRemainingDays(dossier);
        int[] completion = calculateCompletionPercentage(dossier);

        return DossierSummaryDTO.builder()
                .id(dossier.getId())
                .reference(dossier.getReference())
                .saba(dossier.getSaba())
                .agriculteurNom(dossier.getAgriculteur().getNom())
                .agriculteurPrenom(dossier.getAgriculteur().getPrenom())
                .agriculteurCin(dossier.getAgriculteur().getCin())
                .sousRubriqueDesignation(dossier.getSousRubrique().getDesignation())
                .montantDemande(0.0) // Add this field to Dossier entity if needed
                .dateCreation(getCreationDate(dossier))
                .statut(dossier.getEtapeActuelle().getDesignation())
                .joursRestants(joursRestants)
                .peutEtreModifie(canBeModified(dossier))
                .peutEtreSupprime(canBeDeleted(dossier))
                .peutEtreEnvoye(canBeSent(dossier))
                .completionPercentage(completion[0])
                .formsCompleted(completion[1])
                .totalForms(completion[2])
                .build();
    }

    private LocalDate getCreationDate(Dossier dossier) {
        return dossier.getHistoriques().stream()
                .filter(h -> h.getEmplacement().getDesignation().equals("Phase Antenne"))
                .map(h -> h.getDateReception().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .findFirst()
                .orElse(LocalDate.now());
    }

    private DossierInfoDTO mapToDossierInfo(Dossier dossier) {
        return DossierInfoDTO.builder()
                .id(dossier.getId())
                .reference(dossier.getReference())
                .saba(dossier.getSaba())
                .sousRubriqueDesignation(dossier.getSousRubrique().getDesignation())
                .rubriqueDesignation(dossier.getSousRubrique().getRubrique().getDesignation())
                .statut(dossier.getEtapeActuelle().getDesignation())
                .cdaNom(dossier.getCda().getDescription())
                .antenneNom(dossier.getCda().getAntenne() != null ? 
                           dossier.getCda().getAntenne().getDesignation() : "N/A")
                .dateCreation(getCreationDate(dossier))
                .build();
    }

    private AgriculteurInfoDTO mapToAgriculteurInfo(Agriculteur agriculteur) {
        return AgriculteurInfoDTO.builder()
                .id(agriculteur.getId())
                .nom(agriculteur.getNom())
                .prenom(agriculteur.getPrenom())
                .cin(agriculteur.getCin())
                .telephone(agriculteur.getTelephone())
                .communeRurale(agriculteur.getCommuneRurale() != null ? 
                              agriculteur.getCommuneRurale().getDesignation() : null)
                .douar(agriculteur.getDouar() != null ? 
                       agriculteur.getDouar().getDesignation() : null)
                .cercle(agriculteur.getCommuneRurale() != null && 
                       agriculteur.getCommuneRurale().getCercle() != null ?
                       agriculteur.getCommuneRurale().getCercle().getDesignation() : null)
                .province(agriculteur.getCommuneRurale() != null && 
                         agriculteur.getCommuneRurale().getCercle() != null &&
                         agriculteur.getCommuneRurale().getCercle().getProvince() != null ?
                         agriculteur.getCommuneRurale().getCercle().getProvince().getDesignation() : null)
                .build();
    }

   private FormConfigurationDTO mapToFormConfiguration(Map<String, Object> config, Long dossierId) {
        String formId = (String) config.get("formId");
        String title = (String) config.get("title");
        Long documentId = ((Number) config.get("documentId")).longValue();
        
        // Check if form is completed
        boolean isCompleted = pieceJointeRepository.findByDossierIdAndDocumentRequisId(dossierId, documentId)
                .stream()
                .anyMatch(pj -> pj.getDonneesFormulaireJson() != null && 
                               !pj.getDonneesFormulaireJson().trim().isEmpty());

        return FormConfigurationDTO.builder()
                .formId(formId)
                .title(title)
                .description((String) config.get("description"))
                .formConfig(config)
                .isCompleted(isCompleted)
                .requiredDocuments(List.of((String) config.get("documentName")))
                .build();
    }

    private PieceJointeDTO mapToPieceJointeDTO(PieceJointe pieceJointe) {
        Map<String, Object> formDataMap = parseFormData(pieceJointe.getDonneesFormulaireJson());
        
        return PieceJointeDTO.builder()
                .id(pieceJointe.getId())
                .documentType(pieceJointe.getDocumentRequis() != null ? 
                             pieceJointe.getDocumentRequis().getNomDocument() : "Document personnalisé")
                .fileName(extractFileName(pieceJointe.getCheminDocumentOriginal()))
                .filePath(pieceJointe.getCheminDocumentOriginal())
                .customTitle((String) formDataMap.get("customTitle"))
                .dateUpload(pieceJointe.getDateUpload().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .statut(pieceJointe.getStatut().toString())
                .formData(formDataMap)
                .isOriginalDocument(Boolean.TRUE.equals(formDataMap.get("isOriginalDocument")))
                .build();
    }

    private void saveFileForDossier(Dossier dossier, MultipartFile file, String formId, 
                                   String customTitle, boolean isOriginal, Map<String, Object> formData,
                                   String dossierDirectory) {
        try {
            // Store file using FileStorageService
            String filePath = fileStorageService.storeFile(file, dossierDirectory);

            // Save piece jointe record
            PieceJointe pieceJointe = new PieceJointe();
            pieceJointe.setDossier(dossier);
            pieceJointe.setCheminDocumentOriginal(filePath);
            pieceJointe.setDateUpload(new Date());
            pieceJointe.setStatut(PieceJointe.StatutPieceJointe.PENDING);
            
            if (formData != null || customTitle != null) {
                pieceJointe.setDonneesFormulaireJson(convertFormDataToJson(formData, formId, customTitle, isOriginal));
            }

            pieceJointeRepository.save(pieceJointe);
            
        } catch (Exception e) {
            log.error("Error saving file for dossier", e);
            throw new RuntimeException("Erreur lors de la sauvegarde du fichier: " + e.getMessage());
        }
    }

    private void saveFormDataOnly(Dossier dossier, String formId, Map<String, Object> formData) {
        PieceJointe pieceJointe = new PieceJointe();
        pieceJointe.setDossier(dossier);
        pieceJointe.setDateUpload(new Date());
        pieceJointe.setStatut(PieceJointe.StatutPieceJointe.COMPLETE);
        pieceJointe.setDonneesFormulaireJson(convertFormDataToJson(formData, formId, null, false));

        pieceJointeRepository.save(pieceJointe);
    }

    private String convertFormDataToJson(Map<String, Object> formData, String formId, String customTitle, boolean isOriginal) {
        Map<String, Object> dataWithMeta = new HashMap<>();
        if (formData != null) {
            dataWithMeta.putAll(formData);
        }
        dataWithMeta.put("formId", formId);
        dataWithMeta.put("submissionDate", LocalDate.now().toString());
        if (customTitle != null) dataWithMeta.put("customTitle", customTitle);
        dataWithMeta.put("isOriginalDocument", isOriginal);
        
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(dataWithMeta);
        } catch (Exception e) {
            log.error("Error converting form data to JSON", e);
            return "{}";
        }
    }

    private Map<String, Object> parseFormData(String jsonData) {
        if (jsonData == null || jsonData.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(jsonData, Map.class);
        } catch (Exception e) {
            log.error("Error parsing form data JSON", e);
            return new HashMap<>();
        }
    }

    private void createTrace(Dossier dossier, Utilisateur utilisateur, String action, String comment) {
        Trace trace = new Trace();
        trace.setDossier(dossier);
        trace.setUtilisateur(utilisateur);
        trace.setAction(action);
        trace.setDateAction(new Date());
        traceRepository.save(trace);
    }

    private void updateHistoriqueForTransition(Dossier dossier, String newEmplacementName) {
        // Close current historique
        List<Historique> currentHistoriques = historiqueRepository.findByDossierId(dossier.getId());
        currentHistoriques.stream()
                .filter(h -> h.getDateEnvoi() == null)
                .forEach(h -> {
                    h.setDateEnvoi(new Date());
                    historiqueRepository.save(h);
                });

        // Create new historique
        Emplacement newEmplacement = emplacementRepository.findByDesignation(newEmplacementName);
        if (newEmplacement != null) {
            Historique newHistorique = new Historique();
            newHistorique.setDossier(dossier);
            newHistorique.setEmplacement(newEmplacement);
            newHistorique.setDateReception(new Date());
            historiqueRepository.save(newHistorique);
        }
    }

    private int calculateRemainingDays(Dossier dossier) {
        // Get the creation date from historique
        Date creationDate = dossier.getHistoriques().stream()
                .filter(h -> h.getEmplacement().getDesignation().equals("Phase Antenne"))
                .map(Historique::getDateReception)
                .findFirst()
                .orElse(new Date());

        // Calculate days since creation
        LocalDate creationLocalDate = creationDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate now = LocalDate.now();
        
        // Get working days (excluding jours fériés)
        List<JoursFerie> joursFeries = joursFerieRepository.findByDateBetween(
                java.sql.Date.valueOf(creationLocalDate), 
                java.sql.Date.valueOf(now));
        
        long daysPassed = java.time.temporal.ChronoUnit.DAYS.between(creationLocalDate, now);
        long ferieDays = joursFeries.size();
        long workingDaysPassed = daysPassed - ferieDays;
        
        // Phase Antenne has 3 days limit
        return Math.max(0, 3 - (int) workingDaysPassed);
    }

    private boolean canBeModified(Dossier dossier) {
        return "Phase Antenne".equals(dossier.getEtapeActuelle().getDesignation());
    }

    private boolean canBeDeleted(Dossier dossier) {
        return "Phase Antenne".equals(dossier.getEtapeActuelle().getDesignation());
    }

    private boolean canBeSent(Dossier dossier) {
        return "Phase Antenne".equals(dossier.getEtapeActuelle().getDesignation()) &&
               validateDossier(dossier).isEmpty();
    }

    private List<String> validateDossier(Dossier dossier) {
        List<String> errors = new ArrayList<>();
        
        // Check if basic info is complete
        if (dossier.getSaba() == null || dossier.getSaba().isEmpty()) {
            errors.add("Numéro SABA manquant");
        }
        
        // Check if minimum forms are completed
        List<Map<String, Object>> requiredForms = formConfigService.getFormsForSousRubrique(
                dossier.getSousRubrique().getId());
        
        if (requiredForms.isEmpty()) {
            errors.add("Aucun formulaire configuré pour ce type de projet");
        } else {
            long completedForms = pieceJointeRepository.findByDossierId(dossier.getId()).stream()
                    .filter(pj -> pj.getDonneesFormulaireJson() != null && !pj.getDonneesFormulaireJson().trim().isEmpty())
                    .count();
            
            if (completedForms == 0) {
                errors.add("Aucun formulaire rempli");
            }
        }
        
        return errors;
    }

    private int[] calculateCompletionPercentage(Dossier dossier) {
        List<Map<String, Object>> totalForms = formConfigService.getFormsForSousRubrique(
                dossier.getSousRubrique().getId());
        
        if (totalForms.isEmpty()) {
            return new int[]{100, 0, 0};
        }
        
        int completedForms = (int) pieceJointeRepository.findByDossierId(dossier.getId()).stream()
                .filter(pj -> pj.getDonneesFormulaireJson() != null && !pj.getDonneesFormulaireJson().trim().isEmpty())
                .count();
        
        int percentage = Math.min(100, (completedForms * 100) / totalForms.size());
        
        return new int[]{percentage, completedForms, totalForms.size()};
    }

    private String extractFileName(String filePath) {
        if (filePath == null) return "N/A";
        return java.nio.file.Paths.get(filePath).getFileName().toString();
    }
}