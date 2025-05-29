package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "document_requis")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRequis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_document", nullable = false)
    private String nomDocument;

    @Column
    private String description;

    @Column
    private Boolean obligatoire = true;

    @Column(name = "location_formulaire")
    private String locationFormulaire;

    @ManyToOne
    @JoinColumn(name = "sous_rubrique_id")
    private SousRubrique sousRubrique;
}
