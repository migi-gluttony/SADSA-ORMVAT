package ormvat.sadsa.dto.agent_antenne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class DocumentFillingDTOs {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierDocumentsResponse {
        private DossierSummaryDTO dossier;
        private List<DocumentRequisWithFilesDTO> documentsRequis;
        private DocumentStatisticsDTO statistics;
        private NavigationInfoDTO navigationInfo;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NavigationInfoDTO {
        private String backUrl;
        private String dossierDetailUrl;
        private String dossierManagementUrl;
        private Boolean showBackButton;
        private String currentStep;
        private String nextStep;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierSummaryDTO {
        private Long id;
        private String numeroDossier;
        private String saba;
        private String reference;
        private String status;
        private LocalDateTime dateCreation;
        
        // Agriculteur info
        private String agriculteurNom;
        private String agriculteurPrenom;
        private String agriculteurCin;
        private String agriculteurTelephone;
        
        // Project info
        private String rubriqueDesignation;
        private String sousRubriqueDesignation;
        private String antenneDesignation;
        private Double montantDemande;

        // Status info
        private Boolean canEdit;
        private Boolean canSubmit;
        private String statusMessage;
        private String nextAction;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DocumentRequisWithFilesDTO {
        private Long id;
        private String nomDocument;
        private String description;
        private Boolean obligatoire;
        private String locationFormulaire;
        private Map<String, Object> formStructure;
        private List<PieceJointeGroupDTO> fichierGroups; // Grouped files
        private Map<String, Object> formData;
        private String status;
        private DocumentProgressDTO progress;
        private String documentCategory; // For grouping
        private Integer order; // Display order
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PieceJointeGroupDTO {
        private String groupName; // "Fichiers téléchargés", "Données formulaire", etc.
        private String groupType; // "FILES", "FORM_DATA", "MIXED"
        private List<PieceJointeDTO> files;
        private Integer totalFiles;
        private Long totalSize;
        private LocalDateTime lastUpdated;
        private Boolean isComplete;
        private String groupStatus;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PieceJointeDTO {
        private Long id;
        private String nomFichier;
        private String cheminFichier;
        private String typeDocument;
        private String status;
        private LocalDateTime dateUpload;
        private String utilisateurNom;
        private Long tailleFichier;
        private String formatFichier;
        private String displayName; // User-friendly name
        private String description;
        private Boolean canDelete;
        private Boolean canDownload;
        private String thumbnailUrl; // For images
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DocumentProgressDTO {
        private Boolean isRequired;
        private Boolean isComplete;
        private Boolean hasFiles;
        private Boolean hasFormData;
        private Double completionPercentage;
        private String nextStep;
        private List<String> missingElements;
        private List<String> completedElements;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DocumentStatisticsDTO {
        private Integer totalDocuments;
        private Integer documentsCompletes;
        private Integer documentsManquants;
        private Integer documentsOptionnels;
        private Double pourcentageCompletion;
        private Integer totalFiles;
        private Long totalFileSize;
        private Integer formsCompleted;
        private Integer formsTotal;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UploadDocumentRequest {
        private Long dossierId;
        private Long documentRequisId;
        private String typeDocument;
        private String description;
        private String displayName;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UploadDocumentResponse {
        private Long pieceJointeId;
        private String nomFichier;
        private String cheminFichier;
        private String message;
        private Boolean success;
        private PieceJointeDTO uploadedFile;
        private DocumentProgressDTO updatedProgress;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SaveFormDataRequest {
        private Long dossierId;
        private Long documentRequisId;
        private Map<String, Object> formData;
        private Boolean isProgressSave; // True for auto-save, false for final save
        private String saveType; // "AUTO", "MANUAL", "FINAL"
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SaveFormDataResponse {
        private Boolean success;
        private String message;
        private Map<String, Object> savedData;
        private DocumentProgressDTO updatedProgress;
        private LocalDateTime lastSaved;
        private String saveType;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeleteDocumentRequest {
        private Long pieceJointeId;
        private String motif;
        private Boolean confirmDelete;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeleteDocumentResponse {
        private Boolean success;
        private String message;
        private DocumentProgressDTO updatedProgress;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FormValidationRequest {
        private Long dossierId;
        private Long documentRequisId;
        private Map<String, Object> formData;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FormValidationResponse {
        private Boolean isValid;
        private List<FormValidationError> errors;
        private List<FormValidationWarning> warnings;
        private Map<String, Object> validatedData;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FormValidationError {
        private String field;
        private String message;
        private String errorCode;
        private String severity;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FormValidationWarning {
        private String field;
        private String message;
        private String warningCode;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BulkUploadRequest {
        private Long dossierId;
        private List<Long> documentRequisIds;
        private String uploadType; // "SINGLE_FORM", "MULTIPLE_FORMS"
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BulkUploadResponse {
        private Boolean success;
        private String message;
        private List<UploadDocumentResponse> uploadResults;
        private Integer totalUploaded;
        private Integer totalFailed;
        private DocumentStatisticsDTO updatedStatistics;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DocumentTemplateDTO {
        private Long documentRequisId;
        private String templateName;
        private String templateDescription;
        private String templateUrl;
        private String templateType; // "PDF", "WORD", "EXCEL"
        private Boolean isRequired;
        private List<String> instructions;
    }
}