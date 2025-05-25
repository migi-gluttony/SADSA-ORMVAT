package ormvat.sadsa.service.agent_antenne;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ormvat.sadsa.dto.agent_antenne.DossierCreationDTOs.*;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DossierCreationService {

    private final DossierRepository dossierRepository;
    private final AgriculteurRepository agriculteurRepository;
    private final CDARepository cdaRepository;
    private final RubriqueRepository rubriqueRepository;
    private final SousRubriqueRepository sousRubriqueRepository;
    private final EtapeRepository etapeRepository;
    private final TraceRepository traceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ProvinceRepository provinceRepository;
    private final CercleRepository cercleRepository;
    private final CommuneRuraleRepository communeRuraleRepository;
    private final DouarRepository douarRepository;

    /**
     * Récupère toutes les rubriques avec leurs sous-rubriques et documents requis
     */
    public RubriquesResponse getRubriquesWithDocuments() {
        List<Rubrique> rubriques = rubriqueRepository.findAll();

        List<RubriqueDTO> rubriqueDTOs = rubriques.stream()
                .map(this::mapToRubriqueDTO)
                .collect(Collectors.toList());

        return RubriquesResponse.builder()
                .rubriques(rubriqueDTOs)
                .build();
    }

    /**
     * Récupère les informations des CDAs disponibles
     */
    public List<CDAInfoDTO> getAvailableCDAs() {
        List<CDA> cdas = cdaRepository.findAll();

        return cdas.stream()
                .map(cda -> CDAInfoDTO.builder()
                        .id(cda.getId())
                        .description(cda.getDescription())
                        .antenneNom(cda.getAntenne() != null ? cda.getAntenne().getDesignation() : "N/A")
                        .antenneId(cda.getAntenne() != null ? cda.getAntenne().getId() : null)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Crée un nouveau dossier
     */
   @Transactional
public CreateDossierResponse createDossier(CreateDossierRequest request, String userEmail) {
    try {
        log.info("Début création dossier pour l'utilisateur: {}", userEmail);

        // 1. Récupération de l'utilisateur et son CDA
        Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        
        if (utilisateur.getCda() == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucun CDA");
        }

        CDA cda = utilisateur.getCda();

        // 2. Validation des données d'entrée
        validateCreateDossierRequest(request);

        // 3. Récupération ou création de l'agriculteur
        Agriculteur agriculteur = getOrCreateAgriculteur(request.getAgriculteur());

        // 4. Récupération des entités liées
        SousRubrique sousRubrique = sousRubriqueRepository.findById(request.getDossier().getSousRubriqueId())
                .orElseThrow(() -> new RuntimeException("Sous-rubrique non trouvée"));

        Etape etapeInitiale = etapeRepository.findByDesignation("Phase Antenne");
        if (etapeInitiale == null) {
            throw new RuntimeException("Étape initiale non trouvée");
        }

        // 5. Création du dossier
        Dossier dossier = new Dossier();
        dossier.setSaba(request.getDossier().getSaba());
        dossier.setReference(generateReference(cda, sousRubrique));
        dossier.setAgriculteur(agriculteur);
        dossier.setCda(cda);
        dossier.setSousRubrique(sousRubrique);
        dossier.setEtapeActuelle(etapeInitiale);

        // 6. Sauvegarde du dossier
        Dossier savedDossier = dossierRepository.save(dossier);

        // 7. Création de la trace d'audit
        createAuditTrace(savedDossier, userEmail, "CREATION_DOSSIER",
                "Création du dossier SABA: " + request.getDossier().getSaba());

        // 8. Génération du récépissé
        RecepisseDossierDTO recepisse = generateRecepisse(savedDossier, request);

        log.info("Dossier créé avec succès - ID: {}, SABA: {}",
                savedDossier.getId(), savedDossier.getSaba());

        return CreateDossierResponse.builder()
                .dossierId(savedDossier.getId())
                .numeroDossier(savedDossier.getReference())
                .statut(StatutDossier.SOUMIS.name())
                .message("Dossier créé avec succès")
                .recepisse(recepisse)
                .build();

    } catch (Exception e) {
        log.error("Erreur lors de la création du dossier", e);
        throw new RuntimeException("Erreur lors de la création du dossier: " + e.getMessage());
    }
}
    /**
     * Valide une demande de création de dossier
     */
    public DossierValidationDTO validateDossier(CreateDossierRequest request) {
        List<String> missingFields = new ArrayList<>();
        List<String> invalidFields = new ArrayList<>();
        Map<String, String> suggestions = new HashMap<>();

        // Validation agriculteur
        if (request.getAgriculteur().getCin() == null || request.getAgriculteur().getCin().trim().isEmpty()) {
            missingFields.add("CIN de l'agriculteur");
        }

        if (request.getAgriculteur().getNom() == null || request.getAgriculteur().getNom().trim().isEmpty()) {
            missingFields.add("Nom de l'agriculteur");
        }

        if (request.getAgriculteur().getPrenom() == null || request.getAgriculteur().getPrenom().trim().isEmpty()) {
            missingFields.add("Prénom de l'agriculteur");
        }

        // Validation dossier
        if (request.getDossier().getSaba() == null || request.getDossier().getSaba().trim().isEmpty()) {
            missingFields.add("Numéro SABA");
        } else if (!isValidSabaFormat(request.getDossier().getSaba())) {
            invalidFields.add("Format SABA invalide");
            suggestions.put("saba", "Format attendu: 000XXX/YYYY/ZZZ");
        }

       
        if (request.getDossier().getSousRubriqueId() == null) {
            missingFields.add("Sous-rubrique");
        }

        if (request.getDossier().getMontantDemande() == null || request.getDossier().getMontantDemande() <= 0) {
            missingFields.add("Montant demandé");
        }

        // Vérification unicité SABA
        if (request.getDossier().getSaba() != null &&
                dossierRepository.findBySaba(request.getDossier().getSaba()).isPresent()) {
            invalidFields.add("Numéro SABA déjà utilisé");
        }

        boolean isValid = missingFields.isEmpty() && invalidFields.isEmpty();

        return DossierValidationDTO.builder()
                .isValid(isValid)
                .missingFields(missingFields)
                .invalidFields(invalidFields)
                .suggestions(suggestions)
                .build();
    }

    /**
     * Génère un résumé du dossier avant soumission
     */
    public DossierSummaryDTO generateDossierSummary(CreateDossierRequest request, String userEmail) {
    // Get user's CDA instead of from request
    Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    CDA cda = utilisateur.getCda();
    
    SousRubrique sousRubrique = sousRubriqueRepository.findById(request.getDossier().getSousRubriqueId())
            .orElse(null);

    List<FormulaireRempliDTO> formulairesRemplis = new ArrayList<>();
    if (request.getFormulairesDynamiques() != null) {
        for (Map.Entry<String, Object> entry : request.getFormulairesDynamiques().entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> donnees = (Map<String, Object>) entry.getValue();
            
            FormulaireRempliDTO formulaire = FormulaireRempliDTO.builder()
                    .nomFormulaire(entry.getKey())
                    .donnees(donnees)
                    .isComplete(isFormulaireComplete(donnees))
                    .build();
            formulairesRemplis.add(formulaire);
        }
    }

    return DossierSummaryDTO.builder()
            .agriculteur(request.getAgriculteur())
            .dossier(request.getDossier())
            .rubriqueNom(sousRubrique != null && sousRubrique.getRubrique() != null
                    ? sousRubrique.getRubrique().getDesignation()
                    : "N/A")
            .sousRubriqueNom(sousRubrique != null ? sousRubrique.getDesignation() : "N/A")
            .cdaNom(cda != null ? cda.getDescription() : "N/A")
            .formulairesRemplis(formulairesRemplis)
            .nombreDocumentsUpload(0)
            .dateCreation(LocalDate.now())
            .build();
}
    // Méthodes privées utilitaires

    private void validateCreateDossierRequest(CreateDossierRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La demande ne peut pas être nulle");
        }

        if (request.getAgriculteur() == null) {
            throw new IllegalArgumentException("Les informations de l'agriculteur sont requises");
        }

        if (request.getDossier() == null) {
            throw new IllegalArgumentException("Les informations du dossier sont requises");
        }

        DossierValidationDTO validation = validateDossier(request);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Données invalides: " +
                    String.join(", ", validation.getMissingFields()) +
                    String.join(", ", validation.getInvalidFields()));
        }
    }

    private Agriculteur getOrCreateAgriculteur(AgriculteurInfoDTO agriculteurInfo) {
        Optional<Agriculteur> existing = agriculteurRepository.findByCin(agriculteurInfo.getCin());

        Agriculteur agriculteur = existing.orElse(new Agriculteur());
        agriculteur.setNom(agriculteurInfo.getNom());
        agriculteur.setPrenom(agriculteurInfo.getPrenom());
        agriculteur.setCin(agriculteurInfo.getCin());
        agriculteur.setTelephone(agriculteurInfo.getTelephone());

        if (agriculteurInfo.getCommuneRuraleId() != null) {
            CommuneRurale commune = communeRuraleRepository.findById(agriculteurInfo.getCommuneRuraleId())
                    .orElse(null);
            agriculteur.setCommuneRurale(commune);
        }

        if (agriculteurInfo.getDouarId() != null) {
            Douar douar = douarRepository.findById(agriculteurInfo.getDouarId())
                    .orElse(null);
            agriculteur.setDouar(douar);
        }

        return agriculteurRepository.save(agriculteur);
    }

    private String generateReference(CDA cda, SousRubrique sousRubrique) {
        String annee = String.valueOf(LocalDate.now().getYear());
        String cdaCode = String.format("%03d", cda.getId());
        String sousRubriqueCode = String.format("%02d", sousRubrique.getId());
        long sequence = dossierRepository.count() + 1;
        String seqCode = String.format("%06d", sequence);

        return String.format("DOS-%s-%s-%s-%s", annee, cdaCode, sousRubriqueCode, seqCode);
    }

    private RecepisseDossierDTO generateRecepisse(Dossier dossier, CreateDossierRequest request) {
        String numeroRecepisse = "R-" + LocalDate.now().getYear() + "-" +
                String.format("%06d", System.currentTimeMillis() % 1000000);

        return RecepisseDossierDTO.builder()
                .numeroRecepisse(numeroRecepisse)
                .dateDepot(LocalDate.now())
                .nomComplet(dossier.getAgriculteur().getNom() + " " + dossier.getAgriculteur().getPrenom())
                .cin(dossier.getAgriculteur().getCin())
                .telephone(dossier.getAgriculteur().getTelephone())
                .typeProduit(dossier.getSousRubrique().getDesignation())
                .saba(dossier.getSaba())
                .montantDemande(request.getDossier().getMontantDemande())
                .cdaNom(dossier.getCda().getDescription())
                .antenne(dossier.getCda().getAntenne() != null ? dossier.getCda().getAntenne().getDesignation() : "N/A")
                .build();
    }

    private void createAuditTrace(Dossier dossier, String userEmail, String action, String description) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail).orElse(null);

            Trace trace = new Trace();
            trace.setDossier(dossier);
            trace.setUtilisateur(utilisateur);
            trace.setAction(action);
            trace.setDateAction(new Date());

            traceRepository.save(trace);

            log.debug("Trace d'audit créée pour le dossier: {}, action: {}", dossier.getId(), action);
        } catch (Exception e) {
            log.warn("Erreur lors de la création de la trace d'audit: {}", e.getMessage());
        }
    }

    private boolean isValidSabaFormat(String saba) {
        // Format attendu: 000XXX/YYYY/ZZZ
        return saba.matches("\\d{6}/\\d{4}/\\d{3}");
    }

    private boolean isFormulaireComplete(Map<String, Object> donnees) {
        if (donnees == null || donnees.isEmpty()) {
            return false;
        }

        // Vérifier qu'il n'y a pas de valeurs nulles ou vides
        return donnees.values().stream()
                .allMatch(value -> value != null && !value.toString().trim().isEmpty());
    }

    private RubriqueDTO mapToRubriqueDTO(Rubrique rubrique) {
        List<SousRubriqueDTO> sousRubriquesDTOs = rubrique.getSousRubriques().stream()
                .map(this::mapToSousRubriqueDTO)
                .collect(Collectors.toList());

        return RubriqueDTO.builder()
                .id(rubrique.getId())
                .designation(rubrique.getDesignation())
                .sousRubriques(sousRubriquesDTOs)
                .build();
    }

    private SousRubriqueDTO mapToSousRubriqueDTO(SousRubrique sousRubrique) {
        List<DocumentRequiredDTO> documentsRequis = getDocumentsForSousRubrique(sousRubrique.getDesignation());
        String codeType = generateCodeType(sousRubrique.getDesignation());

        return SousRubriqueDTO.builder()
                .id(sousRubrique.getId())
                .designation(sousRubrique.getDesignation())
                .codeType(codeType)
                .documentsRequis(documentsRequis)
                .build();
    }

    private List<DocumentRequiredDTO> getDocumentsForSousRubrique(String designation) {
        List<DocumentRequiredDTO> documents = new ArrayList<>();

        switch (designation.toLowerCase()) {
            case "aménagement hydro-agricole":
            case "amélioration foncière":
                documents.add(DocumentRequiredDTO.builder()
                        .nom("Annexes GAG exigé par le circulaire")
                        .uploadRequired(true)
                        .formRequired(true)
                        .formConfigPath("/assets/forms/aha-af/annexes-gag.json")
                        .build());
                documents.add(DocumentRequiredDTO.builder()
                        .nom("MODELE ENGAGEMENT DE CONSERVATION DE L'INVESTISSEMENT")
                        .uploadRequired(false)
                        .formRequired(true)
                        .formConfigPath("/assets/forms/aha-af/engagement-conservation.json")
                        .build());
                break;

            case "acquisition et installation des serres destinées à la production agricole":
                documents.add(DocumentRequiredDTO.builder()
                        .nom("Modele-demande-approbation_prealable")
                        .uploadRequired(true)
                        .formRequired(true)
                        .formConfigPath("/assets/forms/filieres-vegetales/serres/demande-approbation.json")
                        .build());
                documents.add(DocumentRequiredDTO.builder()
                        .nom("MODELE ENGAGEMENT DE CONSERVATION DE L'INVESTISSEMENT")
                        .uploadRequired(false)
                        .formRequired(true)
                        .formConfigPath("/assets/forms/filieres-vegetales/serres/engagement-conservation.json")
                        .build());
                documents.add(DocumentRequiredDTO.builder()
                        .nom("Modele-demande-subvention")
                        .uploadRequired(true)
                        .formRequired(true)
                        .formConfigPath("/assets/forms/filieres-vegetales/serres/demande-subvention.json")
                        .build());
                break;

            case "arboriculture fruitière":
                documents.add(DocumentRequiredDTO.builder()
                        .nom("Modele-demande-approbation_prealable")
                        .uploadRequired(true)
                        .formRequired(true)
                        .formConfigPath("/assets/forms/filieres-vegetales/arboriculture/demande-approbation.json")
                        .build());
                documents.add(DocumentRequiredDTO.builder()
                        .nom("FICHE DESCRIPTIVE PLANTATION")
                        .uploadRequired(true)
                        .formRequired(true)
                        .formConfigPath("/assets/forms/filieres-vegetales/arboriculture/fiche-descriptive.json")
                        .build());
                documents.add(DocumentRequiredDTO.builder()
                        .nom("MODELE ENGAGEMENT DE CONSERVATION DE L'INVESTISSEMENT")
                        .uploadRequired(false)
                        .formRequired(true)
                        .formConfigPath("/assets/forms/filieres-vegetales/arboriculture/engagement-conservation.json")
                        .build());
                documents.add(DocumentRequiredDTO.builder()
                        .nom("Modele-demande-subvention")
                        .uploadRequired(true)
                        .formRequired(true)
                        .formConfigPath("/assets/forms/filieres-vegetales/arboriculture/demande-subvention.json")
                        .build());
                break;

            // Ajouter les autres cas selon le pattern...
            default:
                // Document par défaut
                documents.add(DocumentRequiredDTO.builder()
                        .nom("MODELE ENGAGEMENT DE CONSERVATION DE L'INVESTISSEMENT")
                        .uploadRequired(false)
                        .formRequired(true)
                        .formConfigPath("/assets/forms/common/engagement-conservation.json")
                        .build());
        }

        return documents;
    }

    private String generateCodeType(String designation) {
        return designation.toLowerCase()
                .replaceAll("[àáâãäå]", "a")
                .replaceAll("[èéêë]", "e")
                .replaceAll("[ìíîï]", "i")
                .replaceAll("[òóôõö]", "o")
                .replaceAll("[ùúûü]", "u")
                .replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    // Auto-generate SABA
    public String generateSabaNumber() {
        long count = dossierRepository.count() + 1;
        int year = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();

        String sequence = String.format("%04d%02d", count, month);
        String yearStr = String.valueOf(year);
        String finalSeq = String.format("%03d", (count % 1000) + 1);

        return String.format("%s/%s/%s", sequence, yearStr, finalSeq);
    }

    // Get geographic data
    public List<GeographicDTO> getProvinces() {
        return provinceRepository.findAll().stream()
                .map(p -> GeographicDTO.builder()
                        .id(p.getId())
                        .designation(p.getDesignation())
                        .build())
                .collect(Collectors.toList());
    }

    public List<GeographicDTO> getCerclesByProvince(Long provinceId) {
        return cercleRepository.findByProvinceId(provinceId).stream()
                .map(c -> GeographicDTO.builder()
                        .id(c.getId())
                        .designation(c.getDesignation())
                        .parentId(c.getProvince().getId())
                        .build())
                .collect(Collectors.toList());
    }

    public List<GeographicDTO> getCommunesByCircle(Long cercleId) {
        return communeRuraleRepository.findByCercleId(cercleId).stream()
                .map(c -> GeographicDTO.builder()
                        .id(c.getId())
                        .designation(c.getDesignation())
                        .parentId(c.getCercle().getId())
                        .build())
                .collect(Collectors.toList());
    }

    public List<GeographicDTO> getDouarsByCommune(Long communeId) {
        return douarRepository.findByCommuneRuraleId(communeId).stream()
                .map(d -> GeographicDTO.builder()
                        .id(d.getId())
                        .designation(d.getDesignation())
                        .parentId(d.getCommuneRurale().getId())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get user's CDA information
     */
    public CDAInfoDTO getUserCDA(String userEmail) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (utilisateur.getCda() == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucun CDA");
        }

        CDA cda = utilisateur.getCda();
        return CDAInfoDTO.builder()
                .id(cda.getId())
                .description(cda.getDescription())
                .antenneNom(cda.getAntenne() != null ? cda.getAntenne().getDesignation() : "N/A")
                .antenneId(cda.getAntenne() != null ? cda.getAntenne().getId() : null)
                .build();
    }
}