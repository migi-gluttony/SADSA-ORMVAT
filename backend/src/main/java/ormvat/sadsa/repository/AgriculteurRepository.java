package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ormvat.sadsa.model.Agriculteur;

import java.util.Optional;

public interface AgriculteurRepository extends JpaRepository<Agriculteur, Long> {
    Optional<Agriculteur> findByCin(String cin);
    boolean existsByCin(String cin);
}
