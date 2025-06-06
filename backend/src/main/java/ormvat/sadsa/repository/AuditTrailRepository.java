package ormvat.sadsa.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import ormvat.sadsa.model.AuditTrail;


@Repository  
public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long> {
    List<AuditTrail> findByEntityIdAndEntityTypeOrderByTimestampDesc(Long entityId, String entityType);
}