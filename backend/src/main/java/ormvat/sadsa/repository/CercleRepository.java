package ormvat.sadsa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ormvat.sadsa.model.Cercle;

public interface CercleRepository extends JpaRepository<Cercle, Long> {
    List<Cercle> findByProvinceId(Long provinceId);
}