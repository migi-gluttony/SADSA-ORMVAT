package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import ormvat.sadsa.model.PhotoVisite;


@Repository
public interface PhotoVisiteRepository extends JpaRepository<PhotoVisite, Long> {
    List<PhotoVisite> findByVisiteTerrainId(Long visiteId);
}
