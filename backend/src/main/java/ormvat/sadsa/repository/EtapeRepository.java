package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ormvat.sadsa.model.Etape;

public interface EtapeRepository extends JpaRepository<Etape, Long> {
    Etape findByDesignation(String designation);
}
