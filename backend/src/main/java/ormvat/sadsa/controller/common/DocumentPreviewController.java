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
            
            PieceJointe pieceJointe = pieceJointeRepository.findById(pieceJointeId)
                    .orElseThrow(() -> new RuntimeException("Document non trouvé"));

            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Check access permissions
            if (!canAccessDocument(pieceJointe, utilisateur)) {
                return ResponseEntity.status(403).build();
            }

            if (pieceJointe.getCheminFichier() == null) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = loadFileAsResource(pieceJointe.getCheminFichier());
            String contentType = getMimeType(pieceJointe.getCheminFichier());
            
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Only allow preview for images and PDFs
            if (!isPreviewableType(contentType)) {
                return ResponseEntity.status(415).build(); // Unsupported Media Type
            }

            log.info("Document previewed - ID: {}, Type: {}, User: {}", 
                    pieceJointeId, contentType, userEmail);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .body(resource);

        } catch (Exception e) {
            log.error("Erreur lors de la prévisualisation du document: {}", pieceJointeId, e);
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
            
            PieceJointe pieceJointe = pieceJointeRepository.findById(pieceJointeId)
                    .orElseThrow(() -> new RuntimeException("Document non trouvé"));

            Utilisateur utilisateur = utilisateurRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Check access permissions
            if (!canAccessDocument(pieceJointe, utilisateur)) {
                return ResponseEntity.status(403).build();
            }

            if (pieceJointe.getCheminFichier() == null) {
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

            log.info("Document downloaded - ID: {}, File: {}, User: {}", 
                    pieceJointeId, pieceJointe.getNomFichier(), userEmail);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + pieceJointe.getNomFichier() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Erreur lors du téléchargement du document: {}", pieceJointeId, e);
            return ResponseEntity.status(500).build();
        }
    }

    // Helper methods

    private boolean canAccessDocument(PieceJointe pieceJointe, Utilisateur utilisateur) {
        Dossier dossier = pieceJointe.getDossier();
        
        switch (utilisateur.getRole()) {
            case ADMIN:
                return true;
            case AGENT_ANTENNE:
                return utilisateur.getAntenne() != null &&
                        dossier.getAntenne().getId().equals(utilisateur.getAntenne().getId());
            case AGENT_GUC:
                return !dossier.getStatus().equals(Dossier.DossierStatus.DRAFT);
            case AGENT_COMMISSION_TERRAIN:
                return dossier.getStatus().equals(Dossier.DossierStatus.SUBMITTED) ||
                        dossier.getStatus().equals(Dossier.DossierStatus.IN_REVIEW) ||
                        dossier.getStatus().equals(Dossier.DossierStatus.APPROVED) ||
                        dossier.getStatus().equals(Dossier.DossierStatus.REJECTED);
            default:
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
                return resource;
            } else {
                throw new RuntimeException("File not found: " + filePath);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found: " + filePath, ex);
        }
    }

    private String getMimeType(String filePath) {
        try {
            Path file = getStorageLocation().resolve(filePath).normalize();
            return Files.probeContentType(file);
        } catch (IOException ex) {
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
            log.warn("Erreur lors de la création de l'audit trail", e);
        }
    }
}