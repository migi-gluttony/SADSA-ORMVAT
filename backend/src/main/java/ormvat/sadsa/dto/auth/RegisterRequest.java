package ormvat.sadsa.dto.auth;

import lombok.Data;
import ormvat.sadsa.model.Utilisateur;
import ormvat.sadsa.model.Utilisateur.UserRole;

@Data
public class RegisterRequest {
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String motDePasse;
    private UserRole role;
    private Long cdaId; // Optional: CDA assignment for users
}