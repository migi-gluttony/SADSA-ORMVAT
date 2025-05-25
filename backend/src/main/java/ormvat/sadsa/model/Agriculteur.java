package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "agriculteur")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Agriculteur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column
    private String telephone;

    @Column(nullable = false, unique = true)
    private String cin;

    @ManyToOne
    @JoinColumn(name = "commune_rurale_id")
    private CommuneRurale communeRurale;
    
    @ManyToOne
    @JoinColumn(name = "douar_id")
    private Douar douar;

    @Column(name = "date_notification")
    @Temporal(TemporalType.DATE)
    private Date dateNotification;
}
