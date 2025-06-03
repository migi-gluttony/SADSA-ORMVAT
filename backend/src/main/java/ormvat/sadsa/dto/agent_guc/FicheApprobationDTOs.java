package ormvat.sadsa.dto.agent_guc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class FicheApprobationDTOs {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GenerateFicheRequest {
        private Long dossierId;
        private String commentaireApprobation;
        private String observationsCommission;
        private BigDecimal montantApprouve;
        private String conditionsSpecifiques;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FicheApprobationResponse {
        private String numeroFiche;
        private LocalDate dateApprobation;
        private LocalDateTime dateGeneration;
        
        // Informations dossier
        private String numeroDossier;
        private String saba;
        private String reference;
        
        // Informations projet
        private String rubriqueDesignation;
        private String sousRubriqueDesignation;
        private BigDecimal montantDemande;
        private BigDecimal montantApprouve;
        
        // Informations agriculteur
        private String agriculteurNom;
        private String agriculteurPrenom;
        private String agriculteurCin;
        private String agriculteurTelephone;
        private String agriculteurCommune;
        private String agriculteurDouar;
        private String agriculteurProvince;
        
        // Informations administrative
        private String antenneDesignation;
        private String cdaNom;
        
        // Détails approbation
        private String statutApprobation;
        private String commentaireApprobation;
        private String observationsCommission;
        private String conditionsSpecifiques;
        private String validiteJusquau;
        
        // Informations signatures
        private String agentGucNom;
        private String agentGucSignature;
        private String responsableNom;
        private String responsableSignature;
        
        // Workflow info
        private String etapeActuelle;
        private String prochainEtape;
        private Boolean farmersRetrieved; // Si le fermier a récupéré la fiche
        private LocalDateTime dateRetrievalFermier;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PrintFicheRequest {
        private Long dossierId;
        private String format; // "PDF", "HTML"
        private Boolean includeDetails;
        private Boolean includeObservations;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FarmerRetrievalRequest {
        private Long dossierId;
        private String farmerCin;
        private String confirmationSignature;
        private LocalDateTime dateRetrieval;
        private String retrievedBy; // Nom de la personne qui récupère
        private String commentaire;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FarmerRetrievalResponse {
        private Boolean success;
        private String message;
        private LocalDateTime dateRetrieval;
        private String nextStep;
        private String antenneDestination;
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
}