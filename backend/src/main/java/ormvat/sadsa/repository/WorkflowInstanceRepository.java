package ormvat.sadsa.repository;

import ormvat.sadsa.model.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {
    Optional<WorkflowInstance> findByIdDossierAndDateSortieIsNull(Long idDossier);
    List<WorkflowInstance> findByIdDossierOrderByDateEntreeDesc(Long idDossier);
}
