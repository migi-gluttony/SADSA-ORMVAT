package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ormvat.sadsa.model.PhotoDocumentation;

import java.util.List;

public interface PhotoDocumentationRepository extends JpaRepository<PhotoDocumentation, Long> {
    List<PhotoDocumentation> findByVisiteId(Long visiteId);
}
