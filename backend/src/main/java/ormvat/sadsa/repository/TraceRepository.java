package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ormvat.sadsa.model.Trace;

import java.util.Date;
import java.util.List;

public interface TraceRepository extends JpaRepository<Trace, Long> {
    List<Trace> findByDossierId(Long dossierId);
    List<Trace> findByUtilisateurId(Long utilisateurId);
    List<Trace> findByDateActionBetween(Date debut, Date fin);
}
