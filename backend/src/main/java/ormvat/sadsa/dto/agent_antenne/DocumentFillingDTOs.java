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
        private Map<String, Object> formStructure; // Parsed JSON form structure
        private List<PieceJointeDTO> fichiers;
        private Map<String, Object> formData; // Filled form data
        private String status; // PENDING, COMPLETE, MISSING
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
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UploadDocumentRequest {
        private Long dossierI;
        private Long documentRequisId;
        private String typeDocument;
        private String description;
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
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SaveFormDataRequest {
        private Long dossierId;
        private Long documentRequisId;
        private Map<String, Object> formData;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SaveFormDataResponse {
        private Boolean success;
        private String message;
        private Map<String, Object> savedData;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeleteDocumentRequest {
        private Long pieceJointeId;
        private String motif;
    }
}