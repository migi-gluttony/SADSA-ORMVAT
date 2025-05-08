package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "jours_ferie")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoursFerie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_joursferie")
    private Long id;

    @Column(name = "jours_ferie")
    private String joursFerie;

    @Column
    @Temporal(TemporalType.DATE)
    private Date date;

    @Column
    private Integer nbr;
}

