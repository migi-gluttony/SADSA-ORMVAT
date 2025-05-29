package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "etape")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Etape {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_etape")
    private Long id;

    @Column(nullable = false)
    private String designation;

    @Column(name = "duree_jours")
    private Integer dureeJours;

    @Column
    private Integer ordre;

    @Column
    private String phase;
}