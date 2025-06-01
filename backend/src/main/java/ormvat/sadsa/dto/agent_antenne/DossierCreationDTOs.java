package ormvat.sadsa.dto.agent_antenne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DossierCreationDTOs {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InitializationDataResponse {
        private AntenneInfoDTO userAntenne;
        private List<SimplifiedRubriqueDTO> rubriques;
        private List<GeographicDTO> provinces;
        private List<AntenneInfoDTO> antennes;
        private String generatedSaba;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SimplifiedRubriqueDTO {
        private Long id;
        private String designation;
        private String description;
        private List<SimplifiedSousRubriqueDTO> sousRubriques;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SimplifiedSousRubriqueDTO {
        private Long id;
        private String designation;
        private String description;
        private List<String> documentsRequis;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateDossierRequest {
        private AgriculteurInfoDTO agriculteur;
        private DossierInfoDTO dossier;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateDossierRequest {
        private Long dossierId;
        private AgriculteurInfoDTO agriculteur;
        private DossierInfoDTO dossier;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AgriculteurInfoDTO {
        private String cin;
        private String nom;
        private String prenom;
        private String telephone;
        private Long communeRuraleId;
        private Long douarId;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierInfoDTO {
        private String saba;
        private String reference;
        private Long sousRubriqueId;
        private Long antenneId;
        private LocalDate dateDepot;
        private Double montantDemande;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateDossierResponse {
        private Long dossierId;
        private String numeroDossier;
        private String statut;
        private String message;
        private RecepisseDossierDTO recepisse;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateDossierResponse {
        private Long dossierId;
        private String message;
        private Boolean success;
        private LocalDateTime lastUpdated;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierEditResponse {
        private Long dossierId;
        private AgriculteurInfoDTO agriculteur;
        private DossierInfoDTO dossier;
        private String currentStatus;
        private Boolean canEdit;
        private String lastModified;
        private List<SimplifiedRubriqueDTO> rubriques;
        private List<GeographicDTO> provinces;
        private List<AntenneInfoDTO> antennes;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AgriculteurCheckResponse {
        private Boolean exists;
        private AgriculteurInfoDTO agriculteur;
        private String message;
        private List<DossierHistoryDTO> previousDossiers;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierHistoryDTO {
        private Long id;
        private String numeroDossier;
        private String saba;
        private String sousRubriqueDesignation;
        private String status;
        private LocalDateTime dateCreation;
        private String antenneDesignation;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RecepisseDossierDTO {
        private String numeroRecepisse;
        private LocalDate dateDepot;
        private String nomComplet;
        private String cin;
        private String telephone;
        private String typeProduit;
        private String saba;
        private String reference; // Added dossier reference
        private Double montantDemande;
        private String antenneName;
        private String cdaName;
        private String numeroSerie; // For tracking
        private LocalDateTime dateEmission;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AntenneInfoDTO {
        private Long id;
        private String designation;
        private String cdaNom;
        private Long cdaId;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GeographicDTO {
        private Long id;
        private String designation;
        private Long parentId;
    }
}