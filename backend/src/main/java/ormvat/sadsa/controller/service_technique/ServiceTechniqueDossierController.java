package ormvat.sadsa.controller.service_technique;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ormvat.sadsa.service.service_technique.ServiceTechniqueDossierService;
import ormvat.sadsa.dto.role_based.RoleBasedDossierDTOs.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/service-technique/dossiers")
@RequiredArgsConstructor
@Slf4j
public class ServiceTechniqueDossierController {

    private final ServiceTechniqueDossierService dossierService;

    @GetMapping
    public ResponseEntity<DossierListResponse> getDossiersForImplementation(Authentication authentication) {
        try {
            DossierListResponse response = dossierService.getDossiersForImplementation(authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur récupération dossiers service technique: {}", e.getMessage());
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

    @PostMapping("/verify/{id}")
    public ResponseEntity<ActionResponse> verifyImplementation(@PathVariable Long id,
                                                              @Valid @RequestBody VerificationRequest request,
                                                              Authentication authentication) {
        try {
            ActionResponse response = dossierService.verifyImplementation(id, request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur vérification implémentation dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/complete/{id}")
    public ResponseEntity<ActionResponse> markComplete(@PathVariable Long id,
                                                      @Valid @RequestBody CompletionRequest request,
                                                      Authentication authentication) {
        try {
            ActionResponse response = dossierService.markComplete(id, request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur finalisation dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/report-issues/{id}")
    public ResponseEntity<ActionResponse> reportIssues(@PathVariable Long id,
                                                      @Valid @RequestBody IssueRequest request,
                                                      Authentication authentication) {
        try {
            ActionResponse response = dossierService.reportIssues(id, request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur signalement problèmes dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
}