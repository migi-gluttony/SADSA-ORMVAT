package ormvat.sadsa.model;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "workflow_instance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_workflow_instance")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_dossier")
    private Dossier dossier;

@Column(name = "etape_designation")
    private String etapeDesignation;

    @Enumerated(EnumType.STRING)
    @Column(name = "emplacement_actuel")
    private EmplacementType emplacementActuel;

    @Column(name = "date_entree")
    private LocalDateTime dateEntree;

    @Column(name = "date_limite")
    private LocalDateTime dateLimite;

    @Column(name = "en_retard")
    private Boolean enRetard = false;

    @Column(name = "jours_restants")
    private Integer joursRestants;

    @ManyToOne
    @JoinColumn(name = "id_etape")
    private Etape etape;

    public enum EmplacementType {
        ANTENNE,
        GUC,
        SERVICE_TECHNIQUE,
        COMMISSION_AHA_AF
    }
}