package ormvat.sadsa.dto.auth;

import java.util.Date;

import lombok.Data;
import ormvat.sadsa.model.Utilisateur;

@Data
public class RegisterRequest {
    private String nom;
    private String prenom;
    private String email;
    private String cni;
    private String cne;
    private Date dateNaissance;
    private String motDePasse;
    private Utilisateur.Role role;
}