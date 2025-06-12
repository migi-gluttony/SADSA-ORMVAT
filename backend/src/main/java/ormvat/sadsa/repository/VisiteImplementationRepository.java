package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ormvat.sadsa.model.VisiteImplementation;

import java.util.List;
import java.util.Optional;

@Repository
public interface VisiteImplementationRepository extends JpaRepository<VisiteImplementation, Long> {
    
    /**
     * Find all implementation visits for a specific dossier
     */
    List<VisiteImplementation> findByDossierId(Long dossierId);
    
    /**
     * Find all implementation visits for a specific dossier ordered by creation date descending
     */
    List<VisiteImplementation> findByDossierIdOrderByDateCreationDesc(Long dossierId);
    
    /**
     * Find the latest implementation visit for a specific dossier
     */
    @Query("SELECT vi FROM VisiteImplementation vi WHERE vi.dossier.id = :dossierId ORDER BY vi.dateCreation DESC")
    Optional<VisiteImplementation> findLatestByDossierId(@Param("dossierId") Long dossierId);
    
    /**
     * Find all visits by Service Technique user
     */
    List<VisiteImplementation> findByUtilisateurServiceTechniqueId(Long userId);
    
    /**
     * Find visits by status
     */
    List<VisiteImplementation> findByStatutVisite(VisiteImplementation.StatutVisite statut);
    
    /**
     * Find completed visits for a dossier
     */
    @Query("SELECT vi FROM VisiteImplementation vi WHERE vi.dossier.id = :dossierId AND vi.statutVisite = :statut")
    List<VisiteImplementation> findByDossierIdAndStatutVisite(@Param("dossierId") Long dossierId, 
                                                             @Param("statut") VisiteImplementation.StatutVisite statut);
    
    /**
     * Find the latest completed visit for a dossier
     */
    @Query("SELECT vi FROM VisiteImplementation vi WHERE vi.dossier.id = :dossierId AND vi.statutVisite = 'TERMINEE' ORDER BY vi.dateModification DESC")
    Optional<VisiteImplementation> findLatestCompletedByDossierId(@Param("dossierId") Long dossierId);
}