package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ormvat.sadsa.model.Antenne;

public interface AntenneRepository extends JpaRepository<Antenne, Long> {
    Antenne findByDesignation(String designation);
}
