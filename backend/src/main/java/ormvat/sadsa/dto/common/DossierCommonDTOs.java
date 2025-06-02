package ormvat.sadsa.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ormvat.sadsa.model.WorkflowInstance;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DossierCommonDTOs {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierListResponse {
        private List<DossierSummaryDTO> dossiers;
        private Long totalCount;
        private Integer currentPage;
        private Integer pageSize;
        private Integer totalPages;
        private String currentUserRole;
        private String currentUserAntenne;
        private String currentUserCDA;
        private DossierStatisticsDTO statistics;
        private List<AvailableActionDTO> availableActions;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierDetailResponse {
        private DossierDetailDTO dossier;
        private AgriculteurDetailDTO agriculteur;
        private List<WorkflowHistoryDTO> historiqueWorkflow;
        private List<AvailableFormDTO> availableForms;
        private List<PieceJointeDetailDTO> pieceJointes;
        private List<NoteDTO> notes;
        private List<String> validationErrors;
        
        // Workflow info
        private Integer joursRestants;
        private Boolean enRetard;
        private String etapeActuelle;
        private WorkflowInstance.EmplacementType emplacementActuel;
        
        // Role-based permissions
        private DossierPermissionsDTO permissions;
        private List<AvailableActionDTO> availableActions;
        
        // User context
        private String currentUserRole;
        private String currentUserAntenne;
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
        private String statut;
        private LocalDateTime dateCreation;
        private LocalDateTime dateSubmission;
        
        // Agriculteur info
        private String agriculteurNom;
        private String agriculteurPrenom;
        private String agriculteurCin;
        private String agriculteurTelephone;
        
        // Project info
        private String rubriqueDesignation;
        private String sousRubriqueDesignation;
        private String antenneDesignation;
        private String cdaNom;
        private BigDecimal montantSubvention;
        
        // Workflow info
        private String etapeActuelle;
        private WorkflowInstance.EmplacementType emplacementActuel;
        private Integer joursRestants;
        private Boolean enRetard;
        private LocalDateTime dateLimite;
        
        // Progress info
        private Integer formsCompleted;
        private Integer totalForms;
        private Double completionPercentage;
        
        // Role-based permissions
        private List<String> availableActions;
        private DossierPermissionsDTO permissions;
        
        // Additional info
        private Integer notesCount;
        private Boolean hasUnreadNotes;
        private String priorite;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierDetailDTO {
        private Long id;
        private String numeroDossier;
        private String saba;
        private String reference;
        private String statut;
        private LocalDateTime dateCreation;
        private LocalDateTime dateSubmission;
        private LocalDateTime dateApprobation;
        private BigDecimal montantSubvention;
        
        // Related entities
        private String rubriqueDesignation;
        private String sousRubriqueDesignation;
        private String antenneDesignation;
        private String cdaNom;
        private String utilisateurCreateurNom;
        
        // GUC specific
        private String priorite;
        private String commentaireGUC;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AgriculteurDetailDTO {
        private Long id;
        private String nom;
        private String prenom;
        private String cin;
        private String telephone;
        private String communeRurale;
        private String douar;
        private String province;
        private String cercle;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WorkflowHistoryDTO {
        private Long id;
        private String etapeDesignation;
        private WorkflowInstance.EmplacementType emplacementType;
        private LocalDateTime dateEntree;
        private LocalDateTime dateSortie;
        private Integer dureeJours;
        private Boolean enRetard;
        private String utilisateurNom;
        private String commentaire;
        private String action;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AvailableFormDTO {
        private String formId;
        private String title;
        private String description;
        private Boolean isCompleted;
        private LocalDateTime lastModified;
        private List<String> requiredDocuments;
        private Map<String, Object> formConfig;
        private Map<String, Object> formData;
        private String documentType;
        private Boolean isReadOnly;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PieceJointeDetailDTO {
        private Long id;
        private String nomFichier;
        private String cheminFichier;
        private String typeDocument;
        private String status;
        private LocalDateTime dateUpload;
        private String utilisateurNom;
        private String customTitle;
        private Boolean isOriginalDocument;
        private Long tailleFichier;
        private String formatFichier;
        private Boolean canDownload;
        private Boolean canDelete;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NoteDTO {
        private Long id;
        private String objet;
        private String contenu;
        private String reponse;
        private LocalDateTime dateCreation;
        private LocalDateTime dateReponse;
        private String typeNote;
        private String priorite;
        private String utilisateurExpediteurNom;
        private String utilisateurDestinataireNom;
        private Boolean isRead;
        private String statut;
        private Boolean canReply;
        private Boolean canEdit;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierStatisticsDTO {
        private Long totalDossiers;
        private Long dossiersEnCours;
        private Long dossiersApprouves;
        private Long dossiersRejetes;
        private Long dossiersEnRetard;
        private Double tauxCompletion;
        private Long dossiersCeMois;
        private Long dossiersAttenteTraitement;
        private Long dossiersEnCommission;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierFilterRequest {
        private String searchTerm;
        private String statut;
        private Long sousRubriqueId;
        private WorkflowInstance.EmplacementType emplacement;
        private LocalDateTime dateDebutCreation;
        private LocalDateTime dateFinCreation;
        private Boolean enRetard;
        private String priorite;
        private Long antenneId;
        private String sortBy;
        private String sortDirection;
        private Integer page;
        private Integer size;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierPermissionsDTO {
        private Boolean peutEtreModifie;
        private Boolean peutEtreEnvoye;
        private Boolean peutEtreSupprime;
        private Boolean peutAjouterNotes;
        private Boolean peutRetournerAntenne;
        private Boolean peutEnvoyerCommission;
        private Boolean peutRejeter;
        private Boolean peutApprouver;
        private Boolean peutVoirDocuments;
        private Boolean peutTelechargerDocuments;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AvailableActionDTO {
        private String action;
        private String label;
        private String icon;
        private String severity;
        private Boolean requiresComment;
        private Boolean requiresConfirmation;
        private String description;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierActionResponse {
        private Boolean success;
        private String message;
        private String newStatut;
        private LocalDateTime dateAction;
        private Map<String, Object> additionalData;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AddNoteRequest {
        private Long dossierId;
        private String objet;
        private String contenu;
        private String typeNote;
        private String priorite;
        private Long utilisateurDestinataireId;
        private Boolean requiresResponse;
    }
}   