package ormvat.sadsa.service.service_technique;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;
import ormvat.sadsa.service.workflow.WorkflowService;
import ormvat.sadsa.service.workflow.AuditService;
import ormvat.sadsa.dto.role_based.RoleBasedDossierDTOs.*;
import ormvat.sadsa.dto.service_technique.ImplementationVisitDTOs.*;

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
public class ServiceTechniqueDossierService {

    private final DossierRepository dossierRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final VisiteImplementationRepository visiteImplementationRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final WorkflowService workflowService;
    private final AuditService auditService;

    @Value("${app.upload.dir:./uploads/ormvat_sadsa}")
    private String uploadDir;

    public DossierListResponse getDossiersForImplementation(String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        
        List<Dossier> dossiers = dossierRepository.findByStatus(Dossier.DossierStatus.REALIZATION_IN_PROGRESS)
                .stream()
                .filter(this::isInServiceTechniquePhase)
                .collect(Collectors.toList());
        
        List<DossierSummaryDTO> summaries = dossiers.stream()
                .map(dossier -> mapToSummaryDTO(dossier, user))
                .collect(Collectors.toList());

        return DossierListResponse.builder()
                .dossiers(summaries)
                .build();
    }

    public DossierDetailResponse getDossierById(Long id, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        if (!isInServiceTechniquePhase(dossier)) {
            throw new RuntimeException("Dossier non accessible par le Service Technique");
        }

        return mapToDetailDTO(dossier, user);
    }

    /**
     * Get dashboard summary for service technique agent
     */
    public ImplementationDashboardSummaryDTO getDashboardSummary(String userEmail) {
        try {
            Utilisateur utilisateur = getServiceTechniqueAgent(userEmail);
            List<VisiteImplementation> allVisits = visiteImplementationRepository.findByUtilisateurServiceTechniqueId(utilisateur.getId());
            
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

            // Calculate global progress average
            double avancementMoyenGlobal = allVisits.stream()
                    .filter(v -> v.getPourcentageAvancement() != null)
                    .mapToInt(VisiteImplementation::getPourcentageAvancement)
                    .average()
                    .orElse(0.0);

            long projetsTermines = allVisits.stream()
                    .filter(v -> v.getPourcentageAvancement() != null && v.getPourcentageAvancement() == 100)
                    .count();
            
            // Get upcoming visits (next 7 days)
            List<VisiteImplementationSummaryDTO> prochaines = allVisits.stream()
                    .filter(v -> v.getDateVisite() != null && 
                            v.getDateVisite().isAfter(today) && 
                            v.getDateVisite().isBefore(today.plusDays(8)) &&
                            v.getDateConstat() == null)
                    .map(this::mapToVisiteImplementationSummaryDTO)
                    .sorted((a, b) -> a.getDateVisite().compareTo(b.getDateVisite()))
                    .limit(5)
                    .collect(Collectors.toList());
            
            // Get urgent visits (overdue or today)
            List<VisiteImplementationSummaryDTO> urgentes = allVisits.stream()
                    .filter(v -> (v.getDateVisite() != null && 
                            v.getDateVisite().isBefore(tomorrow) && 
                            v.getDateConstat() == null) || isVisiteOverdue(v))
                    .map(this::mapToVisiteImplementationSummaryDTO)
                    .sorted((a, b) -> a.getDateVisite().compareTo(b.getDateVisite()))
                    .limit(5)
                    .collect(Collectors.toList());
            
            ImplementationVisitStatisticsDTO statistiques = calculateImplementationVisitStatistics(allVisits);
            
            return ImplementationDashboardSummaryDTO.builder()
                    .visitesAujourdHui(visitesAujourdHui)
                    .visitesDemain(visitesDemain)
                    .visitesEnRetard(visitesEnRetard)
                    .visitesEnAttente(visitesEnAttente)
                    .prochaines(prochaines)
                    .urgentes(urgentes)
                    .statistiques(statistiques)
                    .avancementMoyenGlobal(avancementMoyenGlobal)
                    .projetsTermines(projetsTermines)
                    .build();
                    
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du tableau de bord Service Technique", e);
            throw new RuntimeException("Erreur lors de la récupération: " + e.getMessage());
        }
    }

