package ormvat.sadsa.service.agent_antenne;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ormvat.sadsa.dto.agent_antenne.DossierCreationDTOs.*;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;

import java.time.LocalDate;
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
    private final HistoriqueRepository historiqueRepository;
    private final EmplacementRepository emplacementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ProvinceRepository provinceRepository;
    private final CercleRepository cercleRepository;
    private final CommuneRuraleRepository communeRuraleRepository;
    private final DouarRepository douarRepository;
    private final DocumentRequisRepository documentRequisRepository;

    /**
     * Get all initialization data needed for dossier creation
     */
    public InitializationDataResponse getInitializationData(String userEmail) {
        try {
            // Get user and their CDA
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            CDA userCDA = utilisateur.getCda();
            if (userCDA == null) {
                throw new RuntimeException("L'utilisateur n'est associé à aucun CDA");
            }

            // Get all rubriques with sous-rubriques and documents
            List<Rubrique> rubriques = rubriqueRepository.findAll();
            List<SimplifiedRubriqueDTO> rubriqueDTOs = rubriques.stream()
                    .map(this::mapToSimplifiedRubriqueDTO)
                    .collect(Collectors.toList());

            // Get geographic data
            List<GeographicDTO> provinces = provinceRepository.findAll().stream()
                    .map(p -> GeographicDTO.builder()
                            .id(p.getId())
                            .designation(p.getDesignation())
                            .build())
                    .collect(Collectors.toList());

            // Generate SABA number
            String sabaNumber = generateSabaNumber();

            return InitializationDataResponse.builder()
                    .userCDA(CDAInfoDTO.builder()
                            .id(userCDA.getId())
                            .description(userCDA.getDescription())
                            .antenneNom(userCDA.getAntenne() != null ? userCDA.getAntenne().getDesignation() : "N/A")
                            .antenneId(userCDA.getAntenne() != null ? userCDA.getAntenne().getId() : null)
                            .build())
                    .rubriques(rubriqueDTOs)
                    .provinces(provinces)
                    .generatedSaba(sabaNumber)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors du chargement des données d'initialisation", e);
            throw new RuntimeException("Erreur lors du chargement des données: " + e.getMessage());
        }
    }

    /**
     * Get cercles by province
     */
    public List<GeographicDTO> getCerclesByProvince(Long provinceId) {
        return cercleRepository.findByProvinceId(provinceId).stream()
                .map(c -> GeographicDTO.builder()
                        .id(c.getId())
                        .designation(c.getDesignation())
                        .parentId(c.getProvince().getId())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get communes by cercle
     */
    public List<GeographicDTO> getCommunesByCircle(Long cercleId) {
        return communeRuraleRepository.findByCercleId(cercleId).stream()
                .map(c -> GeographicDTO.builder()
                        .id(c.getId())
                        .designation(c.getDesignation())
                        .parentId(c.getCercle().getId())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get douars by commune
     */
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
     * Create a new dossier with all necessary audit trail
     */
    @Transactional
    public CreateDossierResponse createDossier(CreateDossierRequest request, String userEmail) {
        try {
            log.info("Début création dossier pour l'utilisateur: {}", userEmail);

            // Get user and CDA
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            CDA cda = utilisateur.getCda();
            if (cda == null) {
                throw new RuntimeException("L'utilisateur n'est associé à aucun CDA");
            }

            // Validate and get or create agriculteur
            Agriculteur agriculteur = getOrCreateAgriculteur(request.getAgriculteur());

            // Get sous-rubrique
            SousRubrique sousRubrique = sousRubriqueRepository.findById(request.getDossier().getSousRubriqueId())
                    .orElseThrow(() -> new RuntimeException("Sous-rubrique non trouvée"));

            // Get initial etape (Phase Antenne)
            Emplacement phaseAntenne = emplacementRepository.findByDesignation("Phase Antenne");
            if (phaseAntenne == null) {
                throw new RuntimeException("Emplacement 'Phase Antenne' non trouvé");
            }

            Etape etapeInitiale = etapeRepository.findByDesignation("Phase Antenne");
            if (etapeInitiale == null) {
                throw new RuntimeException("Étape initiale non trouvée");
            }

            // Create dossier
            Dossier dossier = new Dossier();
            dossier.setSaba(request.getDossier().getSaba());
            dossier.setReference(generateReference(cda, sousRubrique));
            dossier.setAgriculteur(agriculteur);
            dossier.setCda(cda);
            dossier.setSousRubrique(sousRubrique);
            dossier.setEtapeActuelle(etapeInitiale);

            Dossier savedDossier = dossierRepository.save(dossier);

            // Create historique entry
            Historique historique = new Historique();
            historique.setDossier(savedDossier);
            historique.setEmplacement(phaseAntenne);
            historique.setDateReception(new Date());
            historiqueRepository.save(historique);

            // Create trace entry
            Trace trace = new Trace();
            trace.setDossier(savedDossier);
            trace.setUtilisateur(utilisateur);
            trace.setAction("CREATION_DOSSIER");
            trace.setDateAction(new Date());
            traceRepository.save(trace);

            // Generate recepisse
            RecepisseDossierDTO recepisse = generateRecepisse(savedDossier, request);

            log.info("Dossier créé avec succès - ID: {}, SABA: {}", savedDossier.getId(), savedDossier.getSaba());

            return CreateDossierResponse.builder()
                    .dossierId(savedDossier.getId())
                    .numeroDossier(savedDossier.getReference())
                    .statut("CREE")
                    .message("Dossier créé avec succès")
                    .recepisse(recepisse)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la création du dossier", e);
            throw new RuntimeException("Erreur lors de la création du dossier: " + e.getMessage());
        }
    }

    // Private helper methods
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

    private String generateReference(CDA cda, SousRubrique sousRubrique) {
        String annee = String.valueOf(LocalDate.now().getYear());
        String cdaCode = String.format("%03d", cda.getId());
        String sousRubriqueCode = String.format("%02d", sousRubrique.getId());
        long sequence = dossierRepository.count() + 1;
        String seqCode = String.format("%06d", sequence);

        return String.format("DOS-%s-%s-%s-%s", annee, cdaCode, sousRubriqueCode, seqCode);
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
                .montantDemande(request.getDossier().getMontantDemande())
                .cdaNom(dossier.getCda().getDescription())
                .antenne(dossier.getCda().getAntenne() != null ? dossier.getCda().getAntenne().getDesignation() : "N/A")
                .build();
    }
}