package ormvat.sadsa.controller.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentPreviewController {

    private final PieceJointeRepository pieceJointeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditTrailRepository auditTrailRepository;

    @Value("${app.upload.dir:./uploads/ormvat_sadsa}")
    private String uploadDir;

    private Path getStorageLocation() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Preview document (for images, PDFs, etc.)
     */
    @GetMapping("/{pieceJointeId}/preview")
    public ResponseEntity<Resource> previewDocument(
            @PathVariable Long pieceJointeId,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Preview request - Document ID: {}, User: {}", pieceJointeId, userEmail);
            
            PieceJointe pieceJointe = pieceJointeRepository.findById(pieceJointeId)
                    .orElseThrow(() -> new RuntimeException("Document non trouvé"));

            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            log.info("Found document: {}, User role: {}", pieceJointe.getNomFichier(), utilisateur.getRole());

            // Check access permissions
            if (!canAccessDocument(pieceJointe, utilisateur)) {
                log.warn("Access denied - User: {}, Role: {}, Document: {}", 
                    userEmail, utilisateur.getRole(), pieceJointeId);
                return ResponseEntity.status(403).build();
            }

            if (pieceJointe.getCheminFichier() == null) {
                log.error("File path is null for document: {}", pieceJointeId);
                return ResponseEntity.notFound().build();
            }

            Resource resource = loadFileAsResource(pieceJointe.getCheminFichier());
            String contentType = getMimeType(pieceJointe.getCheminFichier());
            
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Only allow preview for images and PDFs
            if (!isPreviewableType(contentType)) {
                log.warn("File type not previewable: {}", contentType);
                return ResponseEntity.status(415).build(); // Unsupported Media Type
            }

            log.info("Document preview successful - ID: {}, Type: {}, User: {}", 
                    pieceJointeId, contentType, userEmail);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error previewing document: {}", pieceJointeId, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Download document
     */
    @GetMapping("/{pieceJointeId}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long pieceJointeId,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Download request - Document ID: {}, User: {}", pieceJointeId, userEmail);
            
            PieceJointe pieceJointe = pieceJointeRepository.findById(pieceJointeId)
                    .orElseThrow(() -> new RuntimeException("Document non trouvé"));

            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            log.info("Found document: {}, User role: {}", pieceJointe.getNomFichier(), utilisateur.getRole());

            // Check access permissions
            if (!canAccessDocument(pieceJointe, utilisateur)) {
                log.warn("Download access denied - User: {}, Role: {}, Document: {}", 
                    userEmail, utilisateur.getRole(), pieceJointeId);
                return ResponseEntity.status(403).build();
            }

            if (pieceJointe.getCheminFichier() == null) {
                log.error("File path is null for download: {}", pieceJointeId);
                return ResponseEntity.notFound().build();
            }

            Resource resource = loadFileAsResource(pieceJointe.getCheminFichier());
            String contentType = getMimeType(pieceJointe.getCheminFichier());
            
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Create audit trail
            createAuditTrail("DOWNLOAD_DOCUMENT", pieceJointe.getDossier().getId(), 
                    "Téléchargement document: " + pieceJointe.getNomFichier(), userEmail);

            log.info("Document download successful - ID: {}, File: {}, User: {}", 
                    pieceJointeId, pieceJointe.getNomFichier(), userEmail);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + pieceJointe.getNomFichier() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading document: {}", pieceJointeId, e);
            return ResponseEntity.status(500).build();
        }
    }

    // Helper methods

    private boolean canAccessDocument(PieceJointe pieceJointe, Utilisateur utilisateur) {
        Dossier dossier = pieceJointe.getDossier();
        
        log.debug("Checking access - User role: {}, Dossier status: {}", 
            utilisateur.getRole(), dossier.getStatus());
        
        switch (utilisateur.getRole()) {
            case ADMIN:
                return true;
            case AGENT_ANTENNE:
                boolean antenneAccess = utilisateur.getAntenne() != null &&
                        dossier.getAntenne().getId().equals(utilisateur.getAntenne().getId());
                log.debug("Antenne access check: {}", antenneAccess);
                return antenneAccess;
            case AGENT_GUC:
                boolean gucAccess = !dossier.getStatus().equals(Dossier.DossierStatus.DRAFT);
                log.debug("GUC access check: {}", gucAccess);
                return gucAccess;
            case AGENT_COMMISSION_TERRAIN:
                boolean commissionAccess = dossier.getStatus().equals(Dossier.DossierStatus.SUBMITTED) ||
                        dossier.getStatus().equals(Dossier.DossierStatus.IN_REVIEW) ||
                        dossier.getStatus().equals(Dossier.DossierStatus.APPROVED) ||
                        dossier.getStatus().equals(Dossier.DossierStatus.REJECTED);
                log.debug("Commission access check: {}", commissionAccess);
                return commissionAccess;
            default:
                log.warn("Unknown role: {}", utilisateur.getRole());
                return false;
        }
    }

    private boolean isPreviewableType(String contentType) {
        return contentType.startsWith("image/") || 
               contentType.equals("application/pdf");
    }

    private Resource loadFileAsResource(String filePath) {
        try {
            Path file = getStorageLocation().resolve(filePath).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists()) {
                log.debug("File loaded successfully: {}", file.toString());
                return resource;
            } else {
                log.error("File not found: {}", file.toString());
                throw new RuntimeException("File not found: " + filePath);
            }
        } catch (MalformedURLException ex) {
            log.error("Malformed URL for file: {}", filePath, ex);
            throw new RuntimeException("File not found: " + filePath, ex);
        }
    }

    private String getMimeType(String filePath) {
        try {
            Path file = getStorageLocation().resolve(filePath).normalize();
            String mimeType = Files.probeContentType(file);
            log.debug("Detected MIME type: {} for file: {}", mimeType, filePath);
            return mimeType;
        } catch (IOException ex) {
            log.warn("Could not determine MIME type for file: {}", filePath, ex);
            return null;
        }
    }

    private void createAuditTrail(String action, Long dossierId, String description, String userEmail) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail).orElse(null);
            
            AuditTrail auditTrail = new AuditTrail();
            auditTrail.setAction(action);
            auditTrail.setEntite("PieceJointe");
            auditTrail.setEntiteId(dossierId);
            auditTrail.setDateAction(java.time.LocalDateTime.now());
            auditTrail.setUtilisateur(utilisateur);
            auditTrail.setDescription(description);
            
            auditTrailRepository.save(auditTrail);
        } catch (Exception e) {
            log.warn("Error creating audit trail", e);
        }
    }
}