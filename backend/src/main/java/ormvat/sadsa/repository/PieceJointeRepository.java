package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ormvat.sadsa.model.PieceJointe;

import java.util.List;
import java.util.Optional;

@Repository
public interface PieceJointeRepository extends JpaRepository<PieceJointe, Long> {
    
    // Basic queries
    List<PieceJointe> findByDossierId(Long dossierId);
    List<PieceJointe> findByDossierIdAndDocumentRequisId(Long dossierId, Long documentRequisId);
    
    // Document type specific queries
    List<PieceJointe> findByDossierIdAndDocumentType(Long dossierId, PieceJointe.DocumentType documentType);
    List<PieceJointe> findByDossierIdAndDocumentTypeIn(Long dossierId, List<PieceJointe.DocumentType> documentTypes);
    
    // Status queries
    List<PieceJointe> findByDossierIdAndStatus(Long dossierId, PieceJointe.DocumentStatus status);
    long countByDossierIdAndStatus(Long dossierId, PieceJointe.DocumentStatus status);
    
    // Form data queries
    @Query("SELECT pj FROM PieceJointe pj WHERE pj.dossier.id = :dossierId AND pj.formData IS NOT NULL")
    List<PieceJointe> findByDossierIdWithFormData(@Param("dossierId") Long dossierId);
    
    // File queries
    @Query("SELECT pj FROM PieceJointe pj WHERE pj.dossier.id = :dossierId AND pj.cheminFichier IS NOT NULL")
    List<PieceJointe> findByDossierIdWithFiles(@Param("dossierId") Long dossierId);
    
    // Required documents completion check
    @Query("SELECT COUNT(pj) FROM PieceJointe pj WHERE pj.dossier.id = :dossierId AND pj.isRequired = true AND pj.status = 'COMPLETE'")
    long countCompletedRequiredDocuments(@Param("dossierId") Long dossierId);
    
    @Query("SELECT COUNT(dr) FROM DocumentRequis dr WHERE dr.sousRubrique.id = :sousRubriqueId AND dr.obligatoire = true")
    long countTotalRequiredDocuments(@Param("sousRubriqueId") Long sousRubriqueId);
    
    // Find existing form data or mixed document for specific document requis
    @Query("SELECT pj FROM PieceJointe pj WHERE pj.dossier.id = :dossierId AND pj.documentRequis.id = :documentRequisId AND pj.documentType IN ('FORM_DATA', 'MIXED') ORDER BY pj.lastEdited DESC")
    Optional<PieceJointe> findExistingFormData(@Param("dossierId") Long dossierId, @Param("documentRequisId") Long documentRequisId);
    
    // Find by document requis with specific types
    @Query("SELECT pj FROM PieceJointe pj WHERE pj.dossier.id = :dossierId AND pj.documentRequis.id = :documentRequisId AND pj.documentType = :documentType")
    List<PieceJointe> findByDossierAndDocumentRequisAndType(@Param("dossierId") Long dossierId, 
                                                            @Param("documentRequisId") Long documentRequisId, 
                                                            @Param("documentType") PieceJointe.DocumentType documentType);
    
    // Get completion statistics
    @Query("SELECT COUNT(DISTINCT pj.documentRequis.id) FROM PieceJointe pj WHERE pj.dossier.id = :dossierId AND pj.status = 'COMPLETE' AND pj.isRequired = true")
    long countDistinctCompletedRequiredDocuments(@Param("dossierId") Long dossierId);      // Terrain visit photo queries - photos are linked through dossier, not directly to visite
    @Query("SELECT pj FROM PieceJointe pj WHERE pj.dossier.id = (SELECT vt.dossier.id FROM VisiteTerrain vt WHERE vt.id = :visiteTerrainId) AND pj.documentType = 'TERRAIN_PHOTO'")
    List<PieceJointe> findByVisiteTerrainId(@Param("visiteTerrainId") Long visiteTerrainId);
    
    @Query("SELECT pj FROM PieceJointe pj WHERE pj.dossier.id = (SELECT vt.dossier.id FROM VisiteTerrain vt WHERE vt.id = :visiteTerrainId) AND pj.documentType = 'TERRAIN_PHOTO'")
    List<PieceJointe> findTerrainPhotosByVisiteId(@Param("visiteTerrainId") Long visiteTerrainId);
    
    @Query("SELECT pj FROM PieceJointe pj WHERE pj.dossier.id = :dossierId AND pj.documentType = 'TERRAIN_PHOTO'")
    List<PieceJointe> findAllTerrainPhotosByDossier(@Param("dossierId") Long dossierId);
    

    /**
     * Find all piece jointes for a specific dossier, ordered by upload date descending
     */
    @Query("SELECT pj FROM PieceJointe pj WHERE pj.dossier.id = :dossierId ORDER BY pj.dateUpload DESC")
    List<PieceJointe> findByDossierIdOrderByDateUploadDesc(@Param("dossierId") Long dossierId);
}