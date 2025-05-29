package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
    private Long id;

    @Column(name = "date_visite")
    private LocalDate dateVisite;

    @Column(name = "date_constat")
    private LocalDate dateConstat;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(columnDefinition = "TEXT")
    private String recommandations;

    @Column
    private Boolean approuve;

    @Column(name = "coordonnees_gps")
    private String coordonneesGPS;

    @ManyToOne
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;

    @ManyToOne
    @JoinColumn(name = "utilisateur_commission_id")
    private Utilisateur utilisateurCommission;

    @OneToMany(mappedBy = "visiteTerrain", cascade = CascadeType.ALL)
    private List<PhotoVisite> photos;
}
