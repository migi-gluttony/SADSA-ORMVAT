package ormvat.sadsa.repository;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

import ormvat.sadsa.model.Utilisateur;

public interface UtilisateurRepository extends JpaRepository<Utilisateur,Long> {
    Optional<Utilisateur> findByEmail(String email);
    
    Optional<Utilisateur> findByEmailAndDateNaissanceAndCni(String email, Date dateNaissance, String cni);
    Optional<Utilisateur> findByEmailAndDateNaissanceAndCne(String email, Date dateNaissance, String cne);
    boolean existsByCni(String cni);  
    boolean existsByCne(String cne);
}
