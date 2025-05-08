package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ormvat.sadsa.model.VisiteTerrain;

import java.util.Date;
import java.util.List;

public interface VisiteTerrainRepository extends JpaRepository<VisiteTerrain, Long> {
    List<VisiteTerrain> findByDossierId(Long dossierId);
    List<VisiteTerrain> findByUtilisateurCommissionId(Long utilisateurId);
    List<VisiteTerrain> findByDateVisiteBetween(Date debut, Date fin);
}
