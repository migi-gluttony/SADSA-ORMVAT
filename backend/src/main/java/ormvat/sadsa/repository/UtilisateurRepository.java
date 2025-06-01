package ormvat.sadsa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ormvat.sadsa.model.Utilisateur;
import ormvat.sadsa.model.Utilisateur.UserRole;
import ormvat.sadsa.model.Utilisateur.EquipeCommission;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    List<Utilisateur> findByRole(UserRole role);
    List<Utilisateur> findByAntenneId(Long antenneId);
    boolean existsByEmail(String email);
    
    // New methods for team-based commission routing
    List<Utilisateur> findByRoleAndEquipeCommission(UserRole role, EquipeCommission equipeCommission);
    List<Utilisateur> findByRoleAndEquipeCommissionAndActifTrue(UserRole role, EquipeCommission equipeCommission);
    List<Utilisateur> findByEquipeCommission(EquipeCommission equipeCommission);
    
    // Count commission agents by team
    long countByRoleAndEquipeCommissionAndActifTrue(UserRole role, EquipeCommission equipeCommission);
}