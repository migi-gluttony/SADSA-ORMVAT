package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ormvat.sadsa.model.Rubrique;

public interface RubriqueRepository extends JpaRepository<Rubrique, Long> {
    Rubrique findByDesignation(String designation);
}
