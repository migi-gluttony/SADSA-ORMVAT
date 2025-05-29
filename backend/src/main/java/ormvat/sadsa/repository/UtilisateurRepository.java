package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ormvat.sadsa.model.Utilisateur;
import ormvat.sadsa.model.Utilisateur.UserRole;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    List<Utilisateur> findByRole(UserRole role);
    List<Utilisateur> findByCdaId(Long cdaId);
    boolean existsByEmail(String email);
}
