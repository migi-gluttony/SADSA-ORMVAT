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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EtapeType type;

    @Column(nullable = false)
    private String designation;

    @Column(name = "duree_jours")
    private Integer dureeJours;

    @Column
    private Integer ordre;

    @Column
    private String phase;

    public enum EtapeType {
        AP_PHASE_ANTENNE,
        AP_PHASE_GUC,
        AP_PHASE_AHA_AF,
        AP_PHASE_GUC_FINAL,
        RP_PHASE_ANTENNE_1,
        RP_PHASE_ANTENNE_2,
        RP_PHASE_SERVICE_TECHNIQUE,
        RP_PHASE_GUC_FINAL
    }
}