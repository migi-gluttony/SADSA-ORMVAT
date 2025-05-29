package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "historique_workflow")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoriqueWorkflow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historique_workflow")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_dossier")
    private Dossier dossier;

    @Column(name = "etape_designation")
    private String etapeDesignation;

    @Enumerated(EnumType.STRING)
    @Column(name = "emplacement_type")
    private WorkflowInstance.EmplacementType emplacementType;

    @Column(name = "date_entree")
    private LocalDateTime dateEntree;

    @Column(name = "date_sortie")
    private LocalDateTime dateSortie;

    @Column(name = "duree_jours")
    private Integer dureeJours;

    @Column(name = "en_retard")
    private Boolean enRetard = false;

    @ManyToOne
    @JoinColumn(name = "id_utilisateur")
    private Utilisateur utilisateur;

    @Column
    private String commentaire;

    @ManyToOne
    @JoinColumn(name = "id_etape")
    private Etape etape;
}
