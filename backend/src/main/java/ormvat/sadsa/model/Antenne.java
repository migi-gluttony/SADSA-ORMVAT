package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "antenne")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Antenne {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_antenne")
    private Long id;

    @Column(nullable = false)
    private String designation;

    @Column
    private String abreviation;

    @OneToMany(mappedBy = "antenne")
    private List<CDA> cdas;
}
