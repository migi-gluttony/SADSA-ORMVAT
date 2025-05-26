package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "sous_rubrique")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SousRubrique {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sous_rubrique")
    private Long id;

    @Column(nullable = false)
    private String designation;

    @Column(length = 500)
    private String description;

    @ManyToOne
    @JoinColumn(name = "id_rubrique")
    private Rubrique rubrique;

    @OneToMany(mappedBy = "sousRubrique")
    private List<DocumentRequis> documentsRequis;
}