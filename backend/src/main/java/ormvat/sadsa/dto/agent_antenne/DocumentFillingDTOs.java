package ormvat.sadsa.dto.agent_antenne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ormvat.sadsa.model.PieceJointe;

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
        private List<DocumentWithPiecesDTO> documentsRequis;
        private DocumentStatisticsDTO statistics;
        private NavigationInfoDTO navigationInfo;
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
    public static class DocumentWithPiecesDTO {
        private Long documentRequisId;
        private String nomDocument;
        private String description;
        private Boolean obligatoire;
        private String locationFormulaire; // Path to form structure JSON if exists
        
        // All pieces for this document requis
        private List<PieceJointeDTO> pieces;
        
        // Status and progress
        private String status; // MISSING, PARTIAL, COMPLETE
        private DocumentProgressDTO progress;
        private Map<String, Object> formStructure; // Parsed from locationFormulaire
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PieceJointeDTO {
        private Long id;
        private String nomFichier;
        private String cheminFichier;
        private PieceJointe.DocumentType documentType;
        private String title;
        private String description;
        private PieceJointe.DocumentStatus status;
        private Boolean isRequired;
        private LocalDateTime dateUpload;
        private LocalDateTime lastEdited;
        private String utilisateurNom;
        
        // Form data
        private Map<String, Object> formData;
        private Boolean hasFile;
        private Boolean hasFormData;
        
        // Actions
        private Boolean canEdit;
        private Boolean canDelete;
        private Boolean canDownload;
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
        private Integer totalPieces;
        private Integer piecesWithFiles;
        private Integer piecesWithFormData;
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

    // Request DTOs
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UploadDocumentRequest {
        private Long dossierId;
        private Long documentRequisId;
        private String title;
        private String description;
        private PieceJointe.DocumentType documentType;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SaveFormDataRequest {
        private Long dossierId;
        private Long documentRequisId;
        private Map<String, Object> formData;
        private String title;
        private String description;
        private Boolean isAutoSave = false;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SaveMixedDocumentRequest {
        private Long dossierId;
        private Long documentRequisId;
        private Map<String, Object> formData;
        private String title;
        private String description;
    }

    // Response DTOs
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UploadDocumentResponse {
        private Boolean success;
        private String message;
        private PieceJointeDTO uploadedPiece;
        private DocumentProgressDTO updatedProgress;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SaveFormDataResponse {
        private Boolean success;
        private String message;
        private PieceJointeDTO savedPiece;
        private DocumentProgressDTO updatedProgress;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeleteDocumentResponse {
        private Boolean success;
        private String message;
        private DocumentProgressDTO updatedProgress;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ApiResponse<T> {
        private Boolean success;
        private String message;
        private T data;
        private LocalDateTime timestamp;
        
        public static <T> ApiResponse<T> success(T data, String message) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message(message)
                    .data(data)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        
        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }
}