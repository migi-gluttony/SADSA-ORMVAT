package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ormvat.sadsa.model.CDA;

import java.util.List;

public interface CDARepository extends JpaRepository<CDA, Long> {
    List<CDA> findByAntenneId(Long antenneId);
}
