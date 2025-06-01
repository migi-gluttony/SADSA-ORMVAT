package ormvat.sadsa.dto.auth;

import lombok.Data;
import ormvat.sadsa.model.Utilisateur;
import ormvat.sadsa.model.Utilisateur.UserRole;
import ormvat.sadsa.model.Utilisateur.EquipeCommission;

@Data
public class RegisterRequest {
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String motDePasse;
    private UserRole role;
    private Long antenneId;
    private EquipeCommission equipeCommission; // For commission agents only
}