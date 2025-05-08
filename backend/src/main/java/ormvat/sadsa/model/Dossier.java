package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column
    private String saba;

    @Column
    private String reference;

    @ManyToOne
    @JoinColumn(name = "id_agriculteur")
    private Agriculteur agriculteur;

    @ManyToOne
    @JoinColumn(name = "id_cda")
    private CDA cda;

    @ManyToOne
    @JoinColumn(name = "id_sous_rubrique")
    private SousRubrique sousRubrique;

    @ManyToOne
    @JoinColumn(name = "id_etape")
    private Etape etapeActuelle;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL)
    private List<Historique> historiques;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL)
    private List<Note> notes;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL)
    private List<Trace> traces;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL)
    private List<VisiteTerrain> visites;
}