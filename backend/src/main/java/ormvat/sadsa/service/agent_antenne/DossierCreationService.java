package ormvat.sadsa.service.agent_antenne;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ormvat.sadsa.dto.agent_antenne.DossierCreationDTOs.*;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;
import ormvat.sadsa.service.common.WorkflowService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DossierCreationService {

    private final DossierRepository dossierRepository;
    private final AgriculteurRepository agriculteurRepository;
    private final RubriqueRepository rubriqueRepository;
    private final SousRubriqueRepository sousRubriqueRepository;
    private final AuditTrailRepository auditTrailRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AntenneRepository antenneRepository;
    private final ProvinceRepository provinceRepository;
    private final CercleRepository cercleRepository;
    private final CommuneRuraleRepository communeRuraleRepository;
    private final DouarRepository douarRepository;
    private final DocumentRequisRepository documentRequisRepository;
    private final WorkflowService workflowService;

    /**
     * Get all initialization data needed for dossier creation
     */
    public InitializationDataResponse getInitializationData(String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            Antenne userAntenne = utilisateur.getAntenne();
            if (userAntenne == null) {
                throw new RuntimeException("L'utilisateur n'est associé à aucune antenne");
            }

            List<Rubrique> rubriques = rubriqueRepository.findAll();
            List<SimplifiedRubriqueDTO> rubriqueDTOs = rubriques.stream()
                    .map(this::mapToSimplifiedRubriqueDTO)
                    .collect(Collectors.toList());

            List<GeographicDTO> provinces = provinceRepository.findAll().stream()
                    .map(p -> GeographicDTO.builder()
                            .id(p.getId())
                            .designation(p.getDesignation())
                            .build())
                    .collect(Collectors.toList());

            List<AntenneInfoDTO> antennes = antenneRepository.findAll().stream()
                    .map(a -> AntenneInfoDTO.builder()
                            .id(a.getId())
                            .designation(a.getDesignation())
                            .cdaNom(a.getCda() != null ? a.getCda().getDescription() : "N/A")
                            .cdaId(a.getCda() != null ? a.getCda().getId() : null)
                            .build())
                    .collect(Collectors.toList());

            String sabaNumber = generateSabaNumber();

            return InitializationDataResponse.builder()
                    .userAntenne(AntenneInfoDTO.builder()
                            .id(userAntenne.getId())
                            .designation(userAntenne.getDesignation())
                            .cdaNom(userAntenne.getCda() != null ? userAntenne.getCda().getDescription() : "N/A")
                            .cdaId(userAntenne.getCda() != null ? userAntenne.getCda().getId() : null)
                            .build())
                    .rubriques(rubriqueDTOs)
                    .provinces(provinces)
                    .antennes(antennes)
                    .generatedSaba(sabaNumber)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du chargement des données d'initialisation", e);
            throw new RuntimeException("Erreur lors du chargement des données: " + e.getMessage());
        }
    }

    /**
     * Check if farmer exists by CIN
     */
    public AgriculteurCheckResponse checkFarmerExists(String cin) {
        try {
            Optional<Agriculteur> agriculteurOpt = agriculteurRepository.findByCin(cin);
            
            if (agriculteurOpt.isPresent()) {
                Agriculteur agriculteur = agriculteurOpt.get();
                
                // Get previous dossiers for this farmer
                List<Dossier> previousDossiers = dossierRepository.findByAgriculteurId(agriculteur.getId());
                List<DossierHistoryDTO> dossierHistory = previousDossiers.stream()
                        .map(d -> DossierHistoryDTO.builder()
                                .id(d.getId())
                                .numeroDossier(d.getNumeroDossier())
                                .saba(d.getSaba())
                                .sousRubriqueDesignation(d.getSousRubrique().getDesignation())
                                .status(d.getStatus().name())
                                .dateCreation(d.getDateCreation())
                                .antenneDesignation(d.getAntenne().getDesignation())
                                .build())
                        .collect(Collectors.toList());

                return AgriculteurCheckResponse.builder()
                        .exists(true)
                        .agriculteur(AgriculteurInfoDTO.builder()
                                .cin(agriculteur.getCin())
                                .nom(agriculteur.getNom())
                                .prenom(agriculteur.getPrenom())
                                .telephone(agriculteur.getTelephone())
                                .communeRuraleId(agriculteur.getCommuneRurale() != null ? 
                                        agriculteur.getCommuneRurale().getId() : null)
                                .douarId(agriculteur.getDouar() != null ? 
                                        agriculteur.getDouar().getId() : null)
                                .build())
                        .message("Agriculteur trouvé dans le système")
                        .previousDossiers(dossierHistory)
                        .build();
            } else {
                return AgriculteurCheckResponse.builder()
                        .exists(false)
                        .message("Agriculteur non trouvé dans le système")
                        .previousDossiers(new ArrayList<>())
                        .build();
            }
            
        } catch (Exception e) {
            log.error("Erreur lors de la vérification de l'agriculteur", e);
            throw new RuntimeException("Erreur lors de la vérification: " + e.getMessage());
        }
    }

    /**
     * Search project types by search term
     */
    public List<SimplifiedSousRubriqueDTO> searchProjectTypes(String searchTerm) {
        try {
            List<SousRubrique> allSousRubriques = sousRubriqueRepository.findAll();
            
            return allSousRubriques.stream()
                    .filter(sr -> sr.getDesignation().toLowerCase().contains(searchTerm.toLowerCase()) ||
                            (sr.getDescription() != null && sr.getDescription().toLowerCase().contains(searchTerm.toLowerCase())))
                    .map(sr -> SimplifiedSousRubriqueDTO.builder()
                            .id(sr.getId())
                            .designation(sr.getDesignation())
                            .description(sr.getDescription())
                            .documentsRequis(getDocumentNames(sr.getId()))
                            .build())
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Erreur lors de la recherche de types de projet", e);
            throw new RuntimeException("Erreur lors de la recherche: " + e.getMessage());
        }
    }

    /**
     * Create a new dossier with proper workflow initialization
     */
    @Transactional
    public CreateDossierResponse createDossier(CreateDossierRequest request, String userEmail) {
        try {
            log.info("Début création dossier pour l'utilisateur: {}", userEmail);

            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            Antenne antenne = antenneRepository.findById(request.getDossier().getAntenneId())
                    .orElseThrow(() -> new RuntimeException("Antenne non trouvée"));

            Agriculteur agriculteur = getOrCreateAgriculteur(request.getAgriculteur());

            SousRubrique sousRubrique = sousRubriqueRepository.findById(request.getDossier().getSousRubriqueId())
                    .orElseThrow(() -> new RuntimeException("Sous-rubrique non trouvée"));

            // Create dossier
            Dossier dossier = new Dossier();
            dossier.setSaba(request.getDossier().getSaba());
            dossier.setReference(generateReference(antenne, sousRubrique));
            dossier.setNumeroDossier(generateNumeroDossier());
            dossier.setAgriculteur(agriculteur);
            dossier.setAntenne(antenne);
            dossier.setSousRubrique(sousRubrique);
            dossier.setUtilisateurCreateur(utilisateur);
            dossier.setStatus(Dossier.DossierStatus.DRAFT);
            dossier.setDateCreation(LocalDateTime.now());

            Dossier savedDossier = dossierRepository.save(dossier);

            // Initialize workflow using WorkflowService (starts at AP - Phase Antenne)
            workflowService.initializeWorkflow(savedDossier, utilisateur);

            // Create audit trail
            createAuditTrail("CREATION_DOSSIER", savedDossier, utilisateur, 
                    "Création d'un nouveau dossier avec SABA: " + savedDossier.getSaba());

            RecepisseDossierDTO recepisse = generateRecepisse(savedDossier, request);

            log.info("Dossier créé avec succès - ID: {}, SABA: {}", savedDossier.getId(), savedDossier.getSaba());

            return CreateDossierResponse.builder()
                    .dossierId(savedDossier.getId())
                    .numeroDossier(savedDossier.getNumeroDossier())
                    .statut("CREE")
                    .message("Dossier créé avec succès")
                    .recepisse(recepisse)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la création du dossier", e);
            throw new RuntimeException("Erreur lors de la création du dossier: " + e.getMessage());
        }
    }

    /**
     * Update an existing dossier
     */
    @Transactional
    public UpdateDossierResponse updateDossier(UpdateDossierRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Check if user can edit this dossier
            if (!canEditDossier(dossier, utilisateur)) {
                throw new RuntimeException("Vous n'avez pas l'autorisation de modifier ce dossier");
            }

            // Update agriculteur information
            if (request.getAgriculteur() != null) {
                updateAgriculteur(dossier.getAgriculteur(), request.getAgriculteur());
            }

            // Update dossier information if provided
            if (request.getDossier() != null) {
                updateDossierInfo(dossier, request.getDossier());
            }

            dossierRepository.save(dossier);

            createAuditTrail("MODIFICATION_DOSSIER", dossier, utilisateur, 
                    "Modification du dossier " + dossier.getNumeroDossier());

            return UpdateDossierResponse.builder()
                    .dossierId(dossier.getId())
                    .message("Dossier mis à jour avec succès")
                    .success(true)
                    .lastUpdated(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du dossier", e);
            throw new RuntimeException("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    /**
     * Get dossier for editing
     */
    public DossierEditResponse getDossierForEdit(Long dossierId, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            Dossier dossier = dossierRepository.findById(dossierId)
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            if (!canEditDossier(dossier, utilisateur)) {
                throw new RuntimeException("Vous n'avez pas l'autorisation de modifier ce dossier");
            }

            // Get all required data for editing
            List<Rubrique> rubriques = rubriqueRepository.findAll();
            List<SimplifiedRubriqueDTO> rubriqueDTOs = rubriques.stream()
                    .map(this::mapToSimplifiedRubriqueDTO)
                    .collect(Collectors.toList());

            List<GeographicDTO> provinces = provinceRepository.findAll().stream()
                    .map(p -> GeographicDTO.builder()
                            .id(p.getId())
                            .designation(p.getDesignation())
                            .build())
                    .collect(Collectors.toList());

            List<AntenneInfoDTO> antennes = antenneRepository.findAll().stream()
                    .map(a -> AntenneInfoDTO.builder()
                            .id(a.getId())
                            .designation(a.getDesignation())
                            .cdaNom(a.getCda() != null ? a.getCda().getDescription() : "N/A")
                            .cdaId(a.getCda() != null ? a.getCda().getId() : null)
                            .build())
                    .collect(Collectors.toList());

            return DossierEditResponse.builder()
                    .dossierId(dossier.getId())
                    .agriculteur(AgriculteurInfoDTO.builder()
                            .cin(dossier.getAgriculteur().getCin())
                            .nom(dossier.getAgriculteur().getNom())
                            .prenom(dossier.getAgriculteur().getPrenom())
                            .telephone(dossier.getAgriculteur().getTelephone())
                            .communeRuraleId(dossier.getAgriculteur().getCommuneRurale() != null ? 
                                    dossier.getAgriculteur().getCommuneRurale().getId() : null)
                            .douarId(dossier.getAgriculteur().getDouar() != null ? 
                                    dossier.getAgriculteur().getDouar().getId() : null)
                            .build())
                    .dossier(DossierInfoDTO.builder()
                            .saba(dossier.getSaba())
                            .reference(dossier.getReference())
                            .sousRubriqueId(dossier.getSousRubrique().getId())
                            .antenneId(dossier.getAntenne().getId())
                            .build())
                    .currentStatus(dossier.getStatus().name())
                    .canEdit(true)
                    .lastModified(dossier.getDateCreation().toString())
                    .rubriques(rubriqueDTOs)
                    .provinces(provinces)
                    .antennes(antennes)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la récupération du dossier pour édition", e);
            throw new RuntimeException("Erreur lors de la récupération: " + e.getMessage());
        }
    }

    // Geographic methods
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

    // Private helper methods
    private boolean canEditDossier(Dossier dossier, Utilisateur utilisateur) {
        // Only AGENT_ANTENNE can edit, and only DRAFT or RETURNED_FOR_COMPLETION status
        if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_ANTENNE)) {
            return false;
        }

        // Must be from same antenne
        if (!dossier.getAntenne().getId().equals(utilisateur.getAntenne().getId())) {
            return false;
        }

        // Can only edit if DRAFT or RETURNED_FOR_COMPLETION
        return dossier.getStatus() == Dossier.DossierStatus.DRAFT ||
               dossier.getStatus() == Dossier.DossierStatus.RETURNED_FOR_COMPLETION;
    }

    private void updateAgriculteur(Agriculteur agriculteur, AgriculteurInfoDTO agriculteurInfo) {
        if (agriculteurInfo.getNom() != null) {
            agriculteur.setNom(agriculteurInfo.getNom());
        }
        if (agriculteurInfo.getPrenom() != null) {
            agriculteur.setPrenom(agriculteurInfo.getPrenom());
        }
        if (agriculteurInfo.getTelephone() != null) {
            agriculteur.setTelephone(agriculteurInfo.getTelephone());
        }
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
        agriculteurRepository.save(agriculteur);
    }

    private void updateDossierInfo(Dossier dossier, DossierInfoDTO dossierInfo) {
        if (dossierInfo.getSousRubriqueId() != null && 
            !dossierInfo.getSousRubriqueId().equals(dossier.getSousRubrique().getId())) {
            SousRubrique sousRubrique = sousRubriqueRepository.findById(dossierInfo.getSousRubriqueId())
                    .orElseThrow(() -> new RuntimeException("Sous-rubrique non trouvée"));
            dossier.setSousRubrique(sousRubrique);
        }
        if (dossierInfo.getAntenneId() != null && 
            !dossierInfo.getAntenneId().equals(dossier.getAntenne().getId())) {
            Antenne antenne = antenneRepository.findById(dossierInfo.getAntenneId())
                    .orElseThrow(() -> new RuntimeException("Antenne non trouvée"));
            dossier.setAntenne(antenne);
        }
    }

    private SimplifiedRubriqueDTO mapToSimplifiedRubriqueDTO(Rubrique rubrique) {
        List<SimplifiedSousRubriqueDTO> sousRubriquesDTOs = rubrique.getSousRubriques().stream()
                .map(sr -> SimplifiedSousRubriqueDTO.builder()
                        .id(sr.getId())
                        .designation(sr.getDesignation())
                        .description(sr.getDescription())
                        .documentsRequis(getDocumentNames(sr.getId()))
                        .build())
                .collect(Collectors.toList());

        return SimplifiedRubriqueDTO.builder()
                .id(rubrique.getId())
                .designation(rubrique.getDesignation())
                .description(rubrique.getDescription())
                .sousRubriques(sousRubriquesDTOs)
                .build();
    }

    private List<String> getDocumentNames(Long sousRubriqueId) {
        return documentRequisRepository.findBySousRubriqueId(sousRubriqueId).stream()
                .map(DocumentRequis::getNomDocument)
                .collect(Collectors.toList());
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

    private String generateReference(Antenne antenne, SousRubrique sousRubrique) {
        String annee = String.valueOf(LocalDate.now().getYear());
        String antenneCode = String.format("%03d", antenne.getId());
        String sousRubriqueCode = String.format("%02d", sousRubrique.getId());
        long sequence = dossierRepository.count() + 1;
        String seqCode = String.format("%06d", sequence);

        return String.format("DOS-%s-%s-%s-%s", annee, antenneCode, sousRubriqueCode, seqCode);
    }

    private String generateNumeroDossier() {
        long count = dossierRepository.count() + 1;
        int year = LocalDate.now().getYear();
        return String.format("%d-%06d", year, count);
    }

    private String generateSabaNumber() {
        long count = dossierRepository.count() + 1;
        int year = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();

        String sequence = String.format("%04d%02d", count, month);
        String yearStr = String.valueOf(year);
        String finalSeq = String.format("%03d", (count % 1000) + 1);

        return String.format("%s/%s/%s", sequence, yearStr, finalSeq);
    }

    private RecepisseDossierDTO generateRecepisse(Dossier dossier, CreateDossierRequest request) {
        String numeroRecepisse = "R-" + LocalDate.now().getYear() + "-" +
                String.format("%06d", System.currentTimeMillis() % 1000000);

        return RecepisseDossierDTO.builder()
                .numeroRecepisse(numeroRecepisse)
                .dateDepot(LocalDate.now())
                .nomComplet(dossier.getAgriculteur().getPrenom() + " " + dossier.getAgriculteur().getNom())
                .cin(dossier.getAgriculteur().getCin())
                .telephone(dossier.getAgriculteur().getTelephone())
                .typeProduit(dossier.getSousRubrique().getDesignation())
                .saba(dossier.getSaba())
                .reference(dossier.getReference())
                .montantDemande(request.getDossier().getMontantDemande())
                .antenneName(dossier.getAntenne().getDesignation())
                .cdaName(dossier.getAntenne().getCda() != null ? dossier.getAntenne().getCda().getDescription() : "N/A")
                .numeroSerie(String.format("S-%06d", System.currentTimeMillis() % 1000000))
                .dateEmission(LocalDateTime.now())
                .build();
    }

    private void createAuditTrail(String action, Dossier dossier, Utilisateur utilisateur, String description) {
        AuditTrail auditTrail = new AuditTrail();
        auditTrail.setAction(action);
        auditTrail.setEntite("Dossier");
        auditTrail.setEntiteId(dossier.getId());
        auditTrail.setDateAction(LocalDateTime.now());
        auditTrail.setUtilisateur(utilisateur);
        auditTrail.setDescription(description);
        auditTrailRepository.save(auditTrail);
    }
}