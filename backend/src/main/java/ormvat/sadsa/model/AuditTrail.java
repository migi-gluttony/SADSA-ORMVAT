package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "audit_trail")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditTrail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    @Column
    private String entite;

    @Column(name = "entite_id")
    private Long entiteId;

    @Column(name = "valeur_avant", columnDefinition = "TEXT")
    private String valeurAvant;

    @Column(name = "valeur_apres", columnDefinition = "TEXT")
    private String valeurApres;

    @Column(name = "date_action")
    private LocalDateTime dateAction;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(name = "adresse_ip")
    private String adresseIP;

    @Column(columnDefinition = "TEXT")
    private String description;
}
