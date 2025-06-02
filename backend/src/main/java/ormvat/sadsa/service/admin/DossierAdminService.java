package ormvat.sadsa.service.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ormvat.sadsa.dto.common.DossierCommonDTOs.*;
import ormvat.sadsa.model.*;

import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class DossierAdminService {

    /**
     * Get available actions for Admin users
     */
    public List<AvailableActionDTO> getAvailableActionsForAdmin() {
        List<AvailableActionDTO> actions = new ArrayList<>();

        actions.add(AvailableActionDTO.builder()
                .action("send_to_guc")
                .label("Envoyer au GUC")
                .icon("pi-send")
                .severity("success")
                .requiresComment(false)
                .requiresConfirmation(true)
                .description("Envoyer le dossier au Guichet Unique Central pour traitement")
                .build());

        actions.add(AvailableActionDTO.builder()
                .action("send_to_commission")
                .label("Envoyer à la Commission")
                .icon("pi-forward")
                .severity("info")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Envoyer le dossier à la Commission de Vérification Terrain")
                .build());

        actions.add(AvailableActionDTO.builder()
                .action("return_to_antenne")
                .label("Retourner à l'Antenne")
                .icon("pi-undo")
                .severity("warning")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Retourner le dossier à l'antenne pour complétion")
                .build());

        actions.add(AvailableActionDTO.builder()
                .action("reject")
                .label("Rejeter")
                .icon("pi-times-circle")
                .severity("danger")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Rejeter définitivement le dossier")
                .build());

        actions.add(AvailableActionDTO.builder()
                .action("approve")
                .label("Approuver")
                .icon("pi-check-circle")
                .severity("success")
                .requiresComment(false)
                .requiresConfirmation(true)
                .description("Approuver définitivement le dossier")
                .build());

        return actions;
    }

    /**
     * Get available actions for specific dossier and admin user
     */
    public List<AvailableActionDTO> getAvailableActionsForDossier(Dossier dossier, Utilisateur utilisateur) {
        // Admin can perform all actions on any dossier
        return getAvailableActionsForAdmin();
    }
}