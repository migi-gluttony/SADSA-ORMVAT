package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

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

    @ManyToOne
    @JoinColumn(name = "id_dossier")
    private Dossier dossier;

    @ManyToOne
    @JoinColumn(name = "id_document_requis")
    private DocumentRequis documentRequis;

    @Column(name = "chemin_document_original")
    private String cheminDocumentOriginal;

    @Column(name = "donnees_formulaire_json", columnDefinition = "TEXT")
    private String donneesFormulaireJson;

    @Column(name = "date_upload")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateUpload;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutPieceJointe statut = StatutPieceJointe.PENDING;

    public enum StatutPieceJointe {
        PENDING, COMPLETE, REJECTED
    }
}