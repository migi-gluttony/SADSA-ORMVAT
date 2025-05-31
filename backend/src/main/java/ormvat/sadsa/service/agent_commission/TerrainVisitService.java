package ormvat.sadsa.service.agent_commission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ormvat.sadsa.dto.agent_commission.TerrainVisitDTOs.*;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerrainVisitService {

    private final VisiteTerrainRepository visiteTerrainRepository;
    private final PhotoVisiteRepository photoVisiteRepository;
    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final HistoriqueWorkflowRepository historiqueWorkflowRepository;
    private final AuditTrailRepository auditTrailRepository;

    private static final String UPLOAD_DIR = "uploads/terrain-visits/";

    /**
     * Get terrain visits for commission agent
     */
    public VisitListResponse getTerrainVisits(Integer page, Integer size, String statut, 
                                             String searchTerm, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_COMMISSION)) {
                throw new RuntimeException("Accès non autorisé");
            }

            // Get all terrain visits for this commission agent
            List<VisiteTerrain> allVisits = visiteTerrainRepository.findByUtilisateurCommissionId(utilisateur.getId());

            // Apply filters
            List<VisiteTerrain> filteredVisits = allVisits.stream()
                    .filter(v -> statut == null || matchesStatus(v, statut))
                    .filter(v -> searchTerm == null || matchesSearchTerm(v, searchTerm))
                    .collect(Collectors.toList());

            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, filteredVisits.size());
            List<VisiteTerrain> paginatedVisits = filteredVisits.subList(start, end);

            // Convert to DTOs
            List<VisiteTerrainSummaryDTO> visitSummaries = paginatedVisits.stream()
                    .map(this::mapToVisiteTerrainSummaryDTO)
                    .collect(Collectors.toList());

            // Calculate statistics
            VisitStatisticsDTO statistics = calculateVisitStatistics(allVisits);

            return VisitListResponse.builder()
                    .visites(visitSummaries)
                    .totalCount((long) filteredVisits.size())
                    .currentPage(page)
                    .pageSize(size)
                    .totalPages((int) Math.ceil((double) filteredVisits.size() / size))
                    .statistics(statistics)
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des visites terrain", e);
            throw new RuntimeException("Erreur lors de la récupération: " + e.getMessage());
        }
    }

    /**
     * Get terrain visit detail
     */
    public VisiteTerrainResponse getTerrainVisitDetail(Long visiteId, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            VisiteTerrain visite = visiteTerrainRepository.findById(visiteId)
                    .orElseThrow(() -> new RuntimeException("Visite terrain non trouvée"));

            // Check access permission
            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_COMMISSION) &&
                !utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
                throw new RuntimeException("Accès non autorisé");
            }

            return mapToVisiteTerrainResponse(visite, utilisateur);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération du détail de la visite terrain", e);
            throw new RuntimeException("Erreur lors de la récupération: " + e.getMessage());
        }
    }

    /**
     * Schedule a terrain visit
     */
    @Transactional
    public TerrainVisitActionResponse scheduleTerrainVisit(ScheduleVisitRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_COMMISSION)) {
                throw new RuntimeException("Seul un agent commission peut programmer une visite terrain");
            }

            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Check if dossier is in the right status for terrain visit
            if (!dossier.getStatus().equals(Dossier.DossierStatus.SUBMITTED) &&
                !dossier.getStatus().equals(Dossier.DossierStatus.IN_REVIEW)) {
                throw new RuntimeException("Le dossier n'est pas dans le bon statut pour une visite terrain");
            }

            // Create new terrain visit
            VisiteTerrain visite = new VisiteTerrain();
            visite.setDossier(dossier);
            visite.setUtilisateurCommission(utilisateur);
            visite.setDateVisite(request.getDateVisite());
            visite.setObservations(request.getObservations());
            visite.setCoordonneesGPS(request.getCoordonneesGPS());
            visite.setRecommandations(request.getRecommandations());
            visite.setApprouve(null); // Not decided yet

            visite = visiteTerrainRepository.save(visite);

            // Update dossier status to IN_REVIEW
            dossier.setStatus(Dossier.DossierStatus.IN_REVIEW);
            dossierRepository.save(dossier);

            // Update workflow
            updateWorkflowForTerrainVisit(dossier, utilisateur, "Visite terrain programmée");

            // Create audit trail
            createAuditTrail("VISITE_TERRAIN_PROGRAMMEE", dossier, utilisateur, 
                    "Visite terrain programmée pour le " + request.getDateVisite());

            return TerrainVisitActionResponse.builder()
                    .success(true)
                    .message("Visite terrain programmée avec succès")
                    .visiteId(visite.getId())
                    .newStatut("VISITE_PROGRAMMEE")
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la programmation de la visite terrain", e);
            throw new RuntimeException("Erreur lors de la programmation: " + e.getMessage());
        }
    }

    /**
     * Complete a terrain visit
     */
    @Transactional
    public TerrainVisitActionResponse completeTerrainVisit(CompleteVisitRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_COMMISSION)) {
                throw new RuntimeException("Seul un agent commission peut compléter une visite terrain");
            }

            VisiteTerrain visite = visiteTerrainRepository.findById(request.getVisiteId())
                    .orElseThrow(() -> new RuntimeException("Visite terrain non trouvée"));

            // Check if user has permission to complete this visit
            if (!visite.getUtilisateurCommission().getId().equals(utilisateur.getId()) &&
                !utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
                throw new RuntimeException("Vous n'avez pas l'autorisation de compléter cette visite");
            }

            // Update visit details
            visite.setDateConstat(request.getDateConstat());
            visite.setObservations(request.getObservations());
            visite.setRecommandations(request.getRecommandations());
            visite.setApprouve(request.getApprouve());
            visite.setCoordonneesGPS(request.getCoordonneesGPS());

            visite = visiteTerrainRepository.save(visite);

            // Update dossier status based on approval
            Dossier dossier = visite.getDossier();
            if (request.getApprouve()) {
                dossier.setStatus(Dossier.DossierStatus.APPROVED);
            } else {
                dossier.setStatus(Dossier.DossierStatus.REJECTED);
            }
            dossierRepository.save(dossier);

            // Update workflow
            updateWorkflowForVisitCompletion(dossier, utilisateur, request.getApprouve(), request.getObservations());

            // Create audit trail
            String action = request.getApprouve() ? "VISITE_TERRAIN_APPROUVEE" : "VISITE_TERRAIN_REJETEE";
            createAuditTrail(action, dossier, utilisateur, request.getObservations());

            return TerrainVisitActionResponse.builder()
                    .success(true)
                    .message(request.getApprouve() ? "Visite terrain approuvée" : "Visite terrain rejetée")
                    .visiteId(visite.getId())
                    .newStatut(request.getApprouve() ? "APPROUVE" : "REJETE")
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la finalisation de la visite terrain", e);
            throw new RuntimeException("Erreur lors de la finalisation: " + e.getMessage());
        }
    }

    /**
     * Upload photos for terrain visit
     */
    @Transactional
    public TerrainVisitActionResponse uploadVisitPhotos(Long visiteId, List<MultipartFile> files,
                                                       List<String> descriptions, List<String> coordonnees,
                                                       String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            VisiteTerrain visite = visiteTerrainRepository.findById(visiteId)
                    .orElseThrow(() -> new RuntimeException("Visite terrain non trouvée"));

            // Check permission
            if (!visite.getUtilisateurCommission().getId().equals(utilisateur.getId()) &&
                !utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
                throw new RuntimeException("Vous n'avez pas l'autorisation d'ajouter des photos à cette visite");
            }

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            int photosUploaded = 0;
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String description = descriptions != null && i < descriptions.size() ? descriptions.get(i) : "";
                String gps = coordonnees != null && i < coordonnees.size() ? coordonnees.get(i) : "";

                if (!file.isEmpty()) {
                    String fileName = generateUniqueFileName(file.getOriginalFilename());
                    Path filePath = uploadPath.resolve(fileName);
                    Files.copy(file.getInputStream(), filePath);

                    // Create photo record
                    PhotoVisite photo = new PhotoVisite();
                    photo.setVisiteTerrain(visite);
                    photo.setNomFichier(file.getOriginalFilename());
                    photo.setCheminFichier(filePath.toString());
                    photo.setDescription(description);
                    photo.setCoordonneesGPS(gps);
                    photo.setDatePrise(LocalDateTime.now());

                    photoVisiteRepository.save(photo);
                    photosUploaded++;
                }
            }

            return TerrainVisitActionResponse.builder()
                    .success(true)
                    .message(photosUploaded + " photo(s) uploadée(s) avec succès")
                    .visiteId(visiteId)
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (IOException e) {
            log.error("Erreur IO lors de l'upload des photos", e);
            throw new RuntimeException("Erreur lors de l'upload des photos: " + e.getMessage());
        } catch (Exception e) {
            log.error("Erreur lors de l'upload des photos", e);
            throw new RuntimeException("Erreur lors de l'upload: " + e.getMessage());
        }
    }

    /**
     * Update terrain visit details
     */
    @Transactional
    public TerrainVisitActionResponse updateTerrainVisit(Long visiteId, ScheduleVisitRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            VisiteTerrain visite = visiteTerrainRepository.findById(visiteId)
                    .orElseThrow(() -> new RuntimeException("Visite terrain non trouvée"));

            // Check permission
            if (!visite.getUtilisateurCommission().getId().equals(utilisateur.getId()) &&
                !utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
                throw new RuntimeException("Vous n'avez pas l'autorisation de modifier cette visite");
            }

            // Update visit details
            visite.setDateVisite(request.getDateVisite());
            visite.setObservations(request.getObservations());
            visite.setCoordonneesGPS(request.getCoordonneesGPS());
            visite.setRecommandations(request.getRecommandations());

            visite = visiteTerrainRepository.save(visite);

            // Create audit trail
            createAuditTrail("VISITE_TERRAIN_MODIFIEE", visite.getDossier(), utilisateur,
                    "Visite terrain reprogrammée pour le " + request.getDateVisite());

            return TerrainVisitActionResponse.builder()
                    .success(true)
                    .message("Visite terrain mise à jour avec succès")
                    .visiteId(visiteId)
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de la visite terrain", e);
            throw new RuntimeException("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    // Private helper methods

    private boolean matchesStatus(VisiteTerrain visite, String statut) {
        String visiteStatus = getVisiteStatus(visite);
        return visiteStatus.equalsIgnoreCase(statut);
    }

    private boolean matchesSearchTerm(VisiteTerrain visite, String searchTerm) {
        String term = searchTerm.toLowerCase();
        return (visite.getDossier().getReference() != null && visite.getDossier().getReference().toLowerCase().contains(term)) ||
                (visite.getDossier().getSaba() != null && visite.getDossier().getSaba().toLowerCase().contains(term)) ||
                (visite.getDossier().getAgriculteur().getNom() != null && visite.getDossier().getAgriculteur().getNom().toLowerCase().contains(term)) ||
                (visite.getDossier().getAgriculteur().getPrenom() != null && visite.getDossier().getAgriculteur().getPrenom().toLowerCase().contains(term));
    }

    private String getVisiteStatus(VisiteTerrain visite) {
        if (visite.getDateConstat() != null) {
            return visite.getApprouve() != null ? 
                    (visite.getApprouve() ? "APPROUVEE" : "REJETEE") : 
                    "COMPLETEE";
        } else if (visite.getDateVisite() != null) {
            return "PROGRAMMEE";
        }
        return "NOUVELLE";
    }

    private VisiteTerrainSummaryDTO mapToVisiteTerrainSummaryDTO(VisiteTerrain visite) {
        return VisiteTerrainSummaryDTO.builder()
                .id(visite.getId())
                .dateVisite(visite.getDateVisite())
                .dateConstat(visite.getDateConstat())
                .approuve(visite.getApprouve())
                .statut(getVisiteStatus(visite))
                .dossierId(visite.getDossier().getId())
                .dossierReference(visite.getDossier().getReference())
                .saba(visite.getDossier().getSaba())
                .agriculteurNom(visite.getDossier().getAgriculteur().getNom())
                .agriculteurPrenom(visite.getDossier().getAgriculteur().getPrenom())
                .agriculteurTelephone(visite.getDossier().getAgriculteur().getTelephone())
                .sousRubriqueDesignation(visite.getDossier().getSousRubrique().getDesignation())
                .antenneDesignation(visite.getDossier().getAntenne().getDesignation())
                .joursRestants(calculateDaysRemaining(visite))
                .enRetard(isVisiteEnRetard(visite))
                .canComplete(visite.getDateConstat() == null)
                .canModify(visite.getDateConstat() == null)
                .build();
    }

    private VisiteTerrainResponse mapToVisiteTerrainResponse(VisiteTerrain visite, Utilisateur currentUser) {
        List<PhotoVisiteDTO> photos = photoVisiteRepository.findByVisiteTerrainId(visite.getId())
                .stream()
                .map(this::mapToPhotoVisiteDTO)
                .collect(Collectors.toList());

        return VisiteTerrainResponse.builder()
                .id(visite.getId())
                .dateVisite(visite.getDateVisite())
                .dateConstat(visite.getDateConstat())
                .observations(visite.getObservations())
                .recommandations(visite.getRecommandations())
                .approuve(visite.getApprouve())
                .coordonneesGPS(visite.getCoordonneesGPS())
                .statut(getVisiteStatus(visite))
                .dossierId(visite.getDossier().getId())
                .dossierReference(visite.getDossier().getReference())
                .agriculteurNom(visite.getDossier().getAgriculteur().getNom())
                .agriculteurPrenom(visite.getDossier().getAgriculteur().getPrenom())
                .agriculteurTelephone(visite.getDossier().getAgriculteur().getTelephone())
                .sousRubriqueDesignation(visite.getDossier().getSousRubrique().getDesignation())
                .antenneDesignation(visite.getDossier().getAntenne().getDesignation())
                .utilisateurCommissionNom(visite.getUtilisateurCommission().getPrenom() + " " + visite.getUtilisateurCommission().getNom())
                .photos(photos)
                .canSchedule(false) // Already scheduled
                .canComplete(visite.getDateConstat() == null && canUserCompleteVisit(visite, currentUser))
                .canModify(visite.getDateConstat() == null && canUserModifyVisit(visite, currentUser))
                .build();
    }

    private PhotoVisiteDTO mapToPhotoVisiteDTO(PhotoVisite photo) {
        return PhotoVisiteDTO.builder()
                .id(photo.getId())
                .nomFichier(photo.getNomFichier())
                .cheminFichier(photo.getCheminFichier())
                .description(photo.getDescription())
                .coordonneesGPS(photo.getCoordonneesGPS())
                .datePrise(photo.getDatePrise())
                .build();
    }

    private VisitStatisticsDTO calculateVisitStatistics(List<VisiteTerrain> visites) {
        long total = visites.size();
        long enAttente = visites.stream().filter(v -> v.getDateConstat() == null).count();
        long realisees = visites.stream().filter(v -> v.getDateConstat() != null).count();
        long approuvees = visites.stream().filter(v -> Boolean.TRUE.equals(v.getApprouve())).count();
        long rejetees = visites.stream().filter(v -> Boolean.FALSE.equals(v.getApprouve())).count();
        long enRetard = visites.stream().filter(this::isVisiteEnRetard).count();

        double tauxApprobation = realisees > 0 ? (double) approuvees / realisees * 100 : 0;

        return VisitStatisticsDTO.builder()
                .totalVisites(total)
                .visitesEnAttente(enAttente)
                .visitesRealisees(realisees)
                .visitesApprouvees(approuvees)
                .visitesRejetees(rejetees)
                .visitesEnRetard(enRetard)
                .tauxApprobation(tauxApprobation)
                .build();
    }

    private Integer calculateDaysRemaining(VisiteTerrain visite) {
        if (visite.getDateVisite() == null) return null;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.now(), visite.getDateVisite());
    }

    private Boolean isVisiteEnRetard(VisiteTerrain visite) {
        if (visite.getDateVisite() == null || visite.getDateConstat() != null) return false;
        return visite.getDateVisite().isBefore(java.time.LocalDate.now());
    }

    private boolean canUserCompleteVisit(VisiteTerrain visite, Utilisateur user) {
        return user.getRole().equals(Utilisateur.UserRole.AGENT_COMMISSION) ||
                user.getRole().equals(Utilisateur.UserRole.ADMIN);
    }

    private boolean canUserModifyVisit(VisiteTerrain visite, Utilisateur user) {
        return (user.getRole().equals(Utilisateur.UserRole.AGENT_COMMISSION) &&
                visite.getUtilisateurCommission().getId().equals(user.getId())) ||
                user.getRole().equals(Utilisateur.UserRole.ADMIN);
    }

    private String generateUniqueFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    private void updateWorkflowForTerrainVisit(Dossier dossier, Utilisateur utilisateur, String commentaire) {
        List<WorkflowInstance> workflows = workflowInstanceRepository.findByDossierId(dossier.getId());
        
        if (!workflows.isEmpty()) {
            WorkflowInstance current = workflows.get(0);
            current.setEtapeDesignation("Commission AHA-AF - Visite Terrain");
            current.setEmplacementActuel(WorkflowInstance.EmplacementType.COMMISSION_AHA_AF);
            current.setDateEntree(LocalDateTime.now());
            workflowInstanceRepository.save(current);
        }

        HistoriqueWorkflow history = new HistoriqueWorkflow();
        history.setDossier(dossier);
        history.setEtapeDesignation("Visite Terrain Programmée");
        history.setEmplacementType(WorkflowInstance.EmplacementType.COMMISSION_AHA_AF);
        history.setDateEntree(LocalDateTime.now());
        history.setUtilisateur(utilisateur);
        history.setCommentaire(commentaire);
        historiqueWorkflowRepository.save(history);
    }

    private void updateWorkflowForVisitCompletion(Dossier dossier, Utilisateur utilisateur, Boolean approuve, String commentaire) {
        List<WorkflowInstance> workflows = workflowInstanceRepository.findByDossierId(dossier.getId());
        
        if (!workflows.isEmpty()) {
            WorkflowInstance current = workflows.get(0);
            current.setEtapeDesignation(approuve ? "Terrain Approuvé" : "Terrain Rejeté");
            current.setEmplacementActuel(WorkflowInstance.EmplacementType.COMMISSION_AHA_AF);
            current.setDateEntree(LocalDateTime.now());
            workflowInstanceRepository.save(current);
        }

        HistoriqueWorkflow history = new HistoriqueWorkflow();
        history.setDossier(dossier);
        history.setEtapeDesignation(approuve ? "Terrain Approuvé" : "Terrain Rejeté");
        history.setEmplacementType(WorkflowInstance.EmplacementType.COMMISSION_AHA_AF);
        history.setDateEntree(LocalDateTime.now());
        history.setUtilisateur(utilisateur);
        history.setCommentaire(commentaire);
        historiqueWorkflowRepository.save(history);
    }

    private void createAuditTrail(String action, Dossier dossier, Utilisateur utilisateur, String description) {
        AuditTrail audit = new AuditTrail();
        audit.setAction(action);
        audit.setEntite("Dossier");
        audit.setEntiteId(dossier.getId());
        audit.setDateAction(LocalDateTime.now());
        audit.setUtilisateur(utilisateur);
        audit.setDescription(description);
        auditTrailRepository.save(audit);
    }
}