package ormvat.sadsa.service.agent_commission;

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
public class DossierCommissionService {

    /**
     * Get available actions for Commission agents
     */
    public List<AvailableActionDTO> getAvailableActionsForCommission() {
        List<AvailableActionDTO> actions = new ArrayList<>();

        actions.add(AvailableActionDTO.builder()
                .action("approve_terrain")
                .label("Approuver Terrain")
                .icon("pi-check-circle")
                .severity("success")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Approuver la conformité du terrain après visite")
                .build());

        actions.add(AvailableActionDTO.builder()
                .action("reject_terrain")
                .label("Rejeter Terrain")
                .icon("pi-times-circle")
                .severity("danger")
                .requiresComment(true)
                .requiresConfirmation(true)
                .description("Rejeter la demande suite à non-conformité du terrain")
                .build());

        return actions;
    }

    /**
     * Get available actions for specific dossier and commission user
     */
    public List<AvailableActionDTO> getAvailableActionsForDossier(Dossier dossier, Utilisateur utilisateur) {
        List<AvailableActionDTO> allActions = getAvailableActionsForCommission();
        
        // Commission can act on dossiers in review status
        boolean canAct = dossier.getStatus().equals(Dossier.DossierStatus.IN_REVIEW);
        
        if (!canAct) {
            return new ArrayList<>();
        }
        
        // Check if dossier belongs to agent's team
        if (utilisateur.getEquipeCommission() != null) {
            Long rubriqueId = dossier.getSousRubrique().getRubrique().getId();
            Utilisateur.EquipeCommission equipeRequise = Utilisateur.EquipeCommission.getTeamForRubrique(rubriqueId);
            if (!equipeRequise.equals(utilisateur.getEquipeCommission())) {
                return new ArrayList<>();
            }
        }
        
        return allActions;
    }
}