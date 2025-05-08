package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ormvat.sadsa.model.JoursFerie;

import java.util.Date;
import java.util.List;

public interface JoursFerieRepository extends JpaRepository<JoursFerie, Long> {
    List<JoursFerie> findByDateBetween(Date debut, Date fin);
}
