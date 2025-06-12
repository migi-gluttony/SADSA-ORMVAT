package ormvat.sadsa.dto.service_technique;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ImplementationVisitDTOs {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScheduleImplementationVisitRequest {
        @NotNull(message = "L'ID du dossier est requis")
        private Long dossierId;
        
        @NotNull(message = "La date de visite est requise")
        private LocalDate dateVisite;
        
        @NotBlank(message = "Les observations sont requises")
        private String observations;
        
        private String coordonneesGPS;
        
        private String recommandations;
        
        private Integer pourcentageAvancement;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompleteImplementationVisitRequest {
        @NotNull(message = "L'ID de la visite est requis")
        private Long visiteId;
        
        @NotNull(message = "La date de constat est requise")
        private LocalDate dateConstat;
        
        @NotBlank(message = "Les observations sont requises")
        private String observations;
        
        private String recommandations;
        
        @NotNull(message = "La décision d'approbation est requise")
        private Boolean approuve;
        
        private String coordonneesGPS;
        
        private String motifRejet;
        
        private String conclusion;
        
        private String problemesDetectes;
        
        private String actionsCorrectives;
        
        @NotNull(message = "Le pourcentage d'avancement est requis")
        private Integer pourcentageAvancement;
        
        private Integer dureeVisite;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VisiteImplementationResponse {
        private Long id;
        private LocalDate dateVisite;
        private LocalDate dateConstat;
        private String observations;
        private String recommandations;
        private Boolean approuve;
        private String coordonneesGPS;
        private String statut;
        private String motifRejet;
        private String conclusion;
        private String problemesDetectes;
        private String actionsCorrectives;
        private Integer pourcentageAvancement;
        private Integer dureeVisite;
        
        // Dossier info
        private Long dossierId;
        private String dossierReference;
        private String dossierSaba;
        private String dossierStatut;
        private String agriculteurNom;
        private String agriculteurPrenom;
        private String agriculteurCin;
        private String agriculteurTelephone;
        private String agriculteurCommune;
        private String agriculteurDouar;
        private String rubriqueDesignation;
        private String sousRubriqueDesignation;
        private String antenneDesignation;
        private String cdaNom;
        
        // Service technique agent info
        private String utilisateurServiceTechniqueNom;
        
        // Photos
        private List<PhotoImplementationDTO> photos;
        
        // Workflow info
        private Boolean canSchedule;
        private Boolean canComplete;
        private Boolean canModify;
        private Boolean isOverdue;
        private Integer daysUntilVisit;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PhotoImplementationDTO {
        private Long id;
        private String nomFichier;
        private String cheminFichier;
        private String description;
        private String coordonneesGPS;
        private LocalDateTime datePrise;
        private String downloadUrl;
        private Long tailleFichier;
        private String formatFichier;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImplementationVisitListResponse {
        private List<VisiteImplementationSummaryDTO> visites;
        private Long totalCount;
        private Integer currentPage;
        private Integer pageSize;
        private Integer totalPages;
        private ImplementationVisitStatisticsDTO statistics;
        private List<String> availableStatuses;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VisiteImplementationSummaryDTO {
        private Long id;
        private LocalDate dateVisite;
        private LocalDate dateConstat;
        private Boolean approuve;
        private String statut;
        private String statutDisplay;
        private Boolean isOverdue;
        private Integer daysUntilVisit;
        private Integer pourcentageAvancement;
        
        // Dossier info
        private Long dossierId;
        private String dossierReference;
        private String saba;
        private String agriculteurNom;
        private String agriculteurPrenom;
        private String agriculteurCin;
        private String agriculteurTelephone;
        private String rubriqueDesignation;
        private String sousRubriqueDesignation;
        private String antenneDesignation;
        private String agriculteurCommune;
        private String agriculteurDouar;
        
        // Quick actions
        private Boolean canComplete;
        private Boolean canModify;
        private Boolean canView;
        private Integer photosCount;
        private Boolean hasNotes;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImplementationVisitStatisticsDTO {
        private Long totalVisites;
        private Long visitesEnAttente;
        private Long visitesRealisees;
        private Long visitesApprouvees;
        private Long visitesRejetees;
        private Long visitesEnRetard;
        private Long visitesAujourdHui;
        private Long visitesCetteSemaine;
        private Double tauxApprobation;
        private Double avancementMoyen;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImplementationVisitActionResponse {
        private Boolean success;
        private String message;
        private Long visiteId;
        private String newStatut;
        private LocalDateTime dateAction;
        private String redirectUrl;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateImplementationVisitNotesRequest {
        @NotNull(message = "L'ID de la visite est requis")
        private Long visiteId;
        
        private String observations;
        private String recommandations;
        private String conclusion;
        private String problemesDetectes;
        private String actionsCorrectives;
        private String coordonneesGPS;
        private Integer pourcentageAvancement;
        
        // Auto-save functionality
        private Boolean isAutoSave;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImplementationVisitFilterRequest {
        private String statut;
        private String searchTerm;
        private String antenne;
        private LocalDate dateDebut;
        private LocalDate dateFin;
        private Boolean enRetard;
        private Boolean aCompleter;
        private Integer pourcentageMin;
        private Integer pourcentageMax;
        private String sortBy;
        private String sortDirection;
        private Integer page;
        private Integer size;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImplementationDashboardSummaryDTO {
        private Long visitesAujourdHui;
        private Long visitesDemain;
        private Long visitesEnRetard;
        private Long visitesEnAttente;
        private List<VisiteImplementationSummaryDTO> prochaines;
        private List<VisiteImplementationSummaryDTO> urgentes;
        private ImplementationVisitStatisticsDTO statistiques;
        private Double avancementMoyenGlobal;
        private Long projetsTermines;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class QuickImplementationActionRequest {
        @NotNull(message = "L'ID de la visite est requis")
        private Long visiteId;
        
        @NotBlank(message = "L'action est requise")
        private String action; // "approve", "reject", "postpone", "update_progress"
        
        private String motif;
        private LocalDate nouvelleDate; // For postpone
        private Integer nouveauPourcentage; // For update_progress
    }
}