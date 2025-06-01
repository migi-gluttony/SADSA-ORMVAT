package ormvat.sadsa.dto.agent_commission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TerrainVisitDTOs {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScheduleVisitRequest {
        @NotNull(message = "L'ID du dossier est requis")
        private Long dossierId;
        
        @NotNull(message = "La date de visite est requise")
        private LocalDate dateVisite;
        
        @NotBlank(message = "Les observations sont requises")
        private String observations;
        
        private String coordonneesGPS;
        
        private String recommandations;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompleteVisitRequest {
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
        
        private List<String> pointsNonConformes;
        
        private String remarquesGenerales;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VisiteTerrainResponse {
        private Long id;
        private LocalDate dateVisite;
        private LocalDate dateConstat;
        private String observations;
        private String recommandations;
        private Boolean approuve;
        private String coordonneesGPS;
        private String statut;
        private String motifRejet;
        private List<String> pointsNonConformes;
        private String remarquesGenerales;
        
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
        
        // Commission agent info
        private String utilisateurCommissionNom;
        
        // Photos
        private List<PhotoVisiteDTO> photos;
        
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
    public static class PhotoVisiteDTO {
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
    public static class VisitListResponse {
        private List<VisiteTerrainSummaryDTO> visites;
        private Long totalCount;
        private Integer currentPage;
        private Integer pageSize;
        private Integer totalPages;
        private VisitStatisticsDTO statistics;
        private List<String> availableStatuses;
        private List<String> availableProjectTypes;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VisiteTerrainSummaryDTO {
        private Long id;
        private LocalDate dateVisite;
        private LocalDate dateConstat;
        private Boolean approuve;
        private String statut;
        private String statutDisplay;
        private Boolean isOverdue;
        private Integer daysUntilVisit;
        
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
    public static class VisitStatisticsDTO {
        private Long totalVisites;
        private Long visitesEnAttente;
        private Long visitesRealisees;
        private Long visitesApprouvees;
        private Long visitesRejetees;
        private Long visitesEnRetard;
        private Long visitesAujourdHui;
        private Long visitesCetteSemaine;
        private Double tauxApprobation;
        private Double tauxRejetRapide; // Rejets sans visite terrain
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TerrainVisitActionResponse {
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
    public static class UploadPhotosRequest {
        @NotNull(message = "L'ID de la visite est requis")
        private Long visiteId;
        
        private List<String> descriptions;
        private List<String> coordonnees;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateVisitNotesRequest {
        @NotNull(message = "L'ID de la visite est requis")
        private Long visiteId;
        
        private String observations;
        private String recommandations;
        private String remarquesGenerales;
        private List<String> pointsNonConformes;
        private String coordonneesGPS;
        
        // Auto-save functionality
        private Boolean isAutoSave;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class QuickActionRequest {
        @NotNull(message = "L'ID de la visite est requis")
        private Long visiteId;
        
        @NotBlank(message = "L'action est requise")
        private String action; // "approve", "reject", "postpone"
        
        private String motif;
        private LocalDate nouvelleDate; // For postpone
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VisitFilterRequest {
        private String statut;
        private String searchTerm;
        private String rubrique;
        private String antenne;
        private LocalDate dateDebut;
        private LocalDate dateFin;
        private Boolean enRetard;
        private Boolean aCompleter;
        private String sortBy;
        private String sortDirection;
        private Integer page;
        private Integer size;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DashboardSummaryDTO {
        private Long visitesAujourdHui;
        private Long visitesDemain;
        private Long visitesEnRetard;
        private Long visitesEnAttente;
        private List<VisiteTerrainSummaryDTO> prochaines;
        private List<VisiteTerrainSummaryDTO> urgentes;
        private VisitStatisticsDTO statistiques;
    }
}