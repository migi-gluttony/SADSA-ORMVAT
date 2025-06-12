package ormvat.sadsa.controller.agent_guc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ormvat.sadsa.service.agent_guc.AgentGUCDossierService;
import ormvat.sadsa.service.agent_guc.AgentGUCDocumentService;
import ormvat.sadsa.dto.role_based.RoleBasedDossierDTOs.*;
import ormvat.sadsa.dto.agent_guc.FicheApprobationDTOs.FinalApprovalRequest;
import ormvat.sadsa.dto.agent_guc.FicheApprobationDTOs.FinalApprovalResponse;

import jakarta.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/agent-guc/dossiers")
@RequiredArgsConstructor
@Slf4j
public class AgentGUCDossierController {

    private final AgentGUCDossierService dossierService;
    private final AgentGUCDocumentService documentService;

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

    @PostMapping("/assign-service-technique/{id}")
    public ResponseEntity<ActionResponse> assignToServiceTechnique(@PathVariable Long id,
                                                                 @Valid @RequestBody AssignServiceTechniqueRequest request,
                                                                 Authentication authentication) {
        try {
            ActionResponse response = dossierService.assignToServiceTechnique(id, request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur assignation service technique dossier {}: {}", id, e.getMessage());
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

    @GetMapping("/documents/preview/{documentId}")
    public ResponseEntity<Resource> previewDocument(@PathVariable Long documentId, Authentication authentication) {
        try {
            AgentGUCDocumentService.DocumentResource documentResource = 
                documentService.getDocumentForPreview(documentId, authentication.getName());
            
            File file = new File(documentResource.getFilePath());
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + documentResource.getFileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Erreur prévisualisation document {}: {}", documentId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/documents/download/{documentId}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId, Authentication authentication) {
        try {
            AgentGUCDocumentService.DocumentResource documentResource = 
                documentService.getDocumentForDownload(documentId, authentication.getName());
            
            File file = new File(documentResource.getFilePath());
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + documentResource.getFileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Erreur téléchargement document {}: {}", documentId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}