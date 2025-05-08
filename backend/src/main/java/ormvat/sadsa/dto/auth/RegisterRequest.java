package ormvat.sadsa.dto.auth;

import lombok.Data;
import ormvat.sadsa.model.Utilisateur;

@Data
public class RegisterRequest {
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String motDePasse;
    private Utilisateur.Role role;
    private Long cdaId; // Optional: CDA assignment for users
}