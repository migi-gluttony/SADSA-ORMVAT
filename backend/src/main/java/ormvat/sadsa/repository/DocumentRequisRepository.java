package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ormvat.sadsa.model.DocumentRequis;

import java.util.List;

public interface DocumentRequisRepository extends JpaRepository<DocumentRequis, Long> {
    List<DocumentRequis> findBySousRubriqueId(Long sousRubriqueId);
}