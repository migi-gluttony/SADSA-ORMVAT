package ormvat.sadsa.dto.agent_antenne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class DossierAntenneActionDTOs {

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
}