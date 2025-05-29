package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "photo_visite")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoVisite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_fichier")
    private String nomFichier;

    @Column(name = "chemin_fichier")
    private String cheminFichier;

    @Column
    private String description;

    @Column(name = "coordonnees_gps")
    private String coordonneesGPS;

    @Column(name = "date_prise")
    private LocalDateTime datePrise;

    @ManyToOne
    @JoinColumn(name = "visite_terrain_id")
    private VisiteTerrain visiteTerrain;
}