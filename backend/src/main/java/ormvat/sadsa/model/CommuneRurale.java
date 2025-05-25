package ormvat.sadsa.model;

import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "commune_rurale")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommuneRurale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String designation;
    
    @ManyToOne
    @JoinColumn(name = "cercle_id")
    private Cercle cercle;
    
    @OneToMany(mappedBy = "communeRurale")
    private List<Douar> douars;
}