package ormvat.sadsa.controller.agent_guc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ormvat.sadsa.service.agent_guc.AgentGUCDossierService;
import ormvat.sadsa.dto.role_based.RoleBasedDossierDTOs.*;
import ormvat.sadsa.dto.agent_guc.FicheApprobationDTOs.FinalApprovalRequest;
import ormvat.sadsa.dto.agent_guc.FicheApprobationDTOs.FinalApprovalResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/agent-guc/dossiers")
@RequiredArgsConstructor
@Slf4j
public class AgentGUCDossierController {

    private final AgentGUCDossierService dossierService;

    @GetMapping
    public ResponseEntity<DossierListResponse> getAllDossiers(Authentication authentication) {
        try {
            DossierListResponse response = dossierService.getAllDossiers(authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur récupération dossiers GUC: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<DossierListResponse> getPendingDossiers(Authentication authentication) {
        try {
            DossierListResponse response = dossierService.getPendingDossiers(authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur récupération dossiers en attente: {}", e.getMessage());
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

    @PostMapping("/assign-commission/{id}")
    public ResponseEntity<ActionResponse> assignToCommission(@PathVariable Long id,
                                                           @Valid @RequestBody AssignCommissionRequest request,
                                                           Authentication authentication) {
        try {
            ActionResponse response = dossierService.assignToCommission(id, request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur assignation commission dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/final-approval")
    public ResponseEntity<FinalApprovalResponse> processFinalApproval(@Valid @RequestBody FinalApprovalRequest request,
                                                                     Authentication authentication) {
        try {
            FinalApprovalResponse response = dossierService.processFinalApproval(request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur approbation finale dossier {}: {}", request.getDossierId(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(FinalApprovalResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/reject/{id}")
    public ResponseEntity<ActionResponse> rejectDossier(@PathVariable Long id,
                                                       @Valid @RequestBody RejectRequest request,
                                                       Authentication authentication) {
        try {
            ActionResponse response = dossierService.rejectDossier(id, request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur rejet dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/return/{id}")
    public ResponseEntity<ActionResponse> returnToAntenne(@PathVariable Long id,
                                                         @Valid @RequestBody ReturnRequest request,
                                                         Authentication authentication) {
        try {
            ActionResponse response = dossierService.returnToAntenne(id, request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur retour antenne dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
}