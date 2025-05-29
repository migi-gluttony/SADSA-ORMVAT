package ormvat.sadsa.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import ormvat.sadsa.model.AuditTrail;
import java.time.LocalDateTime;


@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long> {
    List<AuditTrail> findByEntiteAndEntiteId(String entite, Long entiteId);
    List<AuditTrail> findByUtilisateurId(Long utilisateurId);
    List<AuditTrail> findByDateActionBetween(LocalDateTime start, LocalDateTime end);
}