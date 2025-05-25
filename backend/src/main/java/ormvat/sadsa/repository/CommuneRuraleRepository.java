package ormvat.sadsa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ormvat.sadsa.model.CommuneRurale;

public interface CommuneRuraleRepository extends JpaRepository<CommuneRurale, Long> {
    List<CommuneRurale> findByCercleId(Long cercleId);
}
