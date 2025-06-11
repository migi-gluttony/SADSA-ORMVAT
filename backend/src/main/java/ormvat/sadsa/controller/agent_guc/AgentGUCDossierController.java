package ormvat.sadsa.controller.agent_guc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ormvat.sadsa.service.agent_guc.AgentGUCDossierService;
// import ormvat.sadsa.service.agent_guc.AgentGUCDocumentService; // Temporarily commented out
import ormvat.sadsa.dto.role_based.RoleBasedDossierDTOs.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/agent-guc/dossiers")
@RequiredArgsConstructor
@Slf4j
public class AgentGUCDossierController {

    private final AgentGUCDossierService dossierService;
    // Temporarily commented out to fix compilation issue
    // private final AgentGUCDocumentService documentService;

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

    @PostMapping("/approve/{id}")
    public ResponseEntity<ActionResponse> approveDossier(@PathVariable Long id,
                                                        @Valid @RequestBody ApproveRequest request,
                                                        Authentication authentication) {
        try {
            ActionResponse response = dossierService.approveDossier(id, request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur approbation dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
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
                            .build());        }
    }

    // TODO: Add document preview and download endpoints once AgentGUCDocumentService compilation issue is resolved
}