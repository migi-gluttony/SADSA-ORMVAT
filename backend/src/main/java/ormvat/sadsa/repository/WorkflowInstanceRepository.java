package ormvat.sadsa.repository;
import ormvat.sadsa.model.WorkflowInstance;
import ormvat.sadsa.model.Etape;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {
    List<WorkflowInstance> findByDossierId(Long dossierId);
        List<WorkflowInstance> findByEtapeDesignation(String etapeDesignation);

    List<WorkflowInstance> findByEmplacementActuel(WorkflowInstance.EmplacementType emplacementType);
    List<WorkflowInstance> findByEnRetardTrue();
}