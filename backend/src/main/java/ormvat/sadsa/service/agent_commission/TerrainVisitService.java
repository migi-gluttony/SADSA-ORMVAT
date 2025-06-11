package ormvat.sadsa.service.agent_commission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ormvat.sadsa.dto.agent_commission.TerrainVisitDTOs.*;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;
import ormvat.sadsa.service.workflow.WorkflowService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerrainVisitService {
    
    private final VisiteTerrainRepository visiteTerrainRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditTrailRepository auditTrailRepository;

    @Value("${app.upload.dir:./uploads/ormvat_sadsa}")
    private String uploadDir;

    // Optional WorkflowService injection to avoid circular dependencies
    @Autowired(required = false)
    private WorkflowService workflowService;

    /**
     * Get dashboard summary for commission agent
     */
    public DashboardSummaryDTO getDashboardSummary(String userEmail) {
        try {
            Utilisateur utilisateur = getCommissionAgent(userEmail);
            List<VisiteTerrain> allVisits = visiteTerrainRepository.findByUtilisateurCommissionId(utilisateur.getId());
            
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            
            long visitesAujourdHui = allVisits.stream()
                    .filter(v -> v.getDateVisite() != null && v.getDateVisite().equals(today))
                    .count();
                    
            long visitesDemain = allVisits.stream()
                    .filter(v -> v.getDateVisite() != null && v.getDateVisite().equals(tomorrow))
                    .count();
                    
            long visitesEnRetard = allVisits.stream()
                    .filter(this::isVisiteOverdue)
                    .count();
                    
            long visitesEnAttente = allVisits.stream()
                    .filter(v -> v.getDateConstat() == null)
                    .count();
            
            // Get upcoming visits (next 7 days)
            List<VisiteTerrainSummaryDTO> prochaines = allVisits.stream()
                    .filter(v -> v.getDateVisite() != null && 
                            v.getDateVisite().isAfter(today) && 
                            v.getDateVisite().isBefore(today.plusDays(8)) &&
                            v.getDateConstat() == null)
                    .map(this::mapToVisiteTerrainSummaryDTO)
                    .sorted((a, b) -> a.getDateVisite().compareTo(b.getDateVisite()))
                    .limit(5)
                    .collect(Collectors.toList());
            
            // Get urgent visits (overdue or today)
            List<VisiteTerrainSummaryDTO> urgentes = allVisits.stream()
                    .filter(v -> (v.getDateVisite() != null && 
                            v.getDateVisite().isBefore(tomorrow) && 
                            v.getDateConstat() == null) || isVisiteOverdue(v))
                    .map(this::mapToVisiteTerrainSummaryDTO)
                    .sorted((a, b) -> a.getDateVisite().compareTo(b.getDateVisite()))
                    .limit(5)
                    .collect(Collectors.toList());
            
            VisitStatisticsDTO statistiques = calculateVisitStatistics(allVisits);
            
            return DashboardSummaryDTO.builder()
                    .visitesAujourdHui(visitesAujourdHui)
                    .visitesDemain(visitesDemain)
                    .visitesEnRetard(visitesEnRetard)
                    .visitesEnAttente(visitesEnAttente)
                    .prochaines(prochaines)
                    .urgentes(urgentes)
                    .statistiques(statistiques)
                    .build();
                    
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du tableau de bord", e);
            throw new RuntimeException("Erreur lors de la récupération: " + e.getMessage());
        }
    }

    /**
     * Get terrain visits with filtering and pagination
     */
    public VisitListResponse getTerrainVisits(VisitFilterRequest filterRequest, String userEmail) {
        try {
            Utilisateur utilisateur = getCommissionAgent(userEmail);
            
            // Filter visits based on user's team assignment
            List<VisiteTerrain> allVisits;
            if (utilisateur.getEquipeCommission() != null) {
                // Get visits only for this agent's team project type
                allVisits = visiteTerrainRepository.findByUtilisateurCommissionId(utilisateur.getId())
                        .stream()
                        .filter(visite -> {
                            Long rubriqueId = visite.getDossier().getSousRubrique().getRubrique().getId();
                            Utilisateur.EquipeCommission equipeRequise = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);
                            return equipeRequise.equals(utilisateur.getEquipeCommission());
                        })
                        .collect(Collectors.toList());
            } else {
                // Fallback: see all visits assigned to this agent (for admins or unassigned agents)
                allVisits = visiteTerrainRepository.findByUtilisateurCommissionId(utilisateur.getId());
            }

            // Apply filters
            List<VisiteTerrain> filteredVisits = applyFilters(allVisits, filterRequest);

            // Apply sorting
            filteredVisits = applySorting(filteredVisits, filterRequest);

            // Apply pagination
            int page = filterRequest.getPage() != null ? filterRequest.getPage() : 0;
            int size = filterRequest.getSize() != null ? filterRequest.getSize() : 20;
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
                    .availableStatuses(getAvailableStatuses())
                    .availableProjectTypes(getAvailableProjectTypes())
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
            Utilisateur utilisateur = getCommissionAgent(userEmail);
            VisiteTerrain visite = visiteTerrainRepository.findById(visiteId)
                    .orElseThrow(() -> new RuntimeException("Visite terrain non trouvée"));

            // Check access permission
            if (!visite.getUtilisateurCommission().getId().equals(utilisateur.getId()) &&
                !utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
                throw new RuntimeException("Accès non autorisé à cette visite");
            }

            // Additional check for team compatibility
            if (utilisateur.getEquipeCommission() != null) {
                Long rubriqueId = visite.getDossier().getSousRubrique().getRubrique().getId();
                Utilisateur.EquipeCommission equipeRequise = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);
                if (!equipeRequise.equals(utilisateur.getEquipeCommission()) && 
                    !utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
                    throw new RuntimeException("Cette visite n'est pas assignée à votre équipe (" + 
                            utilisateur.getEquipeCommission().getDisplayName() + ")");
                }
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
            Utilisateur utilisateur = getCommissionAgent(userEmail);
            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Check if dossier is in the right status for terrain visit
            if (!canScheduleTerrainVisit(dossier)) {
                throw new RuntimeException("Le dossier n'est pas dans le bon statut pour une visite terrain");
            }

            // Check if agent's team matches dossier's project type
            if (utilisateur.getEquipeCommission() != null) {
                Long rubriqueId = dossier.getSousRubrique().getRubrique().getId();
                Utilisateur.EquipeCommission equipeRequise = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);
                if (!equipeRequise.equals(utilisateur.getEquipeCommission()) && 
                    !utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
                    throw new RuntimeException("Ce dossier n'est pas assigné à votre équipe (" + 
                            utilisateur.getEquipeCommission().getDisplayName() + 
                            "). Il nécessite l'équipe " + equipeRequise.getDisplayName());
                }
            }

            // Create new terrain visit
            VisiteTerrain visite = new VisiteTerrain();
            visite.setDossier(dossier);
            visite.setUtilisateurCommission(utilisateur);
            visite.setDateVisite(request.getDateVisite());
            visite.setObservations(request.getObservations());
            visite.setCoordonneesGPS(request.getCoordonneesGPS());
            visite.setRecommandations(request.getRecommandations());
            visite.setApprouve(null);

            visite = visiteTerrainRepository.save(visite);

            // Update dossier status
            dossier.setStatus(Dossier.DossierStatus.IN_REVIEW);
            dossierRepository.save(dossier);

            // Create audit trail
            createAuditTrail("VISITE_TERRAIN_PROGRAMMEE", dossier, utilisateur, 
                    "Visite terrain programmée pour le " + request.getDateVisite() + 
                    " par " + (utilisateur.getEquipeCommission() != null ? utilisateur.getEquipeCommission().getDisplayName() : "Commission"));

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
     * Update visit notes (auto-save)
     */
    @Transactional
    public TerrainVisitActionResponse updateVisitNotes(UpdateVisitNotesRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = getCommissionAgent(userEmail);
            VisiteTerrain visite = visiteTerrainRepository.findById(request.getVisiteId())
                    .orElseThrow(() -> new RuntimeException("Visite terrain non trouvée"));

            // Check permission
            if (!canModifyVisit(visite, utilisateur)) {
                throw new RuntimeException("Vous n'avez pas l'autorisation de modifier cette visite");
            }

            // Update notes
            if (request.getObservations() != null) {
                visite.setObservations(request.getObservations());
            }
            if (request.getRecommandations() != null) {
                visite.setRecommandations(request.getRecommandations());
            }
            if (request.getCoordonneesGPS() != null) {
                visite.setCoordonneesGPS(request.getCoordonneesGPS());
            }

            visiteTerrainRepository.save(visite);

            // Only create audit trail for manual saves, not auto-saves
            if (!Boolean.TRUE.equals(request.getIsAutoSave())) {
                createAuditTrail("VISITE_TERRAIN_NOTES_MISES_A_JOUR", visite.getDossier(), utilisateur,
                        "Notes de visite mises à jour");
            }

            return TerrainVisitActionResponse.builder()
                    .success(true)
                    .message(Boolean.TRUE.equals(request.getIsAutoSave()) ? 
                            "Notes sauvegardées automatiquement" : "Notes mises à jour avec succès")
                    .visiteId(visite.getId())
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour des notes", e);
            throw new RuntimeException("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    /**
     * Complete a terrain visit with approval/rejection
     * ✅ ENHANCED: Now automatically advances workflow to next phase
     */
    @Transactional
    public TerrainVisitActionResponse completeTerrainVisit(CompleteVisitRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = getCommissionAgent(userEmail);
            VisiteTerrain visite = visiteTerrainRepository.findById(request.getVisiteId())
                    .orElseThrow(() -> new RuntimeException("Visite terrain non trouvée"));

            // Check permission
            if (!canCompleteVisit(visite, utilisateur)) {
                throw new RuntimeException("Vous n'avez pas l'autorisation de finaliser cette visite");
            }

            // Update visit details
            visite.setDateConstat(request.getDateConstat());
            visite.setObservations(request.getObservations());
            visite.setRecommandations(request.getRecommandations());
            visite.setApprouve(request.getApprouve());
            visite.setCoordonneesGPS(request.getCoordonneesGPS());
            
            // Set visit status
            if (request.getApprouve()) {
                visite.setStatutVisite(VisiteTerrain.StatutVisite.TERMINEE);
            } else {
                visite.setStatutVisite(VisiteTerrain.StatutVisite.TERMINEE);
            }

            visite = visiteTerrainRepository.save(visite);

            // Update dossier status and advance workflow
            Dossier dossier = visite.getDossier();
            String workflowMessage = "Visite terrain terminée";
            
            if (request.getApprouve()) {
                // Terrain approved - move to next GUC phase for final approval
                if (workflowService != null) {
                    try {
                        String comment = "Terrain approuvé par " + 
                                (utilisateur.getEquipeCommission() != null ? utilisateur.getEquipeCommission().getDisplayName() : "Commission");
                        
                        // Move to phase 4 (Final GUC approval)
                        workflowService.moveToStep(dossier.getId(), 4L, utilisateur.getId(), comment);
                        workflowMessage = "Terrain approuvé. Le dossier a été envoyé au GUC pour approbation finale.";
                        log.info("Workflow avancé vers la phase 4 pour le dossier {}", dossier.getId());
                    } catch (Exception workflowError) {
                        log.warn("Erreur lors de l'avancement du workflow pour le dossier {}: {}", 
                                 dossier.getId(), workflowError.getMessage());
                        workflowMessage = "Terrain approuvé. Veuillez contacter l'administrateur pour le suivi du workflow.";
                    }
                }
            } else {
                // Terrain rejected - reject the entire dossier
                dossier.setStatus(Dossier.DossierStatus.REJECTED);
                dossierRepository.save(dossier);
                workflowMessage = "Terrain rejeté. Le dossier a été refusé.";
                log.info("Terrain rejeté pour le dossier {}", dossier.getId());
            }

            // Create audit trail with team information
            String action = request.getApprouve() ? "VISITE_TERRAIN_APPROUVEE" : "VISITE_TERRAIN_REJETEE";
            String description = (request.getApprouve() ? "Terrain approuvé" : "Terrain rejeté") + 
                    " par " + (utilisateur.getEquipeCommission() != null ? utilisateur.getEquipeCommission().getDisplayName() : "Commission") +
                    ". " + request.getObservations();
            createAuditTrail(action, dossier, utilisateur, description);

            return TerrainVisitActionResponse.builder()
                    .success(true)
                    .message(workflowMessage)
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
     * Upload photos for terrain visit using new simplified structure
     */
    @Transactional
    public TerrainVisitActionResponse uploadVisitPhotos(Long visiteId, List<MultipartFile> files,
                                                       List<String> descriptions, List<String> coordonnees,
                                                       String userEmail) {
        try {
            Utilisateur utilisateur = getCommissionAgent(userEmail);
            VisiteTerrain visite = visiteTerrainRepository.findById(visiteId)
                    .orElseThrow(() -> new RuntimeException("Visite terrain non trouvée"));

            // Check permission
            if (!canModifyVisit(visite, utilisateur)) {
                throw new RuntimeException("Vous n'avez pas l'autorisation d'ajouter des photos à cette visite");
            }

            int photosUploaded = 0;
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String description = descriptions != null && i < descriptions.size() ? descriptions.get(i) : "";
                String gps = coordonnees != null && i < coordonnees.size() ? coordonnees.get(i) : "";                if (!file.isEmpty() && isValidImageFile(file)) {
                    try {
                        // Upload terrain photo directly
                        uploadTerrainPhotoInternal(visiteId, file, description, gps, userEmail);
                        photosUploaded++;
                    } catch (Exception e) {
                        log.warn("Erreur lors de l'upload de la photo {}: {}", file.getOriginalFilename(), e.getMessage());
                        // Continue with other photos
                    }
                }
            }

            if (photosUploaded > 0) {
                createAuditTrail("PHOTOS_VISITE_AJOUTEES", visite.getDossier(), utilisateur,
                        photosUploaded + " photo(s) ajoutée(s) à la visite terrain par " + 
                        (utilisateur.getEquipeCommission() != null ? utilisateur.getEquipeCommission().getDisplayName() : "Commission"));
            }

            return TerrainVisitActionResponse.builder()
                    .success(true)
                    .message(photosUploaded + " photo(s) uploadée(s) avec succès")
                    .visiteId(visiteId)
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de l'upload des photos", e);
            throw new RuntimeException("Erreur lors de l'upload: " + e.getMessage());
        }
    }

    /**
     * Delete a photo from terrain visit using new simplified structure
     */    @Transactional
    public TerrainVisitActionResponse deleteVisitPhoto(Long pieceJointeId, String userEmail) {
        try {
            // Delete terrain photo directly
            deleteTerrainPhotoInternal(pieceJointeId, userEmail);

            return TerrainVisitActionResponse.builder()
                    .success(true)
                    .message("Photo supprimée avec succès")
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la photo", e);
            throw new RuntimeException("Erreur lors de la suppression: " + e.getMessage());
        }
    }

    /**
     * Get all photos for a terrain visit
     */    public List<PhotoVisiteDTO> getVisitPhotos(Long visiteId, String userEmail) {
        try {
            VisiteTerrain visite = visiteTerrainRepository.findById(visiteId)
                    .orElseThrow(() -> new RuntimeException("Visite terrain non trouvée"));

            // Get all terrain photos for this dossier with the visite ID in metadata
            List<PieceJointe> photos = pieceJointeRepository.findByDossierIdAndDocumentType(
                    visite.getDossier().getId(), PieceJointe.DocumentType.TERRAIN_PHOTO);

            // Filter photos that belong to this specific visit (stored in form data)
            return photos.stream()
                    .filter(photo -> {
                        try {
                            Map<String, Object> metadata = photo.getFormDataAsMap();
                            Object visiteIdFromMetadata = metadata.get("visite_id");
                            return visiteIdFromMetadata != null && 
                                   Long.valueOf(visiteIdFromMetadata.toString()).equals(visiteId);
                        } catch (Exception e) {
                            return false; // Skip photos without proper metadata
                        }
                    })
                    .map(this::mapToPhotoVisiteDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des photos terrain: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la récupération des photos: " + e.getMessage());
        }
    }

    /**
     * Map PieceJointe to PhotoVisiteDTO
     */
    private PhotoVisiteDTO mapToPhotoVisiteDTO(PieceJointe pieceJointe) {
        Map<String, Object> metadata = pieceJointe.getFormDataAsMap();
        String gpsCoordinates = (String) metadata.get("gps_coordinates");
        String photoDescription = (String) metadata.get("photo_description");        return PhotoVisiteDTO.builder()
                .id(pieceJointe.getId())
                .nomFichier(pieceJointe.getNomFichier())
                .cheminFichier(pieceJointe.getCheminFichier())
                .description(photoDescription != null ? photoDescription : pieceJointe.getDescription())
                .coordonneesGPS(gpsCoordinates)
                .datePrise(pieceJointe.getDateUpload())
                .build();
    }

    // Private helper methods

    private Utilisateur getCommissionAgent(String userEmail) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!utilisateur.getRole().equals(Utilisateur.UserRole.AGENT_COMMISSION_TERRAIN) &&
            !utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
            throw new RuntimeException("Accès non autorisé - rôle Commission de Vérification Terrain requis");
        }

        return utilisateur;
    }

    private boolean canScheduleTerrainVisit(Dossier dossier) {
        return dossier.getStatus().equals(Dossier.DossierStatus.SUBMITTED) ||
               dossier.getStatus().equals(Dossier.DossierStatus.IN_REVIEW);
    }

    private boolean canModifyVisit(VisiteTerrain visite, Utilisateur user) {
        boolean isAssignedAgent = user.getRole().equals(Utilisateur.UserRole.AGENT_COMMISSION_TERRAIN) &&
                visite.getUtilisateurCommission().getId().equals(user.getId()) &&
                visite.getDateConstat() == null;
        
        boolean isAdmin = user.getRole().equals(Utilisateur.UserRole.ADMIN);
        
        // Additional check for team compatibility
        if (isAssignedAgent && user.getEquipeCommission() != null) {
            Long rubriqueId = visite.getDossier().getSousRubrique().getRubrique().getId();
            Utilisateur.EquipeCommission equipeRequise = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);
            return equipeRequise.equals(user.getEquipeCommission());
        }
        
        return isAssignedAgent || isAdmin;
    }

    private boolean canCompleteVisit(VisiteTerrain visite, Utilisateur user) {
        boolean isAssignedAgent = user.getRole().equals(Utilisateur.UserRole.AGENT_COMMISSION_TERRAIN) &&
                visite.getUtilisateurCommission().getId().equals(user.getId()) &&
                visite.getDateConstat() == null;
        
        boolean isAdmin = user.getRole().equals(Utilisateur.UserRole.ADMIN);
        
        // Additional check for team compatibility
        if (isAssignedAgent && user.getEquipeCommission() != null) {
            Long rubriqueId = visite.getDossier().getSousRubrique().getRubrique().getId();
            Utilisateur.EquipeCommission equipeRequise = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);
            return equipeRequise.equals(user.getEquipeCommission());
        }
        
        return isAssignedAgent || isAdmin;
    }

    private boolean isVisiteOverdue(VisiteTerrain visite) {
        return visite.getDateVisite() != null && 
               visite.getDateConstat() == null && 
               visite.getDateVisite().isBefore(LocalDate.now());
    }

    private List<VisiteTerrain> applyFilters(List<VisiteTerrain> visites, VisitFilterRequest filter) {
        return visites.stream()
                .filter(v -> filter.getStatut() == null || matchesStatus(v, filter.getStatut()))
                .filter(v -> filter.getSearchTerm() == null || matchesSearchTerm(v, filter.getSearchTerm()))
                .filter(v -> filter.getRubrique() == null || matchesRubrique(v, filter.getRubrique()))
                .filter(v -> filter.getAntenne() == null || matchesAntenne(v, filter.getAntenne()))
                .filter(v -> filter.getDateDebut() == null || !v.getDateVisite().isBefore(filter.getDateDebut()))
                .filter(v -> filter.getDateFin() == null || !v.getDateVisite().isAfter(filter.getDateFin()))
                .filter(v -> filter.getEnRetard() == null || filter.getEnRetard().equals(isVisiteOverdue(v)))
                .filter(v -> filter.getACompleter() == null || 
                        filter.getACompleter().equals(v.getDateConstat() == null))
                .collect(Collectors.toList());
    }

    private List<VisiteTerrain> applySorting(List<VisiteTerrain> visites, VisitFilterRequest filter) {
        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "dateVisite";
        boolean ascending = !"DESC".equalsIgnoreCase(filter.getSortDirection());

        return visites.stream()
                .sorted((a, b) -> {
                    int result = 0;
                    switch (sortBy) {
                        case "dateVisite":
                            result = a.getDateVisite().compareTo(b.getDateVisite());
                            break;
                        case "dateConstat":
                            if (a.getDateConstat() == null && b.getDateConstat() == null) result = 0;
                            else if (a.getDateConstat() == null) result = 1;
                            else if (b.getDateConstat() == null) result = -1;
                            else result = a.getDateConstat().compareTo(b.getDateConstat());
                            break;
                        case "agriculteur":
                            result = a.getDossier().getAgriculteur().getNom()
                                    .compareTo(b.getDossier().getAgriculteur().getNom());
                            break;
                        case "statut":
                            result = getVisiteStatus(a).compareTo(getVisiteStatus(b));
                            break;
                        default:
                            result = a.getDateVisite().compareTo(b.getDateVisite());
                    }
                    return ascending ? result : -result;
                })
                .collect(Collectors.toList());
    }

    private boolean matchesStatus(VisiteTerrain visite, String statut) {
        return getVisiteStatus(visite).equalsIgnoreCase(statut);
    }

    private boolean matchesSearchTerm(VisiteTerrain visite, String searchTerm) {
        String term = searchTerm.toLowerCase();
        Dossier dossier = visite.getDossier();
        return (dossier.getReference() != null && dossier.getReference().toLowerCase().contains(term)) ||
                (dossier.getSaba() != null && dossier.getSaba().toLowerCase().contains(term)) ||
                (dossier.getAgriculteur().getNom() != null && 
                 dossier.getAgriculteur().getNom().toLowerCase().contains(term)) ||
                (dossier.getAgriculteur().getPrenom() != null && 
                 dossier.getAgriculteur().getPrenom().toLowerCase().contains(term)) ||
                (dossier.getAgriculteur().getCin() != null && 
                 dossier.getAgriculteur().getCin().toLowerCase().contains(term));
    }

    private boolean matchesRubrique(VisiteTerrain visite, String rubrique) {
        return visite.getDossier().getSousRubrique().getRubrique().getDesignation()
                .toLowerCase().contains(rubrique.toLowerCase());
    }

    private boolean matchesAntenne(VisiteTerrain visite, String antenne) {
        return visite.getDossier().getAntenne().getDesignation()
                .toLowerCase().contains(antenne.toLowerCase());
    }

    private String getVisiteStatus(VisiteTerrain visite) {
        if (visite.getDateConstat() != null) {
            if (visite.getApprouve() == null) return "COMPLETEE";
            return visite.getApprouve() ? "APPROUVEE" : "REJETEE";
        } else if (visite.getDateVisite() != null) {
            if (visite.getDateVisite().isBefore(LocalDate.now())) {
                return "EN_RETARD";
            }
            return "PROGRAMMEE";
        }
        return "NOUVELLE";
    }

    private String getVisiteStatusDisplay(VisiteTerrain visite) {
        switch (getVisiteStatus(visite)) {
            case "PROGRAMMEE": return "Programmée";
            case "EN_RETARD": return "En retard";
            case "COMPLETEE": return "Complétée";
            case "APPROUVEE": return "Approuvée";
            case "REJETEE": return "Rejetée";
            default: return "Nouvelle";
        }
    }

    private VisiteTerrainSummaryDTO mapToVisiteTerrainSummaryDTO(VisiteTerrain visite) {
        Dossier dossier = visite.getDossier();
        // UPDATED: Count terrain photos using PieceJointe instead of PhotoVisite
        int photosCount = pieceJointeRepository.findTerrainPhotosByVisiteId(visite.getId()).size();
        
        return VisiteTerrainSummaryDTO.builder()
                .id(visite.getId())
                .dateVisite(visite.getDateVisite())
                .dateConstat(visite.getDateConstat())
                .approuve(visite.getApprouve())
                .statut(getVisiteStatus(visite))
                .statutDisplay(getVisiteStatusDisplay(visite))
                .isOverdue(isVisiteOverdue(visite))
                .daysUntilVisit(calculateDaysUntilVisit(visite))
                .dossierId(dossier.getId())
                .dossierReference(dossier.getReference())
                .saba(dossier.getSaba())
                .agriculteurNom(dossier.getAgriculteur().getNom())
                .agriculteurPrenom(dossier.getAgriculteur().getPrenom())
                .agriculteurCin(dossier.getAgriculteur().getCin())
                .agriculteurTelephone(dossier.getAgriculteur().getTelephone())
                .rubriqueDesignation(dossier.getSousRubrique().getRubrique().getDesignation())
                .sousRubriqueDesignation(dossier.getSousRubrique().getDesignation())
                .antenneDesignation(dossier.getAntenne().getDesignation())
                .agriculteurCommune(dossier.getAgriculteur().getCommuneRurale() != null ? 
                        dossier.getAgriculteur().getCommuneRurale().getDesignation() : null)
                .agriculteurDouar(dossier.getAgriculteur().getDouar() != null ? 
                        dossier.getAgriculteur().getDouar().getDesignation() : null)
                .canComplete(visite.getDateConstat() == null)
                .canModify(visite.getDateConstat() == null)
                .canView(true)
                .photosCount(photosCount)
                .hasNotes(visite.getObservations() != null && !visite.getObservations().isEmpty())
                .build();
    }

    private VisiteTerrainResponse mapToVisiteTerrainResponse(VisiteTerrain visite, Utilisateur currentUser) {
        // UPDATED: Get terrain photos using PieceJointe instead of PhotoVisite
        List<PhotoVisiteDTO> photos = pieceJointeRepository.findTerrainPhotosByVisiteId(visite.getId())
                .stream()
                .map(this::mapToPieceJointeToPhotoDTO)
                .collect(Collectors.toList());

        Dossier dossier = visite.getDossier();
        
        return VisiteTerrainResponse.builder()
                .id(visite.getId())
                .dateVisite(visite.getDateVisite())
                .dateConstat(visite.getDateConstat())
                .observations(visite.getObservations())
                .recommandations(visite.getRecommandations())
                .approuve(visite.getApprouve())
                .coordonneesGPS(visite.getCoordonneesGPS())
                .statut(getVisiteStatus(visite))
                .dossierId(dossier.getId())
                .dossierReference(dossier.getReference())
                .dossierSaba(dossier.getSaba())
                .dossierStatut(dossier.getStatus().name())
                .agriculteurNom(dossier.getAgriculteur().getNom())
                .agriculteurPrenom(dossier.getAgriculteur().getPrenom())
                .agriculteurCin(dossier.getAgriculteur().getCin())
                .agriculteurTelephone(dossier.getAgriculteur().getTelephone())
                .agriculteurCommune(dossier.getAgriculteur().getCommuneRurale() != null ? 
                        dossier.getAgriculteur().getCommuneRurale().getDesignation() : null)
                .agriculteurDouar(dossier.getAgriculteur().getDouar() != null ? 
                        dossier.getAgriculteur().getDouar().getDesignation() : null)
                .rubriqueDesignation(dossier.getSousRubrique().getRubrique().getDesignation())
                .sousRubriqueDesignation(dossier.getSousRubrique().getDesignation())
                .antenneDesignation(dossier.getAntenne().getDesignation())
                .cdaNom(dossier.getAntenne().getCda() != null ? 
                        dossier.getAntenne().getCda().getDescription() : null)
                .utilisateurCommissionNom(visite.getUtilisateurCommission().getPrenom() + " " + 
                        visite.getUtilisateurCommission().getNom())
                .photos(photos)
                .canSchedule(false)
                .canComplete(canCompleteVisit(visite, currentUser))
                .canModify(canModifyVisit(visite, currentUser))
                .isOverdue(isVisiteOverdue(visite))
                .daysUntilVisit(calculateDaysUntilVisit(visite))
                .build();
    }

    // UPDATED: Map PieceJointe to PhotoVisiteDTO for compatibility
    private PhotoVisiteDTO mapToPieceJointeToPhotoDTO(PieceJointe pieceJointe) {
        // Extract GPS coordinates from form data if available
        String coordonneesGPS = null;
        if (pieceJointe.hasFormData()) {
            var formData = pieceJointe.getFormDataAsMap();
            coordonneesGPS = (String) formData.get("coordonnees_gps");
        }
        
        return PhotoVisiteDTO.builder()
                .id(pieceJointe.getId())
                .nomFichier(pieceJointe.getNomFichier())
                .cheminFichier(pieceJointe.getCheminFichier())
                .description(pieceJointe.getDescription())
                .coordonneesGPS(coordonneesGPS)
                .datePrise(pieceJointe.getDateUpload())
                .downloadUrl("/api/agent_commission/terrain-visits/photos/" + pieceJointe.getId() + "/download")
                .build();
    }

    private Integer calculateDaysUntilVisit(VisiteTerrain visite) {
        if (visite.getDateVisite() == null) return null;
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), visite.getDateVisite());
    }

    private VisitStatisticsDTO calculateVisitStatistics(List<VisiteTerrain> visites) {
        long total = visites.size();
        long enAttente = visites.stream().filter(v -> v.getDateConstat() == null).count();
        long realisees = visites.stream().filter(v -> v.getDateConstat() != null).count();
        long approuvees = visites.stream().filter(v -> Boolean.TRUE.equals(v.getApprouve())).count();
        long rejetees = visites.stream().filter(v -> Boolean.FALSE.equals(v.getApprouve())).count();
        long enRetard = visites.stream().filter(this::isVisiteOverdue).count();
        
        LocalDate today = LocalDate.now();
        long aujourd_hui = visites.stream()
                .filter(v -> v.getDateVisite() != null && v.getDateVisite().equals(today))
                .count();
                
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        long cetteSemaine = visites.stream()
                .filter(v -> v.getDateVisite() != null && 
                        !v.getDateVisite().isBefore(startOfWeek) && 
                        !v.getDateVisite().isAfter(endOfWeek))
                .count();

        double tauxApprobation = realisees > 0 ? (double) approuvees / realisees * 100 : 0;

        return VisitStatisticsDTO.builder()
                .totalVisites(total)
                .visitesEnAttente(enAttente)
                .visitesRealisees(realisees)
                .visitesApprouvees(approuvees)
                .visitesRejetees(rejetees)
                .visitesEnRetard(enRetard)
                .visitesAujourdHui(aujourd_hui)
                .visitesCetteSemaine(cetteSemaine)
                .tauxApprobation(tauxApprobation)
                .tauxRejetRapide(0.0) // To be implemented
                .build();
    }

    private List<String> getAvailableStatuses() {
        return List.of("NOUVELLE", "PROGRAMMEE", "EN_RETARD", "COMPLETEE", "APPROUVEE", "REJETEE");
    }

    private List<String> getAvailableProjectTypes() {
        return List.of("FILIERES VEGETALES", "FILIERES ANIMALES", 
                       "AMENAGEMENT HYDRO-AGRICOLE ET AMELIORATION FONCIERE");
    }

    private boolean isValidImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (
                contentType.startsWith("image/") || 
                contentType.equals("application/pdf")
        );
    }

    private void createAuditTrail(String action, Dossier dossier, Utilisateur utilisateur, String description) {
        AuditTrail audit = new AuditTrail();
        audit.setTimestamp(LocalDateTime.now());
        audit.setUserId(utilisateur.getId());        audit.setAction(action);
        audit.setEntityType("Dossier");
        audit.setEntityId(dossier.getId());
        audit.setOldValue(null);
        audit.setNewValue(null);
        audit.setDetails(description);
        auditTrailRepository.save(audit);
    }    /**
     * Internal method to upload terrain photo
     */
    private void uploadTerrainPhotoInternal(Long visiteId, MultipartFile file, String description, String gps, String userEmail) {
        try {
            VisiteTerrain visite = visiteTerrainRepository.findById(visiteId)
                    .orElseThrow(() -> new RuntimeException("Visite terrain non trouvée"));
            
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            // Create storage directory
            Path storageLocation = Paths.get(uploadDir, "terrain-visits").toAbsolutePath().normalize();
            if (!Files.exists(storageLocation)) {
                Files.createDirectories(storageLocation);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String uniqueFilename = "terrain_" + visiteId + "_" + System.currentTimeMillis() + extension;
            Path targetLocation = storageLocation.resolve(uniqueFilename);
            
            // Save file
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // Create PieceJointe record
            PieceJointe pieceJointe = new PieceJointe();
            pieceJointe.setDossier(visite.getDossier());
            pieceJointe.setUtilisateur(utilisateur);
            pieceJointe.setNomFichier(originalFilename);
            pieceJointe.setCheminFichier(targetLocation.toString());
            pieceJointe.setDateUpload(LocalDateTime.now());
            pieceJointe.setDocumentType(PieceJointe.DocumentType.TERRAIN_PHOTO);
            pieceJointe.setStatus(PieceJointe.DocumentStatus.COMPLETE);
            
            // Use description field for photo description and GPS coordinates
            String fullDescription = description;
            if (gps != null && !gps.trim().isEmpty()) {
                fullDescription += " | GPS: " + gps;
            }
            pieceJointe.setDescription(fullDescription);
            
            // Store additional metadata in form data as JSON
            if (gps != null && !gps.trim().isEmpty()) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("type", "terrain_photo");
                metadata.put("visite_id", visiteId);
                metadata.put("gps_coordinates", gps);
                metadata.put("photo_description", description);
                pieceJointe.setFormDataFromMap(metadata);
            }
            
            pieceJointeRepository.save(pieceJointe);
            
        } catch (Exception e) {
            log.error("Erreur lors de l'upload de la photo terrain: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'upload de la photo: " + e.getMessage());
        }
    }    /**
     * Internal method to delete terrain photo
     */
    private void deleteTerrainPhotoInternal(Long pieceJointeId, String userEmail) {
        try {
            PieceJointe pieceJointe = pieceJointeRepository.findById(pieceJointeId)
                    .orElseThrow(() -> new RuntimeException("Document non trouvé"));
            
            // Verify it's a terrain photo
            if (pieceJointe.getDocumentType() != PieceJointe.DocumentType.TERRAIN_PHOTO) {
                throw new RuntimeException("Ce n'est pas une photo de terrain");
            }
            
            // Delete physical file
            if (pieceJointe.getCheminFichier() != null) {
                try {
                    Path filePath = Paths.get(pieceJointe.getCheminFichier());
                    Files.deleteIfExists(filePath);
                } catch (Exception e) {
                    log.warn("Impossible de supprimer le fichier physique: {}", e.getMessage());
                }
            }
            
            // Delete database record
            pieceJointeRepository.delete(pieceJointe);
            
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la photo terrain: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la suppression de la photo: " + e.getMessage());
        }
    }
}