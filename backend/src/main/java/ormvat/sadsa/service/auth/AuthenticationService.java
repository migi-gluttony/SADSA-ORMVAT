package ormvat.sadsa.service.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ormvat.sadsa.dto.auth.*;
import ormvat.sadsa.model.CDA;
import ormvat.sadsa.model.Utilisateur;
import ormvat.sadsa.repository.CDARepository;
import ormvat.sadsa.repository.UtilisateurRepository;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UtilisateurRepository utilisateurRepository;
    private final CDARepository cdaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user
     */
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }

        // Build user entity
        Utilisateur.UtilisateurBuilder userBuilder = Utilisateur.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .telephone(request.getTelephone())
                .motDePasse(passwordEncoder.encode(request.getMotDePasse()))
                .role(request.getRole())
                .statut("actif");

        // Assign CDA if provided
        if (request.getCdaId() != null) {
            CDA cda = cdaRepository.findById(request.getCdaId())
                    .orElseThrow(() -> new RuntimeException("CDA non trouvé"));
            userBuilder.cda(cda);
        }

        Utilisateur utilisateur = userBuilder.build();
        utilisateurRepository.save(utilisateur);

        // Generate JWT token
        String jwtToken = jwtService.generateToken(utilisateur);
        
        return AuthResponse.builder()
                .token(jwtToken)
                .build();
    }

    /**
     * Authenticate a user and generate a JWT token
     */
    public AuthResponse login(LoginRequest request) {
        // Authenticate user
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getMotDePasse()));
            
        // Find user
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
        
        // Generate JWT token
        String jwtToken = jwtService.generateToken(utilisateur);
        
        return AuthResponse.builder()
                .token(jwtToken)
                .build();
    }
    
    /**
     * Change user password
     */
    public void changePassword(String email, PasswordChangeRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
        
        // Verify current password
        if (!passwordEncoder.matches(request.getAncienMotDePasse(), utilisateur.getMotDePasse())) {
            throw new RuntimeException("Mot de passe actuel incorrect");
        }
        
        // Update password
        utilisateur.setMotDePasse(passwordEncoder.encode(request.getNouveauMotDePasse()));
        utilisateurRepository.save(utilisateur);
    }
}