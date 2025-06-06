package ormvat.sadsa.controller.agent_commission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ormvat.sadsa.service.agent_commission.AgentCommissionDossierService;
import ormvat.sadsa.dto.role_based.RoleBasedDossierDTOs.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/agent-commission/dossiers")
@RequiredArgsConstructor
@Slf4j
public class AgentCommissionDossierController {

    private final AgentCommissionDossierService dossierService;

    @GetMapping
    public ResponseEntity<DossierListResponse> getDossiersForInspection(Authentication authentication) {
        try {
            DossierListResponse response = dossierService.getDossiersForInspection(authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur récupération dossiers commission: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/assigned")
    public ResponseEntity<DossierListResponse> getMyAssignedDossiers(Authentication authentication) {
        try {
            DossierListResponse response = dossierService.getMyAssignedDossiers(authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur récupération dossiers assignés: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<DossierDetailResponse> getDossierDetail(@PathVariable Long id, Authentication authentication) {
        try {
            DossierDetailResponse response = dossierService.getDossierById(id, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur récupération détail dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/approve-terrain/{id}")
    public ResponseEntity<ActionResponse> approveTerrainInspection(@PathVariable Long id,
                                                                  @Valid @RequestBody TerrainApprovalRequest request,
                                                                  Authentication authentication) {
        try {
            ActionResponse response = dossierService.approveTerrainInspection(id, request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur approbation terrain dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/reject-terrain/{id}")
    public ResponseEntity<ActionResponse> rejectTerrainInspection(@PathVariable Long id,
                                                                 @Valid @RequestBody TerrainRejectionRequest request,
                                                                 Authentication authentication) {
        try {
            ActionResponse response = dossierService.rejectTerrainInspection(id, request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur rejet terrain dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/schedule-visit/{id}")
    public ResponseEntity<ActionResponse> scheduleVisit(@PathVariable Long id,
                                                       @Valid @RequestBody ScheduleVisitRequest request,
                                                       Authentication authentication) {
        try {
            ActionResponse response = dossierService.scheduleVisit(id, request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur programmation visite dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
}