    /**
     * Get implementation visits with filtering and pagination
     */
    public ImplementationVisitListResponse getImplementationVisits(ImplementationVisitFilterRequest filterRequest, String userEmail) {
        try {
            Utilisateur utilisateur = getServiceTechniqueAgent(userEmail);
            
            List<VisiteImplementation> allVisits = visiteImplementationRepository.findByUtilisateurServiceTechniqueId(utilisateur.getId());

            // Apply filters
            List<VisiteImplementation> filteredVisits = applyFilters(allVisits, filterRequest);

            // Apply sorting
            filteredVisits = applySorting(filteredVisits, filterRequest);

            // Apply pagination
            int page = filterRequest.getPage() != null ? filterRequest.getPage() : 0;
            int size = filterRequest.getSize() != null ? filterRequest.getSize() : 20;
            int start = page * size;
            int end = Math.min(start + size, filteredVisits.size());
            
            List<VisiteImplementation> paginatedVisits = filteredVisits.subList(start, end);

            // Convert to DTOs
            List<VisiteImplementationSummaryDTO> visitSummaries = paginatedVisits.stream()
                    .map(this::mapToVisiteImplementationSummaryDTO)
                    .collect(Collectors.toList());

            // Calculate statistics
            ImplementationVisitStatisticsDTO statistics = calculateImplementationVisitStatistics(allVisits);

            return ImplementationVisitListResponse.builder()
                    .visites(visitSummaries)
                    .totalCount((long) filteredVisits.size())
                    .currentPage(page)
                    .pageSize(size)
                    .totalPages((int) Math.ceil((double) filteredVisits.size() / size))
                    .statistics(statistics)
                    .availableStatuses(getAvailableStatuses())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des visites d'implémentation", e);
            throw new RuntimeException("Erreur lors de la récupération: " + e.getMessage());
        }
    }

    /**
     * Get implementation visit detail
     */
    public VisiteImplementationResponse getImplementationVisitDetail(Long visiteId, String userEmail) {
        try {
            Utilisateur utilisateur = getServiceTechniqueAgent(userEmail);
            VisiteImplementation visite = visiteImplementationRepository.findById(visiteId)
                    .orElseThrow(() -> new RuntimeException("Visite d'implémentation non trouvée"));

            // Check access permission
            if (!visite.getUtilisateurServiceTechnique().getId().equals(utilisateur.getId()) &&
                !utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
                throw new RuntimeException("Accès non autorisé à cette visite");
            }

            return mapToVisiteImplementationResponse(visite, utilisateur);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération du détail de la visite d'implémentation", e);
            throw new RuntimeException("Erreur lors de la récupération: " + e.getMessage());
        }
    }

