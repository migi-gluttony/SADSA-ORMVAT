package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "dossier")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dossier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dossier")
    private Long id;

    @Column(name = "numero_dossier", unique = true)
    private String numeroDossier;

    @Column
    private String saba;

    @Column
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column
    private DossierStatus status;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_submission")
    private LocalDateTime dateSubmission;

    @Column(name = "date_approbation")
    private LocalDateTime dateApprobation;

    @Column(name = "montant_subvention")
    private BigDecimal montantSubvention;

    @ManyToOne
    @JoinColumn(name = "agriculteur_id")
    private Agriculteur agriculteur;

    @ManyToOne
    @JoinColumn(name = "antenne_id") // Changed from cda_id to antenne_id
    private Antenne antenne;

    @ManyToOne
    @JoinColumn(name = "sous_rubrique_id")
    private SousRubrique sousRubrique;

    @ManyToOne
    @JoinColumn(name = "utilisateur_createur_id")
    private Utilisateur utilisateurCreateur;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL)
    private List<WorkflowInstance> workflowInstances;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL)
    private List<PieceJointe> pieceJointes;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL)
    private List<Note> notes;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL)
    private List<VisiteTerrain> visites;

    public enum DossierStatus {
        DRAFT,
        SUBMITTED,
        IN_REVIEW,
        APPROVED,
        APPROVED_AWAITING_FARMER,
        REALIZATION_IN_PROGRESS,
        REJECTED,
        COMPLETED,
        CANCELLED,
        RETURNED_FOR_COMPLETION,
        PENDING_CORRECTION
    }
}