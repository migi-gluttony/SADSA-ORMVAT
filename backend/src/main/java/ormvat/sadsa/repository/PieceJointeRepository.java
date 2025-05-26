package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ormvat.sadsa.model.PieceJointe;

import java.util.List;

public interface PieceJointeRepository extends JpaRepository<PieceJointe, Long> {
    List<PieceJointe> findByDossierId(Long dossierId);
    List<PieceJointe> findByDossierIdAndDocumentRequisId(Long dossierId, Long documentRequisId);
}