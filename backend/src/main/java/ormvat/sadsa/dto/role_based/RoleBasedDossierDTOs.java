package ormvat.sadsa.dto.role_based;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public class RoleBasedDossierDTOs {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierListResponse {
        private List<DossierSummaryDTO> dossiers;
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
        private String agriculteurNom;
        private String agriculteurCin;
        private String statut;
        private LocalDateTime dateCreation;
        private BigDecimal montantSubvention;
        private String currentStep;
        private Boolean enRetard;
        private Integer joursRetard;
        private Integer joursRestants;
        private String sousRubriqueDesignation;
        private String antenneDesignation;
        private String cdaNom;
        private Integer notesCount;
        private List<ActionDTO> availableActions;
        
        // Commission feedback fields
        private Boolean commissionDecisionMade;
        private Boolean commissionApproved;
        private String commissionComments;
        private LocalDateTime commissionDecisionDate;
        private String commissionAgentName;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DossierDetailResponse {
        private Long id;
        private String numeroDossier;
        private String saba;
        private String reference;
        private String statut;
        private LocalDateTime dateCreation;
        private LocalDateTime dateSubmission;
        private LocalDateTime dateApprobation;
        private BigDecimal montantSubvention;
        private AgriculteurDTO agriculteur;
        private AntenneDTO antenne;
        private ProjetDTO projet;
        private UtilisateurCreateurDTO utilisateurCreateur;
        private TimingDTO timing;
        private List<WorkflowHistoryDTO> workflowHistory;
        private List<DocumentDTO> documents;
        private List<ActionDTO> availableActions;
        
        // Commission feedback details
        private CommissionFeedbackDTO commissionFeedback;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CommissionFeedbackDTO {
        private Boolean decisionMade;
        private Boolean approved;
        private String observations;
        private String recommandations;
        private String conclusion;
        private LocalDateTime dateVisite;
        private LocalDateTime dateDecision;
        private String agentCommissionName;
        private String coordonneesGPS;
        private String conditionsMeteo;
        private Integer dureeVisite;
        private List<String> photosUrls;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AgriculteurDTO {
        private Long id;
        private String nom;
        private String prenom;
        private String cin;
        private String telephone;
        private String communeRurale;
        private String douar;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AntenneDTO {
        private Long id;
        private String designation;
        private String abreviation;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProjetDTO {
        private String rubrique;
        private String sousRubrique;
        private String description;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UtilisateurCreateurDTO {
        private Long id;
        private String nom;
        private String email;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TimingDTO {
        private String currentStep;
        private String assignedTo;
        private LocalDateTime dateEntree;
        private Integer delaiMaxJours;
        private Integer joursRestants;
        private Boolean enRetard;
        private Integer joursRetard;
        private LocalDateTime dateLimite;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WorkflowHistoryDTO {
        private Long id;
        private Long idEtape;
        private String phaseNom;
        private LocalDateTime dateEntree;
        private LocalDateTime dateSortie;
        private Long userId;
        private String userName;
        private String commentaire;
        private Integer dureeJours;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DocumentDTO {
        private Long id;
        private String nomDocument;
        private String cheminFichier;
        private String statut;
        private LocalDateTime dateUpload;
        private String documentType;
        private String title;
        private String description;
        private String formData;
        private String formConfig;
        private Boolean isValidated;
        private String validationNotes;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ActionDTO {
        private String action;
        private String label;
        private String endpoint;
        private String method;
    }

    // Request DTOs
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateDossierRequest {
        private AgriculteurCreateDTO agriculteur;
        private Long sousRubriqueId;
        private BigDecimal montantSubvention;
        private String saba;
        private String reference;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AgriculteurCreateDTO {
        private String nom;
        private String prenom;
        private String cin;
        private String telephone;
        private Long communeRuraleId;
        private Long douarId;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateDossierRequest {
        private AgriculteurCreateDTO agriculteur;
        private Long sousRubriqueId;
        private BigDecimal montantSubvention;
        private String saba;
        private String reference;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StartRealizationRequest {
        private String commentaire;
        private String typeReception; // Type of reception: "DEPOT_ATTESTATION", "NOTIFICATION_DIRECTE", etc.
        private String observations;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AssignCommissionRequest {
        private String commentaire;
        private String priorite;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AssignServiceTechniqueRequest {
        private String commentaire;
        private String priorite;
        private String typeRealisationPrevue; // Type of realization planned
        private String observationsSpecifiques; // Specific observations for Service Technique
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ApproveRequest {
        private String commentaire;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RejectRequest {
        private String motif;
        private String commentaire;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReturnRequest {
        private String motif;
        private String commentaire;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FinalApprovalRequest {
        private Long dossierId;
        private Boolean approved;
        private String commentaireApprobation;
        private String motifRejet;
        private BigDecimal montantApprouve;
        private String conditionsSpecifiques;
        private String observationsCommission;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FinalApprovalResponse {
        private Boolean success;
        private String message;
        private String newStatut;
        private String numeroFiche;
        private LocalDateTime dateAction;
        private Boolean ficheGenerated;
        private String nextStep;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TerrainApprovalRequest {
        private String commentaire;
        private String observations;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TerrainRejectionRequest {
        private String motif;
        private String observations;
        private String commentaire;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScheduleVisitRequest {
        private LocalDateTime dateVisite;
        private String commentaire;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VerificationRequest {
        private String commentaire;
        private String observations;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompletionRequest {
        private String commentaire;
        private String observations;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IssueRequest {
        private String probleme;
        private String commentaire;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ActionResponse {
        private Boolean success;
        private String message;
        private LocalDateTime timestamp;
        private String nextPhase; // Additional info about what happens next
        private String newStatus; // New dossier status
    }
}