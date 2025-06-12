package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ormvat.sadsa.model.VisiteImplementation;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VisiteImplementationRepository extends JpaRepository<VisiteImplementation, Long> {

    List<VisiteImplementation> findByUtilisateurServiceTechniqueId(Long utilisateurId);

    List<VisiteImplementation> findByDossierId(Long dossierId);

    @Query("SELECT v FROM VisiteImplementation v WHERE v.dossier.id = :dossierId AND v.dateConstat IS NULL")
    List<VisiteImplementation> findActiveVisitesByDossierId(@Param("dossierId") Long dossierId);

    @Query("SELECT v FROM VisiteImplementation v WHERE v.utilisateurServiceTechnique.id = :userId AND v.dateVisite = :date")
    List<VisiteImplementation> findByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT v FROM VisiteImplementation v WHERE v.utilisateurServiceTechnique.id = :userId AND v.dateVisite BETWEEN :startDate AND :endDate")
    List<VisiteImplementation> findByUserAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT v FROM VisiteImplementation v WHERE v.utilisateurServiceTechnique.id = :userId AND v.dateConstat IS NULL AND v.dateVisite < :today")
    List<VisiteImplementation> findOverdueVisitesByUser(@Param("userId") Long userId, @Param("today") LocalDate today);

    @Query("SELECT COUNT(v) FROM VisiteImplementation v WHERE v.utilisateurServiceTechnique.id = :userId AND v.dateConstat IS NULL")
    Long countPendingVisitesByUser(@Param("userId") Long userId);

    @Query("SELECT v FROM VisiteImplementation v WHERE v.utilisateurServiceTechnique.id = :userId ORDER BY v.dateVisite DESC")
    List<VisiteImplementation> findByUserOrderByDateDesc(@Param("userId") Long userId);
}