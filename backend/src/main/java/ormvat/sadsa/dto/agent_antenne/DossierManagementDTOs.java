package ormvat.sadsa.dto.agent_antenne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class DossierManagementDTOs {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierListResponse {
        private List<DossierSummaryDTO> dossiers;
        private int totalCount;
        private int pageNumber;
        private int pageSize;
        private String currentUserCDA;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierSummaryDTO {
        private Long id;
        private String reference;
        private String saba;
        private String agriculteurNom;
        private String agriculteurPrenom;
        private String agriculteurCin;
        private String sousRubriqueDesignation;
        private Double montantDemande;
        private LocalDate dateCreation;
        private String statut;
        private int joursRestants;
        private boolean peutEtreModifie;
        private boolean peutEtreSupprime;
        private boolean peutEtreEnvoye;
        private int completionPercentage;
        private int formsCompleted;
        private int totalForms;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierDetailResponse {
        private DossierInfoDTO dossier;
        private AgriculteurInfoDTO agriculteur;
        private List<FormConfigurationDTO> availableForms;
        private List<PieceJointeDTO> pieceJointes;
        private int joursRestants;
        private boolean peutEtreModifie;
        private boolean peutEtreSupprime;
        private boolean peutEtreEnvoye;
        private List<String> validationErrors;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierInfoDTO {
        private Long id;
        private String reference;
        private String saba;
        private String sousRubriqueDesignation;
        private String rubriqueDesignation;
        private Double montantDemande;
        private LocalDate dateCreation;
        private LocalDate dateDepot;
        private String statut;
        private String cdaNom;
        private String antenneNom;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AgriculteurInfoDTO {
        private Long id;
        private String nom;
        private String prenom;
        private String cin;
        private String telephone;
        private String communeRurale;
        private String douar;
        private String cercle;
        private String province;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FormConfigurationDTO {
        private String formId;
        private String title;
        private String description;
        private Map<String, Object> formConfig;
        private boolean isCompleted;
        private LocalDate lastModified;
        private List<String> requiredDocuments;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PieceJointeDTO {
        private Long id;
        private String documentType;
        private String fileName;
        private String filePath;
        private String customTitle;
        private LocalDate dateUpload;
        private String statut;
        private Map<String, Object> formData;
        private boolean isOriginalDocument;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FormSubmissionRequest {
        private Long dossierId;
        private String formId;
        private Map<String, Object> formData;
        private List<FileUploadRequest> files;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileUploadRequest {
        private String fileName;
        private String customTitle;
        private boolean isOriginalDocument;
        private byte[] fileContent;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierActionRequest {
        private String action; // DELETE, SEND_TO_GUC
        private String comment;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierActionResponse {
        private boolean success;
        private String message;
        private String newStatus;
        private LocalDate actionDate;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierSearchRequest {
        private String searchTerm;
        private String statut;
        private LocalDate dateFrom;
        private LocalDate dateTo;
        private Long sousRubriqueId;
        private int page;
        private int size;
        private String sortBy;
        private String sortDirection;
    }
}