    /**
     * Schedule an implementation visit
     */
    @Transactional
    public ImplementationVisitActionResponse scheduleImplementationVisit(ScheduleImplementationVisitRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = getServiceTechniqueAgent(userEmail);
            Dossier dossier = dossierRepository.findById(request.getDossierId())
                    .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

            // Check if dossier is in the right status for implementation visit
            if (!canScheduleImplementationVisit(dossier)) {
                throw new RuntimeException("Le dossier n'est pas dans le bon statut pour une visite d'implémentation");
            }

            // Create new implementation visit
            VisiteImplementation visite = new VisiteImplementation();
            visite.setDossier(dossier);
            visite.setUtilisateurServiceTechnique(utilisateur);
            visite.setDateVisite(request.getDateVisite());
            visite.setObservations(request.getObservations());
            visite.setCoordonneesGPS(request.getCoordonneesGPS());
            visite.setRecommandations(request.getRecommandations());
            visite.setPourcentageAvancement(request.getPourcentageAvancement() != null ? request.getPourcentageAvancement() : 0);
            visite.setApprouve(null);

            visite = visiteImplementationRepository.save(visite);

            // Create audit trail
            createAuditTrail("VISITE_IMPLEMENTATION_PROGRAMMEE", dossier, utilisateur, 
                    "Visite d'implémentation programmée pour le " + request.getDateVisite());

            return ImplementationVisitActionResponse.builder()
                    .success(true)
                    .message("Visite d'implémentation programmée avec succès")
                    .visiteId(visite.getId())
                    .newStatut("VISITE_PROGRAMMEE")
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la programmation de la visite d'implémentation", e);
            throw new RuntimeException("Erreur lors de la programmation: " + e.getMessage());
        }
    }

    /**
     * Complete an implementation visit with approval/rejection
     */
    @Transactional
    public ImplementationVisitActionResponse completeImplementationVisit(CompleteImplementationVisitRequest request, String userEmail) {
        try {
            Utilisateur utilisateur = getServiceTechniqueAgent(userEmail);
            VisiteImplementation visite = visiteImplementationRepository.findById(request.getVisiteId())
                    .orElseThrow(() -> new RuntimeException("Visite d'implémentation non trouvée"));

            // Check permission
            if (!canCompleteVisit(visite, utilisateur)) {
                throw new RuntimeException("Vous n'avez pas l'autorisation de finaliser cette visite");
            }

            // Update visit details
            visite.setDateConstat(request.getDateConstat());
            visite.setObservations(request.getObservations());            visite.setRecommandations(request.getRecommandations());
            visite.setApprouve(request.getApprouve());
            visite.setCoordonneesGPS(request.getCoordonneesGPS());
            visite.setConclusion(request.getConclusion());
            visite.setProblemes_detectes(request.getProblemesDetectes());
            visite.setActions_correctives(request.getActionsCorrectives());
            visite.setPourcentageAvancement(request.getPourcentageAvancement());
            visite.setDureeVisite(request.getDureeVisite());
            
            // Set visit status
            visite.setStatutVisite(VisiteImplementation.StatutVisite.TERMINEE);

            visite = visiteImplementationRepository.save(visite);

            // Update dossier status and advance workflow
            Dossier dossier = visite.getDossier();
            String workflowMessage = "Visite d'implémentation terminée";
            
            if (request.getApprouve() && request.getPourcentageAvancement() == 100) {
                // Implementation complete and approved - move to final GUC phase
                try {
                    String comment = "Implémentation terminée et approuvée par le Service Technique";
                    
                    // Move to phase 8 (Final GUC validation)
                    workflowService.moveToStep(dossier.getId(), 8L, utilisateur.getId(), comment);
                    workflowMessage = "Implémentation terminée et approuvée. Le dossier a été envoyé au GUC pour validation finale.";
                    log.info("Workflow avancé vers la phase 8 pour le dossier {}", dossier.getId());
                } catch (Exception workflowError) {
                    log.warn("Erreur lors de l'avancement du workflow pour le dossier {}: {}", 
                             dossier.getId(), workflowError.getMessage());
                    workflowMessage = "Implémentation terminée. Veuillez contacter l'administrateur pour le suivi du workflow.";
                }
            } else if (!request.getApprouve()) {
                // Implementation rejected
                workflowMessage = "Implémentation rejetée. Des actions correctives sont nécessaires.";
                log.info("Implémentation rejetée pour le dossier {}", dossier.getId());
            } else {
                // Implementation in progress
                workflowMessage = "Avancement de l'implémentation mis à jour (" + request.getPourcentageAvancement() + "%).";
            }

            // Create audit trail
            String action = request.getApprouve() ? "VISITE_IMPLEMENTATION_APPROUVEE" : "VISITE_IMPLEMENTATION_REJETEE";
            String description = (request.getApprouve() ? "Implémentation approuvée" : "Implémentation rejetée") + 
                    " par le Service Technique. Avancement: " + request.getPourcentageAvancement() + "%. " + request.getObservations();
            createAuditTrail(action, dossier, utilisateur, description);

            return ImplementationVisitActionResponse.builder()
                    .success(true)
                    .message(workflowMessage)
                    .visiteId(visite.getId())
                    .newStatut(request.getApprouve() ? "APPROUVE" : "REJETE")
                    .dateAction(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Erreur lors de la finalisation de la visite d'implémentation", e);
            throw new RuntimeException("Erreur lors de la finalisation: " + e.getMessage());
        }
    }

    /**
     * Upload photos for implementation visit
     */
    @Transactional
    public ImplementationVisitActionResponse uploadImplementationVisitPhotos(Long visiteId, List<MultipartFile> files,
                                                       List<String> descriptions, List<String> coordonnees,
                                                       String userEmail) {
        try {
            Utilisateur utilisateur = getServiceTechniqueAgent(userEmail);
            VisiteImplementation visite = visiteImplementationRepository.findById(visiteId)
                    .orElseThrow(() -> new RuntimeException("Visite d'implémentation non trouvée"));

            // Check permission
            if (!canModifyVisit(visite, utilisateur)) {
                throw new RuntimeException("Vous n'avez pas l'autorisation d'ajouter des photos à cette visite");
            }

            int photosUploaded = 0;
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String description = descriptions != null && i < descriptions.size() ? descriptions.get(i) : "";
                String gps = coordonnees != null && i < coordonnees.size() ? coordonnees.get(i) : "";
                
                if (!file.isEmpty() && isValidImageFile(file)) {
                    try {
                        uploadImplementationPhotoInternal(visiteId, file, description, gps, userEmail);
                        photosUploaded++;
                    } catch (Exception e) {
                        log.warn("Erreur lors de l'upload de la photo {}: {}", file.getOriginalFilename(), e.getMessage());
                    }
                }
            }

            if (photosUploaded > 0) {
                createAuditTrail("PHOTOS_IMPLEMENTATION_AJOUTEES", visite.getDossier(), utilisateur,
                        photosUploaded + " photo(s) ajoutée(s) à la visite d'implémentation");
            }

            return ImplementationVisitActionResponse.builder()
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

    @Transactional
    public ActionResponse verifyImplementation(Long id, VerificationRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForServiceTechniqueAction(id);

        auditService.logAction(user.getId(), "VERIFY_IMPLEMENTATION", "Dossier", dossier.getId(),
                              null, "VERIFIED", 
                              "Vérification implémentation - " + request.getCommentaire());

        return ActionResponse.builder()
                .success(true)
                .message("Implémentation vérifiée")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public ActionResponse markComplete(Long id, CompletionRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForServiceTechniqueAction(id);

        // Move to Phase 8 (Final GUC validation)
        workflowService.moveToStep(dossier.getId(), 8L, user.getId(), 
                                  "Réalisation terminée - " + request.getCommentaire());

        auditService.logAction(user.getId(), "COMPLETE_IMPLEMENTATION", "Dossier", dossier.getId(),
                              "Phase 7", "Phase 8", 
                              "Réalisation terminée - " + request.getCommentaire());

        return ActionResponse.builder()
                .success(true)
                .message("Réalisation marquée comme terminée - Envoyé au GUC pour validation finale")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public ActionResponse reportIssues(Long id, IssueRequest request, String userEmail) {
        Utilisateur user = getUserByEmail(userEmail);
        Dossier dossier = getDossierForServiceTechniqueAction(id);

        auditService.logAction(user.getId(), "REPORT_ISSUES", "Dossier", dossier.getId(),
                              null, "ISSUES_REPORTED", 
                              "Problèmes signalés - " + request.getProbleme() + " - " + request.getCommentaire());

        return ActionResponse.builder()
                .success(true)
                .message("Problèmes signalés")
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Helper methods
    private Utilisateur getUserByEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private Utilisateur getServiceTechniqueAgent(String userEmail) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!utilisateur.getRole().equals(Utilisateur.UserRole.SERVICE_TECHNIQUE) &&
            !utilisateur.getRole().equals(Utilisateur.UserRole.ADMIN)) {
            throw new RuntimeException("Accès non autorisé - rôle Service Technique requis");
        }

        return utilisateur;
    }

    private Dossier getDossierForServiceTechniqueAction(Long id) {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        if (!isInServiceTechniquePhase(dossier)) {
            throw new RuntimeException("Action Service Technique non autorisée sur ce dossier");
        }

        return dossier;
    }

    private boolean isInServiceTechniquePhase(Dossier dossier) {
        if (dossier.getStatus() != Dossier.DossierStatus.REALIZATION_IN_PROGRESS) return false;
        
        // Check if current workflow step is Phase 7 (Service Technique)
        var currentStep = workflowService.getCurrentStep(dossier.getId());
        if (currentStep == null) return false;
        
        return currentStep.getIdEtape() == 7L;
    }

    private boolean canScheduleImplementationVisit(Dossier dossier) {
        return dossier.getStatus().equals(Dossier.DossierStatus.REALIZATION_IN_PROGRESS) && isInServiceTechniquePhase(dossier);
    }

    private boolean canModifyVisit(VisiteImplementation visite, Utilisateur user) {
        boolean isAssignedAgent = user.getRole().equals(Utilisateur.UserRole.SERVICE_TECHNIQUE) &&
                visite.getUtilisateurServiceTechnique().getId().equals(user.getId()) &&
                visite.getDateConstat() == null;
        
        boolean isAdmin = user.getRole().equals(Utilisateur.UserRole.ADMIN);
        
        return isAssignedAgent || isAdmin;
    }

    private boolean canCompleteVisit(VisiteImplementation visite, Utilisateur user) {
        boolean isAssignedAgent = user.getRole().equals(Utilisateur.UserRole.SERVICE_TECHNIQUE) &&
                visite.getUtilisateurServiceTechnique().getId().equals(user.getId()) &&
                visite.getDateConstat() == null;
        
        boolean isAdmin = user.getRole().equals(Utilisateur.UserRole.ADMIN);
        
        return isAssignedAgent || isAdmin;
    }

    private boolean isVisiteOverdue(VisiteImplementation visite) {
        return visite.getDateVisite() != null && 
               visite.getDateConstat() == null && 
               visite.getDateVisite().isBefore(LocalDate.now());
    }

    private boolean isValidImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (
                contentType.startsWith("image/") || 
                contentType.equals("application/pdf")
        );
    }

    private void createAuditTrail(String action, Dossier dossier, Utilisateur utilisateur, String description) {
        auditService.logAction(utilisateur.getId(), action, "Dossier", dossier.getId(),
                              null, null, description);
    }

    private void uploadImplementationPhotoInternal(Long visiteId, MultipartFile file, String description, String gps, String userEmail) {
        try {
            VisiteImplementation visite = visiteImplementationRepository.findById(visiteId)
                    .orElseThrow(() -> new RuntimeException("Visite d'implémentation non trouvée"));
            
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            // Create storage directory
            Path storageLocation = Paths.get(uploadDir, "implementation-visits").toAbsolutePath().normalize();
            if (!Files.exists(storageLocation)) {
                Files.createDirectories(storageLocation);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String uniqueFilename = "implementation_" + visiteId + "_" + System.currentTimeMillis() + extension;
            Path targetLocation = storageLocation.resolve(uniqueFilename);
            
            // Save file
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // Create PieceJointe record with TERRAIN_PHOTO type (reusing existing type)
            PieceJointe pieceJointe = new PieceJointe();
            pieceJointe.setDossier(visite.getDossier());
            pieceJointe.setUtilisateur(utilisateur);
            pieceJointe.setNomFichier(originalFilename);
            pieceJointe.setCheminFichier(targetLocation.toString());
            pieceJointe.setDateUpload(LocalDateTime.now());
            pieceJointe.setDocumentType(PieceJointe.DocumentType.TERRAIN_PHOTO); // Reuse existing type
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
                metadata.put("type", "implementation_photo");
                metadata.put("visite_id", visiteId);
                metadata.put("gps_coordinates", gps);
                metadata.put("photo_description", description);
                pieceJointe.setFormDataFromMap(metadata);
            }
            
            pieceJointeRepository.save(pieceJointe);
            
        } catch (Exception e) {
            log.error("Erreur lors de l'upload de la photo d'implémentation: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'upload de la photo: " + e.getMessage());
        }
    }

    // DTO mapping methods
    private DossierSummaryDTO mapToSummaryDTO(Dossier dossier, Utilisateur user) {
        TimingDTO timing = workflowService.getTimingInfo(dossier.getId());

        return DossierSummaryDTO.builder()
                .id(dossier.getId())
                .numeroDossier(dossier.getNumeroDossier())
                .saba(dossier.getSaba())
                .reference(dossier.getReference())
                .agriculteurNom(dossier.getAgriculteur().getNom() + " " + dossier.getAgriculteur().getPrenom())
                .agriculteurCin(dossier.getAgriculteur().getCin())
                .statut(dossier.getStatus().name())
                .dateCreation(dossier.getDateCreation())
                .montantSubvention(dossier.getMontantSubvention())
                .currentStep(timing.getCurrentStep())
                .enRetard(timing.getEnRetard())
                .joursRetard(timing.getJoursRetard())
                .joursRestants(timing.getJoursRestants())
                .availableActions(getActionsForServiceTechniquePhase(dossier.getId()))
                .build();
    }

    private DossierDetailResponse mapToDetailDTO(Dossier dossier, Utilisateur user) {
        TimingDTO timing = workflowService.getTimingInfo(dossier.getId());

        return DossierDetailResponse.builder()
                .id(dossier.getId())
                .numeroDossier(dossier.getNumeroDossier())
                .saba(dossier.getSaba())
                .reference(dossier.getReference())
                .statut(dossier.getStatus().name())
                .dateCreation(dossier.getDateCreation())
                .dateSubmission(dossier.getDateSubmission())
                .dateApprobation(dossier.getDateApprobation())
                .montantSubvention(dossier.getMontantSubvention())
                .agriculteur(mapToAgriculteurDTO(dossier.getAgriculteur()))
                .antenne(mapToAntenneDTO(dossier.getAntenne()))
                .projet(mapToProjetDTO(dossier.getSousRubrique()))
                .utilisateurCreateur(mapToUtilisateurCreateurDTO(dossier.getUtilisateurCreateur()))
                .timing(timing)
                .workflowHistory(mapToWorkflowHistoryDTOs(dossier.getId()))
                .documents(List.of()) // TODO: Implement documents
                .availableActions(getActionsForServiceTechniquePhase(dossier.getId()))
                .build();
    }

    private AgriculteurDTO mapToAgriculteurDTO(Agriculteur agriculteur) {
        return AgriculteurDTO.builder()
                .id(agriculteur.getId())
                .nom(agriculteur.getNom())
                .prenom(agriculteur.getPrenom())
                .cin(agriculteur.getCin())
                .telephone(agriculteur.getTelephone())
                .communeRurale(agriculteur.getCommuneRurale() != null ? 
                              agriculteur.getCommuneRurale().getDesignation() : null)
                .douar(agriculteur.getDouar() != null ? 
                      agriculteur.getDouar().getDesignation() : null)
                .build();
    }

    private AntenneDTO mapToAntenneDTO(Antenne antenne) {
        return AntenneDTO.builder()
                .id(antenne.getId())
                .designation(antenne.getDesignation())
                .abreviation(antenne.getAbreviation())
                .build();
    }

    private ProjetDTO mapToProjetDTO(SousRubrique sousRubrique) {
        return ProjetDTO.builder()
                .rubrique(sousRubrique.getRubrique().getDesignation())
                .sousRubrique(sousRubrique.getDesignation())
                .description(sousRubrique.getDescription())
                .build();
    }

    private UtilisateurCreateurDTO mapToUtilisateurCreateurDTO(Utilisateur utilisateur) {
        return UtilisateurCreateurDTO.builder()
                .id(utilisateur.getId())
                .nom(utilisateur.getNom() + " " + utilisateur.getPrenom())
                .email(utilisateur.getEmail())
                .build();
    }

    private List<WorkflowHistoryDTO> mapToWorkflowHistoryDTOs(Long dossierId) {
        return workflowService.getWorkflowHistory(dossierId).stream()
                .map(wi -> WorkflowHistoryDTO.builder()
                        .id(wi.getId())
                        .idEtape(wi.getIdEtape())
                        .phaseNom("Phase " + wi.getIdEtape())
                        .dateEntree(wi.getDateEntree())
                        .dateSortie(wi.getDateSortie())
                        .userId(wi.getIdUser())
                        .userName("User " + wi.getIdUser()) // TODO: Get actual name
                        .commentaire(wi.getCommentaire())
                        .dureeJours(wi.getDateSortie() != null ? 
                                   (int) java.time.Duration.between(wi.getDateEntree(), wi.getDateSortie()).toDays() : null)
                        .build())
                .collect(Collectors.toList());
    }

    private List<ActionDTO> getActionsForServiceTechniquePhase(Long dossierId) {
        return List.of(
                ActionDTO.builder()
                        .action("schedule-visit")
                        .label("Programmer Visite")
                        .endpoint("/api/service-technique/visits/schedule")
                        .method("POST")
                        .build(),
                ActionDTO.builder()
                        .action("verify")
                        .label("Vérifier Implémentation")
                        .endpoint("/api/service-technique/dossiers/verify/" + dossierId)
                        .method("POST")
                        .build(),
                ActionDTO.builder()
                        .action("complete")
                        .label("Marquer Terminé")
                        .endpoint("/api/service-technique/dossiers/complete/" + dossierId)
                        .method("POST")
                        .build(),
                ActionDTO.builder()
                        .action("report-issues")
                        .label("Signaler Problèmes")
                        .endpoint("/api/service-technique/dossiers/report-issues/" + dossierId)
                        .method("POST")
                        .build()
        );
    }

    // Implementation visit specific mappings
    private VisiteImplementationSummaryDTO mapToVisiteImplementationSummaryDTO(VisiteImplementation visite) {
        Dossier dossier = visite.getDossier();
        int photosCount = getImplementationPhotosCount(visite.getId());
        
        return VisiteImplementationSummaryDTO.builder()
                .id(visite.getId())
                .dateVisite(visite.getDateVisite())
                .dateConstat(visite.getDateConstat())
                .approuve(visite.getApprouve())
                .statut(getVisiteStatus(visite))
                .statutDisplay(getVisiteStatusDisplay(visite))
                .isOverdue(isVisiteOverdue(visite))
                .daysUntilVisit(calculateDaysUntilVisit(visite))
                .pourcentageAvancement(visite.getPourcentageAvancement())
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

    private VisiteImplementationResponse mapToVisiteImplementationResponse(VisiteImplementation visite, Utilisateur currentUser) {
        List<PhotoImplementationDTO> photos = getImplementationPhotos(visite.getId());
        
        Dossier dossier = visite.getDossier();
        
        return VisiteImplementationResponse.builder()
                .id(visite.getId())
                .dateVisite(visite.getDateVisite())
                .dateConstat(visite.getDateConstat())                .observations(visite.getObservations())
                .recommandations(visite.getRecommandations())
                .approuve(visite.getApprouve())
                .coordonneesGPS(visite.getCoordonneesGPS())
                .statut(getVisiteStatus(visite))
                .conclusion(visite.getConclusion())
                .problemesDetectes(visite.getProblemes_detectes())
                .actionsCorrectives(visite.getActions_correctives())
                .pourcentageAvancement(visite.getPourcentageAvancement())
                .dureeVisite(visite.getDureeVisite())
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
                .utilisateurServiceTechniqueNom(visite.getUtilisateurServiceTechnique().getPrenom() + " " + 
                        visite.getUtilisateurServiceTechnique().getNom())
                .photos(photos)
                .canSchedule(false)
                .canComplete(canCompleteVisit(visite, currentUser))
                .canModify(canModifyVisit(visite, currentUser))
                .isOverdue(isVisiteOverdue(visite))
                .daysUntilVisit(calculateDaysUntilVisit(visite))
                .build();
    }

    // Utility methods for implementation visits
    private String getVisiteStatus(VisiteImplementation visite) {
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

    private String getVisiteStatusDisplay(VisiteImplementation visite) {
        switch (getVisiteStatus(visite)) {
            case "PROGRAMMEE": return "Programmée";
            case "EN_RETARD": return "En retard";
            case "COMPLETEE": return "Complétée";
            case "APPROUVEE": return "Approuvée";
            case "REJETEE": return "Rejetée";
            default: return "Nouvelle";
        }
    }

    private Integer calculateDaysUntilVisit(VisiteImplementation visite) {
        if (visite.getDateVisite() == null) return null;
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), visite.getDateVisite());
    }

    private ImplementationVisitStatisticsDTO calculateImplementationVisitStatistics(List<VisiteImplementation> visites) {
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
        double avancementMoyen = visites.stream()
                .filter(v -> v.getPourcentageAvancement() != null)
                .mapToInt(VisiteImplementation::getPourcentageAvancement)
                .average()
                .orElse(0.0);

        return ImplementationVisitStatisticsDTO.builder()
                .totalVisites(total)
                .visitesEnAttente(enAttente)
                .visitesRealisees(realisees)
                .visitesApprouvees(approuvees)
                .visitesRejetees(rejetees)
                .visitesEnRetard(enRetard)
                .visitesAujourdHui(aujourd_hui)
                .visitesCetteSemaine(cetteSemaine)
                .tauxApprobation(tauxApprobation)
                .avancementMoyen(avancementMoyen)
                .build();
    }

    private List<String> getAvailableStatuses() {
        return List.of("NOUVELLE", "PROGRAMMEE", "EN_RETARD", "COMPLETEE", "APPROUVEE", "REJETEE");
    }

    private int getImplementationPhotosCount(Long visiteId) {
        // Get implementation photos count from PieceJointe
        return pieceJointeRepository.findByDossierIdAndDocumentType(
                visiteImplementationRepository.findById(visiteId).get().getDossier().getId(), 
                PieceJointe.DocumentType.TERRAIN_PHOTO
        ).size();
    }

    private List<PhotoImplementationDTO> getImplementationPhotos(Long visiteId) {
        // Get implementation photos from PieceJointe similar to terrain photos
        VisiteImplementation visite = visiteImplementationRepository.findById(visiteId).get();
        
        List<PieceJointe> photos = pieceJointeRepository.findByDossierIdAndDocumentType(
                visite.getDossier().getId(), PieceJointe.DocumentType.TERRAIN_PHOTO);

        // Filter photos that belong to this specific implementation visit
        return photos.stream()
                .filter(photo -> {
                    try {
                        Map<String, Object> metadata = photo.getFormDataAsMap();
                        Object typeFromMetadata = metadata.get("type");
                        Object visiteIdFromMetadata = metadata.get("visite_id");
                        return "implementation_photo".equals(typeFromMetadata) && 
                               visiteIdFromMetadata != null && 
                               Long.valueOf(visiteIdFromMetadata.toString()).equals(visiteId);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(this::mapToPhotoImplementationDTO)
                .collect(Collectors.toList());
    }

    private PhotoImplementationDTO mapToPhotoImplementationDTO(PieceJointe pieceJointe) {
        Map<String, Object> metadata = pieceJointe.getFormDataAsMap();
        String gpsCoordinates = (String) metadata.get("gps_coordinates");
        String photoDescription = (String) metadata.get("photo_description");
        
        return PhotoImplementationDTO.builder()
                .id(pieceJointe.getId())
                .nomFichier(pieceJointe.getNomFichier())
                .cheminFichier(pieceJointe.getCheminFichier())
                .description(photoDescription != null ? photoDescription : pieceJointe.getDescription())
                .coordonneesGPS(gpsCoordinates)
                .datePrise(pieceJointe.getDateUpload())
                .downloadUrl("/api/service-technique/visits/photos/" + pieceJointe.getId() + "/download")
                .build();
    }

    // Filter and sorting methods
    private List<VisiteImplementation> applyFilters(List<VisiteImplementation> visites, ImplementationVisitFilterRequest filter) {
        return visites.stream()
                .filter(v -> filter.getStatut() == null || matchesStatus(v, filter.getStatut()))
                .filter(v -> filter.getSearchTerm() == null || matchesSearchTerm(v, filter.getSearchTerm()))
                .filter(v -> filter.getAntenne() == null || matchesAntenne(v, filter.getAntenne()))
                .filter(v -> filter.getDateDebut() == null || !v.getDateVisite().isBefore(filter.getDateDebut()))
                .filter(v -> filter.getDateFin() == null || !v.getDateVisite().isAfter(filter.getDateFin()))
                .filter(v -> filter.getEnRetard() == null || filter.getEnRetard().equals(isVisiteOverdue(v)))
                .filter(v -> filter.getACompleter() == null || 
                        filter.getACompleter().equals(v.getDateConstat() == null))
                .filter(v -> filter.getPourcentageMin() == null || 
                        (v.getPourcentageAvancement() != null && v.getPourcentageAvancement() >= filter.getPourcentageMin()))
                .filter(v -> filter.getPourcentageMax() == null || 
                        (v.getPourcentageAvancement() != null && v.getPourcentageAvancement() <= filter.getPourcentageMax()))
                .collect(Collectors.toList());
    }

    private List<VisiteImplementation> applySorting(List<VisiteImplementation> visites, ImplementationVisitFilterRequest filter) {
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
                        case "pourcentageAvancement":
                            result = Integer.compare(
                                a.getPourcentageAvancement() != null ? a.getPourcentageAvancement() : 0,
                                b.getPourcentageAvancement() != null ? b.getPourcentageAvancement() : 0
                            );
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

    private boolean matchesStatus(VisiteImplementation visite, String statut) {
        return getVisiteStatus(visite).equalsIgnoreCase(statut);
    }

    private boolean matchesSearchTerm(VisiteImplementation visite, String searchTerm) {
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

    private boolean matchesAntenne(VisiteImplementation visite, String antenne) {
        return visite.getDossier().getAntenne().getDesignation()
                .toLowerCase().contains(antenne.toLowerCase());
    }
}