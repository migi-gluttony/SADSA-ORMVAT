package ormvat.sadsa.dto.agent_guc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class DossierGUCActionDTOs {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SendToCommissionRequest {
        private Long dossierId;
        private String commentaire;
        private String priorite;
        private LocalDateTime dateEcheance;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReturnToAntenneRequest {
        private Long dossierId;
        private String motif;
        private String commentaire;
        private List<String> documentsManquants;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RejectDossierRequest {
        private Long dossierId;
        private String motif;
        private String commentaire;
        private Boolean definitif;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdatePrioriteRequest {
        private Long dossierId;
        private String priorite;
        private String justification;
    }
}