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
    @Column(name = "id_document_requis")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_sous_rubrique")
    private SousRubrique sousRubrique;

    @Column(name = "nom_document", nullable = false)
    private String nomDocument;

    @Column(name = "description")
    private String description;

    @Column(name = "obligatoire")
    private Boolean obligatoire = true;

    @Column(name = "config_formulaire_json", columnDefinition = "TEXT")
    private String configFormulaireJson;
}