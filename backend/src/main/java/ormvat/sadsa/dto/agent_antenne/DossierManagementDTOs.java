package ormvat.sadsa.dto.agent_antenne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ormvat.sadsa.model.Dossier;
import ormvat.sadsa.model.WorkflowInstance;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DossierManagementDTOs {

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
        private String currentUserAntenne;
        private String currentUserCDA;
        private DossierStatisticsDTO statistics;
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
        
        // Permissions
        private Boolean peutEtreModifie;
        private Boolean peutEtreEnvoye;
        private Boolean peutEtreSupprime;
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
        
        // Permissions
        private Boolean peutEtreModifie;
        private Boolean peutEtreEnvoye;
        private Boolean peutEtreSupprime;
        private Boolean peutAjouterNotes;
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
        private String sortBy;
        private String sortDirection;
        private Integer page;
        private Integer size;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SendToGUCRequest {
        private Long dossierId;
        private String commentaire;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeleteDossierRequest {
        private Long dossierId;
        private String motif;
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
    }
}