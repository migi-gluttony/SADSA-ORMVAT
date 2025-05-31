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
        
        private List<UploadPhotoRequest> photos;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UploadPhotoRequest {
        private String nomFichier;
        private String description;
        private String coordonneesGPS;
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
        
        // Dossier info
        private Long dossierId;
        private String dossierReference;
        private String agriculteurNom;
        private String agriculteurPrenom;
        private String agriculteurTelephone;
        private String sousRubriqueDesignation;
        private String antenneDesignation;
        
        // Commission agent info
        private String utilisateurCommissionNom;
        
        // Photos
        private List<PhotoVisiteDTO> photos;
        
        // Workflow info
        private Boolean canSchedule;
        private Boolean canComplete;
        private Boolean canModify;
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
        
        // Dossier info
        private Long dossierId;
        private String dossierReference;
        private String saba;
        private String agriculteurNom;
        private String agriculteurPrenom;
        private String agriculteurTelephone;
        private String sousRubriqueDesignation;
        private String antenneDesignation;
        
        // Timing info
        private Integer joursRestants;
        private Boolean enRetard;
        
        // Actions available
        private Boolean canComplete;
        private Boolean canModify;
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
        private Double tauxApprobation;
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
    }
}