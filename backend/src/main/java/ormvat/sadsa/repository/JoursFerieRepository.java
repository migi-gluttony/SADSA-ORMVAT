package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ormvat.sadsa.model.JoursFerie;

import java.time.LocalDate;
import java.util.List;

public interface JoursFerieRepository extends JpaRepository<JoursFerie, Long> {
    List<JoursFerie> findByDateBetween(LocalDate debut, LocalDate fin);
    List<JoursFerie> findByDate(LocalDate date);
    List<JoursFerie> findByFixeTrue();
}