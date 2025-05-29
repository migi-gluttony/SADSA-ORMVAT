package ormvat.sadsa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ormvat.sadsa.model.Dossier;
import ormvat.sadsa.model.Etape;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface DossierRepository extends JpaRepository<Dossier, Long> {
    List<Dossier> findByAgriculteurId(Long agriculteurId);
    List<Dossier> findByCdaId(Long cdaId);
    Optional<Dossier> findByReference(String reference);
    Optional<Dossier> findBySaba(String saba);
}
