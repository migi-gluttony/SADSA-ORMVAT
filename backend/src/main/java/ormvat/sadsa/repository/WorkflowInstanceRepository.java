package ormvat.sadsa.repository;

import ormvat.sadsa.model.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {
    
    /**
     * Find workflow instances by dossier ID
     */
    List<WorkflowInstance> findByDossierId(Long dossierId);
    
    /**
     * Find workflow instances by etape ID (replaces etapeDesignation queries)
     */
    @Query("SELECT w FROM WorkflowInstance w WHERE w.etape.id = :etapeId")
    List<WorkflowInstance> findByEtapeId(@Param("etapeId") Long etapeId);
    
    /**
     * Find workflow instances by phase number (1-8)
     */
    @Query("SELECT w FROM WorkflowInstance w WHERE w.etape.id = :phaseNumber")
    List<WorkflowInstance> findByPhaseNumber(@Param("phaseNumber") Integer phaseNumber);
    
    /**
     * Find workflow instances by emplacement type
     */
    List<WorkflowInstance> findByEmplacementActuel(WorkflowInstance.EmplacementType emplacementType);
    
    /**
     * Find overdue workflow instances
     */
    List<WorkflowInstance> findByEnRetardTrue();
    
    /**
     * Find workflow instances in approval phase (phases 1-4)
     */
    @Query("SELECT w FROM WorkflowInstance w WHERE w.etape.id BETWEEN 1 AND 4")
    List<WorkflowInstance> findInApprovalPhase();
    
    /**
     * Find workflow instances in realization phase (phases 5-8)
     */
    @Query("SELECT w FROM WorkflowInstance w WHERE w.etape.id BETWEEN 5 AND 8")
    List<WorkflowInstance> findInRealizationPhase();
    
    /**
     * Find workflow instances at Antenne phases (1, 5)
     */
    @Query("SELECT w FROM WorkflowInstance w WHERE w.etape.id IN (1, 5)")
    List<WorkflowInstance> findAtAntennePhases();
    
    /**
     * Find workflow instances at GUC phases (2, 4, 6, 8)
     */
    @Query("SELECT w FROM WorkflowInstance w WHERE w.etape.id IN (2, 4, 6, 8)")
    List<WorkflowInstance> findAtGUCPhases();
    
    /**
     * Find workflow instances at Commission phase (3)
     */
    @Query("SELECT w FROM WorkflowInstance w WHERE w.etape.id = 3")
    List<WorkflowInstance> findAtCommissionPhase();
    
    /**
     * Find workflow instances at Service Technique phase (7)
     */
    @Query("SELECT w FROM WorkflowInstance w WHERE w.etape.id = 7")
    List<WorkflowInstance> findAtServiceTechniquePhase();
    
    /**
     * Count workflow instances by phase
     */
    @Query("SELECT w.etape.id, COUNT(w) FROM WorkflowInstance w GROUP BY w.etape.id")
    List<Object[]> countByPhase();
    
    /**
     * Find workflow instances with remaining days less than specified threshold
     */
    @Query("SELECT w FROM WorkflowInstance w WHERE w.joursRestants <= :threshold")
    List<WorkflowInstance> findWithRemainingDaysLessThan(@Param("threshold") Integer threshold);
    
    /**
     * Find workflow instances for a specific dossier at a specific phase
     */
    @Query("SELECT w FROM WorkflowInstance w WHERE w.dossier.id = :dossierId AND w.etape.id = :etapeId")
    List<WorkflowInstance> findByDossierIdAndEtapeId(@Param("dossierId") Long dossierId, @Param("etapeId") Long etapeId);
}