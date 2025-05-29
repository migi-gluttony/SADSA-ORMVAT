package ormvat.sadsa.dto.agent_antenne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
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
        private List<AntenneInfoDTO> antennes; // Added list of all antennes
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
        private Long antenneId; // Changed from cdaId to antenneId
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
    public static class RecepisseDossierDTO {
        private String numeroRecepisse;
        private LocalDate dateDepot;
        private String nomComplet;
        private String cin;
        private String telephone;
        private String typeProduit;
        private String saba;
        private Double montantDemande;
        private String antenneName; // Changed from cdaNom to antenneName
        private String cdaName; // Keep CDA name for receipt
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