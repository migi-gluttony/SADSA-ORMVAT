package ormvat.sadsa.controller.agent_antenne;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ormvat.sadsa.service.agent_antenne.AgentAntenneDossierService;
import ormvat.sadsa.dto.role_based.RoleBasedDossierDTOs.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/agent-antenne/dossiers")
@RequiredArgsConstructor
@Slf4j
public class AgentAntenneDossierController {

    private final AgentAntenneDossierService dossierService;

    @GetMapping
    public ResponseEntity<DossierListResponse> getDossiers(Authentication authentication) {
        try {
            DossierListResponse response = dossierService.getMyAntenneDossiers(authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur récupération dossiers antenne: {}", e.getMessage());
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

    @PostMapping("/create")
    public ResponseEntity<ActionResponse> createDossier(@Valid @RequestBody CreateDossierRequest request, 
                                                       Authentication authentication) {
        try {
            ActionResponse response = dossierService.createDossier(request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur création dossier: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ActionResponse> updateDossier(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateDossierRequest request,
                                                       Authentication authentication) {
        try {
            ActionResponse response = dossierService.updateDossier(id, request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur modification dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/submit/{id}")
    public ResponseEntity<ActionResponse> submitDossier(@PathVariable Long id, Authentication authentication) {
        try {
            ActionResponse response = dossierService.submitDossier(id, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur soumission dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ActionResponse> deleteDossier(@PathVariable Long id, Authentication authentication) {
        try {
            ActionResponse response = dossierService.deleteDossier(id, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur suppression dossier {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ActionResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }
}