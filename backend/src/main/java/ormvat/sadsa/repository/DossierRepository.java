package ormvat.sadsa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ormvat.sadsa.model.Dossier;
import ormvat.sadsa.model.Etape;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, Long> {
    List<Dossier> findByAntenneId(Long antenneId);
    List<Dossier> findByStatus(Dossier.DossierStatus status);
    List<Dossier> findByStatusIn(List<Dossier.DossierStatus> statuses);
    List<Dossier> findByUtilisateurCreateurId(Long utilisateurId);
    List<Dossier> findByAgriculteurId(Long agriculteurId);
    Optional<Dossier> findByNumeroDossier(String numeroDossier);
    Optional<Dossier> findBySaba(String saba);
}
