package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "emplacement")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Emplacement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_emplacement")
    private Long id;

    @Column(nullable = false)
    private String designation;

    @Column(name = "periode_a")
    private Integer periodeApprobation;

    @Column(name = "periode_r")
    private Integer periodeRealisation;
}
