package ormvat.sadsa.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ormvat.sadsa.service.admin.AdminDossierService;
import ormvat.sadsa.service.workflow.AuditService;
import ormvat.sadsa.dto.role_based.RoleBasedDossierDTOs.*;
import ormvat.sadsa.model.AuditTrail;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dossiers")
@RequiredArgsConstructor
@Slf4j
public class AdminDossierController {

    private final AdminDossierService dossierService;
    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<DossierListResponse> getAllDossiers(Authentication authentication) {
        try {
            DossierListResponse response = dossierService.getAllDossiers(authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur récupération dossiers admin: {}", e.getMessage());
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

    @PostMapping("/force-status/{id}")
    public ResponseEntity<ActionResponse> forceStatusChange(@PathVariable Long id,
                                                           @Valid @RequestBody AdminForceStatusRequest request,
                                                           Authentication authentication) {
        try {
            ActionResponse response = dossierService.forceStatusChange(id, request.getNewStatus(), 
                                                                      request.getReason(), authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur force statut dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/force-workflow/{id}")
    public ResponseEntity<ActionResponse> forceWorkflowMove(@PathVariable Long id,
                                                           @Valid @RequestBody AdminForceWorkflowRequest request,
                                                           Authentication authentication) {
        try {
            ActionResponse response = dossierService.forceWorkflowMove(id, request.getTargetPhase(), 
                                                                      request.getReason(), authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur force workflow dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ActionResponse> deleteDossier(@PathVariable Long id,
                                                       @Valid @RequestBody AdminDeleteRequest request,
                                                       Authentication authentication) {
        try {
            ActionResponse response = dossierService.deleteDossier(id, request.getReason(), authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur suppression admin dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/audit/{id}")
    public ResponseEntity<List<AuditTrail>> getAuditTrail(@PathVariable Long id, Authentication authentication) {
        try {
            List<AuditTrail> auditTrail = auditService.getAuditHistory(id, "Dossier");
            return ResponseEntity.ok(auditTrail);
        } catch (Exception e) {
            log.error("Erreur récupération audit trail dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Additional DTOs for Admin requests
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AdminForceStatusRequest {
        private String newStatus;
        private String reason;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AdminForceWorkflowRequest {
        private Long targetPhase;
        private String reason;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class AdminDeleteRequest {
        private String reason;
    }
}