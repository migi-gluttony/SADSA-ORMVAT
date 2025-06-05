package ormvat.sadsa.repository;

import ormvat.sadsa.model.HistoriqueWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface HistoriqueWorkflowRepository extends JpaRepository<HistoriqueWorkflow, Long> {
    
    /**
     * Find workflow history by dossier ID
     */
    List<HistoriqueWorkflow> findByDossierId(Long dossierId);
    
    /**
     * Find workflow history by etape ID (replaces etapeDesignation queries)
     */
    @Query("SELECT h FROM HistoriqueWorkflow h WHERE h.etape.id = :etapeId")
    List<HistoriqueWorkflow> findByEtapeId(@Param("etapeId") Long etapeId);
    
    /**
     * Find workflow history by phase number (1-8)
     */
    @Query("SELECT h FROM HistoriqueWorkflow h WHERE h.etape.id = :phaseNumber")
    List<HistoriqueWorkflow> findByPhaseNumber(@Param("phaseNumber") Integer phaseNumber);
    
    /**
     * Find workflow history by user ID
     */
    List<HistoriqueWorkflow> findByUtilisateurId(Long utilisateurId);
    
    /**
     * Find workflow history by date range
     */
    List<HistoriqueWorkflow> findByDateEntreeBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Find workflow history in approval phase (phases 1-4)
     */
    @Query("SELECT h FROM HistoriqueWorkflow h WHERE h.etape.id BETWEEN 1 AND 4")
    List<HistoriqueWorkflow> findInApprovalPhase();
    
    /**
     * Find workflow history in realization phase (phases 5-8)
     */
    @Query("SELECT h FROM HistoriqueWorkflow h WHERE h.etape.id BETWEEN 5 AND 8")
    List<HistoriqueWorkflow> findInRealizationPhase();
    
    /**
     * Find workflow history for dossier and phase
     */
    @Query("SELECT h FROM HistoriqueWorkflow h WHERE h.dossier.id = :dossierId AND h.etape.id = :etapeId")
    List<HistoriqueWorkflow> findByDossierIdAndEtapeId(@Param("dossierId") Long dossierId, @Param("etapeId") Long etapeId);
    
    /**
     * Find overdue entries
     */
    List<HistoriqueWorkflow> findByEnRetardTrue();
    
    /**
     * Find open (unclosed) workflow history entries
     */
    @Query("SELECT h FROM HistoriqueWorkflow h WHERE h.dateSortie IS NULL")
    List<HistoriqueWorkflow> findOpenEntries();
    
    /**
     * Find open entries for specific dossier
     */
    @Query("SELECT h FROM HistoriqueWorkflow h WHERE h.dossier.id = :dossierId AND h.dateSortie IS NULL")
    List<HistoriqueWorkflow> findOpenEntriesByDossierId(@Param("dossierId") Long dossierId);
    
    /**
     * Find completed entries (with dateSortie)
     */
    @Query("SELECT h FROM HistoriqueWorkflow h WHERE h.dateSortie IS NOT NULL")
    List<HistoriqueWorkflow> findCompletedEntries();
    
    /**
     * Count entries by phase
     */
    @Query("SELECT h.etape.id, COUNT(h) FROM HistoriqueWorkflow h GROUP BY h.etape.id")
    List<Object[]> countByPhase();
    
    /**
     * Calculate average duration by phase
     */
    @Query("SELECT h.etape.id, AVG(h.dureeJours) FROM HistoriqueWorkflow h WHERE h.dureeJours IS NOT NULL GROUP BY h.etape.id")
    List<Object[]> averageDurationByPhase();
    
    /**
     * Find entries that exceeded duration
     */
    @Query("SELECT h FROM HistoriqueWorkflow h WHERE h.dureeJours > h.etape.dureeJours")
    List<HistoriqueWorkflow> findExceededDurationEntries();
    
    /**
     * Find most recent entry for dossier
     */
    @Query("SELECT h FROM HistoriqueWorkflow h WHERE h.dossier.id = :dossierId ORDER BY h.dateEntree DESC")
    List<HistoriqueWorkflow> findMostRecentByDossierId(@Param("dossierId") Long dossierId);
}