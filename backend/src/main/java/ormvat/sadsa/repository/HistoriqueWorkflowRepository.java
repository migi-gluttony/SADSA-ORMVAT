package ormvat.sadsa.repository;

import ormvat.sadsa.model.HistoriqueWorkflow;
import ormvat.sadsa.model.Etape;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;




@Repository
public interface HistoriqueWorkflowRepository extends JpaRepository<HistoriqueWorkflow, Long> {
    List<HistoriqueWorkflow> findByDossierId(Long dossierId);
    List<HistoriqueWorkflow> findByEtapeDesignation(String etapeDesignation);
    List<HistoriqueWorkflow> findByUtilisateurId(Long utilisateurId);
}