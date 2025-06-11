package ormvat.sadsa.service.agent_guc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ormvat.sadsa.model.*;
import ormvat.sadsa.repository.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentGUCDocumentService {

    private final PieceJointeRepository pieceJointeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DossierRepository dossierRepository;

    public DocumentResource getDocumentForPreview(Long documentId, String userEmail) {
        return getDocumentResource(documentId, userEmail, "preview");
    }

    public DocumentResource getDocumentForDownload(Long documentId, String userEmail) {
        return getDocumentResource(documentId, userEmail, "download");
    }

    private DocumentResource getDocumentResource(Long documentId, String userEmail, String action) {
        // Verify user exists and has GUC role
        Utilisateur user = utilisateurRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!user.getRole().equals(Utilisateur.UserRole.AGENT_GUC) && 
            !user.getRole().equals(Utilisateur.UserRole.ADMIN)) {
            throw new RuntimeException("Accès non autorisé - rôle insuffisant");
        }

        // Get the document
        PieceJointe document = pieceJointeRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document non trouvé"));

        // Verify the document belongs to a dossier the user can access
        Dossier dossier = document.getDossier();
        if (dossier == null) {
            throw new RuntimeException("Document non associé à un dossier");
        }

        // For GUC agents, they can access any document in the system
        // Additional access control can be added here if needed

        // Verify the document has a file path
        if (document.getCheminFichier() == null || document.getCheminFichier().isEmpty()) {
            throw new RuntimeException("Document ne contient pas de fichier");
        }

        log.info("User {} ({}) {} document {} from dossier {}", 
                user.getEmail(), user.getRole(), action, documentId, dossier.getId());

        return DocumentResource.builder()
                .filePath(document.getCheminFichier())
                .fileName(document.getNomFichier() != null ? document.getNomFichier() : "document")
                .documentId(documentId)
                .dossierId(dossier.getId())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentResource {
        private String filePath;
        private String fileName;
        private Long documentId;
        private Long dossierId;
    }
}