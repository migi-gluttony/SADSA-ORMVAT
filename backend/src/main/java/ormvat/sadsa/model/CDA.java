package ormvat.sadsa.model;

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

    @ManyToOne
    @JoinColumn(name = "id_antenne")
    private Antenne antenne;
}
