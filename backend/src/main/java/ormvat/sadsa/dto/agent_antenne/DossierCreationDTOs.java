package ormvat.sadsa.dto.agent_antenne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class DossierCreationDTOs {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateDossierRequest {
        // Informations agriculteur
        private AgriculteurInfoDTO agriculteur;
        
        // Informations dossier
        private DossierInfoDTO dossier;
        
        // Formulaires dynamiques
        private Map<String, Object> formulairesDynamiques;
        
        // Métadonnées
        private String userAgent;
        private String ipAddress;
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
        private String cdaNom;
        private String antenne;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RubriquesResponse {
        private List<RubriqueDTO> rubriques;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RubriqueDTO {
        private Long id;
        private String designation;
        private List<SousRubriqueDTO> sousRubriques;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SousRubriqueDTO {
        private Long id;
        private String designation;
        private String codeType;
        private List<DocumentRequiredDTO> documentsRequis;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DocumentRequiredDTO {
        private String nom;
        private boolean uploadRequired;
        private boolean formRequired;
        private String formConfigPath; // Chemin vers le fichier JSON de configuration
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CDAInfoDTO {
        private Long id;
        private String description;
        private String antenneNom;
        private Long antenneId;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ValidationErrorResponse {
        private Map<String, String> fieldErrors;
        private List<String> globalErrors;
        private String message;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierValidationDTO {
        private boolean isValid;
        private List<String> missingFields;
        private List<String> invalidFields;
        private Map<String, String> suggestions;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierSummaryDTO {
        private AgriculteurInfoDTO agriculteur;
        private DossierInfoDTO dossier;
        private String rubriqueNom;
        private String sousRubriqueNom;
        private String cdaNom;
        private List<FormulaireRempliDTO> formulairesRemplis;
        private int nombreDocumentsUpload;
        private LocalDate dateCreation;
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
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FormulaireRempliDTO {
        private String nomFormulaire;
        private Map<String, Object> donnees;
        private boolean isComplete;
    }

    // Énumérations pour les statuts
    public enum StatutDossier {
        BROUILLON("Brouillon"),
        SOUMIS("Soumis"),
        EN_COURS("En cours de traitement"),
        APPROUVE("Approuvé"),
        REJETE("Rejeté"),
        REALISE("Réalisé");

        private final String libelle;

        StatutDossier(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }
    }

    public enum TypeDocument {
        ENGAGEMENT_CONSERVATION("MODELE ENGAGEMENT DE CONSERVATION DE L'INVESTISSEMENT"),
        DEMANDE_APPROBATION("Modele-demande-approbation_prealable"),
        DEMANDE_SUBVENTION("Modele-demande-subvention"),
        ACCORD_PRINCIPE("Demand_daccord_principe_fr"),
        FICHE_DESCRIPTIVE("FICHE DESCRIPTIVE PLANTATION"),
        ETUDE_TECHNICO("MODELE ETUDE TECHNICO ECONOMIQUE UNITE VALORISATION FDA version 2019(1)"),
        FICHE_IAA("FICHE IAA approbation version 2019"),
        ANNEXES_GAG("Annexes GAG exigé par le circulaire");

        private final String libelle;

        TypeDocument(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }
    }
}