package ormvat.sadsa.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "note")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String objet;

    @Column(columnDefinition = "TEXT")
    private String contenu;

    @Column(columnDefinition = "TEXT")
    private String reponse;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;

    @Column(name = "type_note")
    private String typeNote;

    @Column
    private String priorite;

    @ManyToOne
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;

    @ManyToOne
    @JoinColumn(name = "utilisateur_expediteur_id")
    private Utilisateur utilisateurExpediteur;

    @ManyToOne
    @JoinColumn(name = "utilisateur_destinataire_id")
    private Utilisateur utilisateurDestinataire;
}
