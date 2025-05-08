package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "visite_terrain")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisiteTerrain {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_visite")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_dossier")
    private Dossier dossier;

    @Column(name = "date_visite")
    @Temporal(TemporalType.DATE)
    private Date dateVisite;

    @Column(name = "date_constat")
    @Temporal(TemporalType.DATE)
    private Date dateConstat;

    @Column(length = 1000)
    private String observations;

    @Column
    private Boolean approuve;

    @ManyToOne
    @JoinColumn(name = "id_utilisateur_commission")
    private Utilisateur utilisateurCommission;

    @OneToMany(mappedBy = "visite", cascade = CascadeType.ALL)
    private List<PhotoDocumentation> photos;
}
