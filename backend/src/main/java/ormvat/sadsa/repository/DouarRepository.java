package ormvat.sadsa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ormvat.sadsa.model.Douar;

public interface DouarRepository extends JpaRepository<Douar, Long> {
    List<Douar> findByCommuneRuraleId(Long communeRuraleId);
}
