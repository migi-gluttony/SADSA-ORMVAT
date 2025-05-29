package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "piece_jointe")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PieceJointe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_fichier")
    private String nomFichier;

    @Column(name = "chemin_fichier")
    private String cheminFichier;

    @Column(name = "type_document")
    private String typeDocument;

    @Enumerated(EnumType.STRING)
    @Column
    private DocumentStatus status;

    @Column(name = "chemin_donnees_formulaire")
    private String cheminDonneesFormulaire;

    @Column(name = "date_upload")
    private LocalDateTime dateUpload;

    @ManyToOne
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;

    @ManyToOne
    @JoinColumn(name = "document_requis_id")
    private DocumentRequis documentRequis;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    public enum DocumentStatus {
        PENDING,
        COMPLETE,
        REJECTED,
        MISSING
    }
}