package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ormvat.sadsa.model.AuditTrail;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long> {
    
    /**
     * Find audit history for a specific entity, ordered by timestamp descending
     */
    @Query("SELECT at FROM AuditTrail at WHERE at.entityId = :entityId AND at.entityType = :entityType ORDER BY at.timestamp DESC")
    List<AuditTrail> findByEntityIdAndEntityTypeOrderByTimestampDesc(@Param("entityId") Long entityId, 
                                                                      @Param("entityType") String entityType);
    
    /**
     * Find audit history by user
     */
    @Query("SELECT at FROM AuditTrail at WHERE at.userId = :userId ORDER BY at.timestamp DESC")
    List<AuditTrail> findByUserIdOrderByTimestampDesc(@Param("userId") Long userId);
    
    /**
     * Find audit history by action
     */
    @Query("SELECT at FROM AuditTrail at WHERE at.action = :action ORDER BY at.timestamp DESC")
    List<AuditTrail> findByActionOrderByTimestampDesc(@Param("action") String action);
    
    /**
     * Find audit history by entity type
     */
    @Query("SELECT at FROM AuditTrail at WHERE at.entityType = :entityType ORDER BY at.timestamp DESC")
    List<AuditTrail> findByEntityTypeOrderByTimestampDesc(@Param("entityType") String entityType);
    
    /**
     * Find audit history within date range
     */
    @Query("SELECT at FROM AuditTrail at WHERE at.timestamp BETWEEN :startDate AND :endDate ORDER BY at.timestamp DESC")
    List<AuditTrail> findByTimestampBetweenOrderByTimestampDesc(@Param("startDate") LocalDateTime startDate, 
                                                                @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find audit history for specific entity and user
     */
    @Query("SELECT at FROM AuditTrail at WHERE at.entityId = :entityId AND at.entityType = :entityType AND at.userId = :userId ORDER BY at.timestamp DESC")
    List<AuditTrail> findByEntityIdAndEntityTypeAndUserIdOrderByTimestampDesc(@Param("entityId") Long entityId, 
                                                                               @Param("entityType") String entityType,
                                                                               @Param("userId") Long userId);
    
    /**
     * Count audit entries for an entity
     */
    @Query("SELECT COUNT(at) FROM AuditTrail at WHERE at.entityId = :entityId AND at.entityType = :entityType")
    Long countByEntityIdAndEntityType(@Param("entityId") Long entityId, @Param("entityType") String entityType);
    
    /**
     * Find recent audit entries (last N days)
     */
    @Query("SELECT at FROM AuditTrail at WHERE at.timestamp >= :cutoffDate ORDER BY at.timestamp DESC")
    List<AuditTrail> findRecentEntries(@Param("cutoffDate") LocalDateTime cutoffDate);
}