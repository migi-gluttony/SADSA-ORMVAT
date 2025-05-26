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
    List<Dossier> findByEtapeActuelle(Etape etape);
    Optional<Dossier> findByReference(String reference);
    Optional<Dossier> findBySaba(String saba);
    
    // Recherche avancée des dossiers
    @Query("SELECT d FROM Dossier d WHERE " +
           "(:reference IS NULL OR d.reference LIKE %:reference%) AND " +
           "(:saba IS NULL OR d.saba LIKE %:saba%) AND " +
           "(:agriculteurId IS NULL OR d.agriculteur.id = :agriculteurId) AND " +
           "(:cdaId IS NULL OR d.cda.id = :cdaId) AND " +
           "(:etapeId IS NULL OR d.etapeActuelle.id = :etapeId) AND " +
           "(:sousRubriqueId IS NULL OR d.sousRubrique.id = :sousRubriqueId)")
    List<Dossier> searchDossiers(String reference, String saba, Long agriculteurId, 
                                 Long cdaId, Long etapeId, Long sousRubriqueId);
    
    // Dossiers en retard
    @Query("SELECT d FROM Dossier d JOIN Historique h ON d.id = h.dossier.id " +
           "WHERE h.dateReception <= :dateLimite AND h.dateEnvoi IS NULL")
    List<Dossier> findDossiersEnRetard(Date dateLimite);
    Page<Dossier> findAll(Specification<Dossier> spec, Pageable pageable);
}
