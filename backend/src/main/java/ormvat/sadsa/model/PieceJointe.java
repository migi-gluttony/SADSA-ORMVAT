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
    @Column(name = "id_piece_jointe")
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
    @JoinColumn(name = "id_dossier")
    private Dossier dossier;

    @ManyToOne
    @JoinColumn(name = "id_document_requis")
    private DocumentRequis documentRequis;

    @ManyToOne
    @JoinColumn(name = "id_utilisateur")
    private Utilisateur utilisateur;

    public enum DocumentStatus {
        PENDING,
        COMPLETE,
        REJECTED,
        MISSING
    }
}