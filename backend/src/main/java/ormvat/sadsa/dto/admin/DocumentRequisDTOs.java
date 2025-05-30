package ormvat.sadsa.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public class DocumentRequisDTOs {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateDocumentRequisRequest {
        @NotBlank(message = "Le nom du document est requis")
        private String nomDocument;
        
        private String description;
        
        @NotNull(message = "La sous-rubrique est requise")
        private Long sousRubriqueId;
        
        @Builder.Default
        private Boolean obligatoire = true;
        
        // JSON properties for dynamic form structure
        private Map<String, Object> proprietes;
        
        // Note: locationFormulaire will be set automatically when JSON file is uploaded
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateDocumentRequisRequest {
        @NotNull(message = "L'ID du document est requis")
        private Long id;
        
        @NotBlank(message = "Le nom du document est requis")
        private String nomDocument;
        
        private String description;
        
        @Builder.Default
        private Boolean obligatoire = true;
        
        // JSON properties for dynamic form structure
        private Map<String, Object> proprietes;
        
        // Note: locationFormulaire will be updated automatically when JSON file is uploaded
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DocumentRequisResponse {
        private Long id;
        private String nomDocument;
        private String description;
        private Boolean obligatoire;
        private String locationFormulaire;
        private Long sousRubriqueId;
        private String sousRubriqueDesignation;
        private String rubriqueDesignation;
        private Map<String, Object> proprietes;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RubriqueWithDocumentsResponse {
        private Long id;
        private String designation;
        private String description;
        private List<SousRubriqueWithDocumentsDTO> sousRubriques;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SousRubriqueWithDocumentsDTO {
        private Long id;
        private String designation;
        private String description;
        private List<DocumentRequisResponse> documentsRequis;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DocumentRequisManagementResponse {
        private List<RubriqueWithDocumentsResponse> rubriques;
        private Long totalDocuments;
        private Long totalObligatoires;
        private Long totalOptionnels;
    }
}