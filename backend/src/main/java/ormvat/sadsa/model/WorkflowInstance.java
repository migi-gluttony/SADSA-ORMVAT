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
        COMMISSION_AHA_AF, 
        AWAITING_FARMER_ACTION
    }
    
    // ============================================================================
    // HELPER METHODS - Delegate to etape for all information
    // ============================================================================
    
    /**
     * Get current phase number (1-8)
     */
    public Integer getCurrentPhase() {
        return etape != null ? etape.getId().intValue() : null;
    }
    
    /**
     * Get current phase display name
     */
    public String getCurrentPhaseDisplayName() {
        if (etape == null) return "Phase inconnue";
        
        switch (etape.getId().intValue()) {
            case 1: return "Phase 1: Création & Documents (Antenne)";
            case 2: return "Phase 2: Validation GUC Initiale";
            case 3: return "Phase 3: Commission Terrain";
            case 4: return "Phase 4: Approbation Finale GUC";
            case 5: return "Phase 5: Réalisation - Antenne";
            case 6: return "Phase 6: Réalisation - GUC";
            case 7: return "Phase 7: Réalisation - Service Technique";
            case 8: return "Phase 8: Réalisation - GUC Final";
            default: return "Phase " + etape.getId() + " (inconnue)";
        }
    }
    
    /**
     * Check if in approval phase (AP)
     */
    public Boolean isApprovalPhase() {
        return etape != null && etape.getId() >= 1 && etape.getId() <= 4;
    }
    
    /**
     * Check if in realization phase (RP)
     */
    public Boolean isRealizationPhase() {
        return etape != null && etape.getId() >= 5 && etape.getId() <= 8;
    }
}