package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "douar")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Douar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String designation;
    
    @ManyToOne
    @JoinColumn(name = "commune_rurale_id")
    private CommuneRurale communeRurale;
}