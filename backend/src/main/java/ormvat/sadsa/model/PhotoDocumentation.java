package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "photo_documentation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoDocumentation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_photo")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_visite")
    private VisiteTerrain visite;

    @Column(name = "chemin_fichier")
    private String cheminFichier;

    @Column
    private String description;

    @Column(name = "date_prise")
    @Temporal(TemporalType.TIMESTAMP)
    private Date datePrise;

    @Column(name = "coordonnees_gps")
    private String coordonneesGPS;
}
