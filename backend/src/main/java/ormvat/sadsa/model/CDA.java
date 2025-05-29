package ormvat.sadsa.model;

import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cda")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CDA {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cda")
    private Long id;

    @Column
    private String description;

    @OneToMany(mappedBy = "cda")
    private List<Antenne> antennes;
}