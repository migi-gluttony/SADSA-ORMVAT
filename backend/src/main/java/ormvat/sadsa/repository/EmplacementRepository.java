package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ormvat.sadsa.model.Emplacement;

public interface EmplacementRepository extends JpaRepository<Emplacement, Long> {
    Emplacement findByDesignation(String designation);
}

