package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "rubrique")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rubrique {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rubrique")
    private Long id;

    @Column(nullable = false)
    private String designation;

    @OneToMany(mappedBy = "rubrique")
    private List<SousRubrique> sousRubriques;
}
