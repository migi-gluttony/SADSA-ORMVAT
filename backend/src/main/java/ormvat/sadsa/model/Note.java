package ormvat.sadsa.model;

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
    @Column(name = "id_note")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_dossier")
    private Dossier dossier;

    @Column
    private String objet;

    @Column(length = 1000)
    private String note;

    @Column(length = 1000)
    private String reponse;
}
