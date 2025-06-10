package ormvat.sadsa.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "piece_jointe")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PieceJointe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_piece_jointe")
    private Long id;

    @Column(name = "nom_fichier")
    private String nomFichier;

    @Column(name = "chemin_fichier")
    private String cheminFichier;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "form_data", columnDefinition = "JSON")
    private String formData; // Store as JSON string

    @Column
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "is_validated")
    private Boolean isValidated = false;

    @Column(name = "validation_notes", columnDefinition = "TEXT")
    private String validationNotes;

    @Column(name = "date_upload", nullable = false)
    private LocalDateTime dateUpload;

    @Column(name = "last_edited")
    private LocalDateTime lastEdited;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    // Relationships
    @ManyToOne
    @JoinColumn(name = "id_dossier", nullable = false)
    private Dossier dossier;

    @ManyToOne
    @JoinColumn(name = "id_document_requis")
    private DocumentRequis documentRequis;

    @ManyToOne
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    // Enums
    public enum DocumentType {
        SCANNED_DOCUMENT,    // Scanned documents brought by farmer
        FORM_DATA,           // Data entered by agent from forms
        TERRAIN_PHOTO,       // Photos taken during terrain visits
        SUPPORTING_DOC,      // Additional supporting documents
        GENERATED_REPORT,    // System-generated reports
        MIXED                // Both file and form data
    }

    public enum DocumentStatus {
        PENDING, COMPLETE, REJECTED, MISSING, DRAFT
    }

    // Helper methods for JSON handling
    public Map<String, Object> getFormDataAsMap() {
        if (formData == null || formData.isEmpty()) {
            return new HashMap<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(formData, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public void setFormDataFromMap(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            this.formData = null;
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.formData = mapper.writeValueAsString(data);
            this.lastEdited = LocalDateTime.now();
        } catch (Exception e) {
            throw new RuntimeException("Error converting form data to JSON", e);
        }
    }

    // Convenience method to check if document has file
    public boolean hasFile() {
        return cheminFichier != null && !cheminFichier.isEmpty();
    }

    // Convenience method to check if document has form data
    public boolean hasFormData() {
        return formData != null && !formData.isEmpty();
    }
}