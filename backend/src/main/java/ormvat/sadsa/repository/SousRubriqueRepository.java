package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ormvat.sadsa.model.SousRubrique;

import java.util.List;

public interface SousRubriqueRepository extends JpaRepository<SousRubrique, Long> {
    List<SousRubrique> findByRubriqueId(Long rubriqueId);
}
