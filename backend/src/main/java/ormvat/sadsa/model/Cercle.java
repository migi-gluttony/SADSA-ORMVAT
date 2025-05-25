package ormvat.sadsa.model;

import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cercle")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cercle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String designation;
    
    @ManyToOne
    @JoinColumn(name = "province_id")
    private Province province;
    
    @OneToMany(mappedBy = "cercle")
    private List<CommuneRurale> communesRurales;
}