package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "visite_implementation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisiteImplementation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_visite")
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

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    // Status of the implementation visit
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_visite")
    private StatutVisite statutVisite = StatutVisite.PROGRAMMEE;

    // Implementation-specific fields
    @Column(name = "duree_visite") // Duration in minutes
    private Integer dureeVisite;

    @Column(name = "pourcentage_avancement")
    private Integer pourcentageAvancement; // Progress percentage

    @Column(columnDefinition = "TEXT")
    private String conclusion; // Overall conclusion of the visit

    @Column(columnDefinition = "TEXT")
    private String problemes_detectes; // Issues detected during implementation

    @Column(columnDefinition = "TEXT")
    private String actions_correctives; // Corrective actions needed

    // Relationships
    @ManyToOne
    @JoinColumn(name = "id_dossier")
    private Dossier dossier;

    @ManyToOne
    @JoinColumn(name = "id_utilisateur_service_technique")
    private Utilisateur utilisateurServiceTechnique;

    // Enum for visit status
    public enum StatutVisite {
        PROGRAMMEE("Programmée"),
        EN_COURS("En cours"),
        TERMINEE("Terminée"),
        REPORTEE("Reportée"),
        ANNULEE("Annulée");

        private final String libelle;

        StatutVisite(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }
    }

    // Helper method to set modification date
    @PreUpdate
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        this.dateModification = LocalDateTime.now();
    }

    // Convenience methods
    public boolean isApprouve() {
        return Boolean.TRUE.equals(this.approuve);
    }

    public boolean isTerminee() {
        return this.statutVisite == StatutVisite.TERMINEE;
    }

    public boolean peutEtreModifiee() {
        return this.statutVisite == StatutVisite.PROGRAMMEE || this.statutVisite == StatutVisite.EN_COURS;
    }
}