package ormvat.sadsa.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "utilisateur")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipe_commission")
    private EquipeCommission equipeCommission;

    @Column
    private String statut;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "derniere_connexion")
    private LocalDateTime derniereConnexion;

    @Column
    private Boolean actif = true;

    @ManyToOne
    @JoinColumn(name = "antenne_id")
    private Antenne antenne;

    // Updated Role enum with neutral commission name
    public enum UserRole {
        ADMIN,
        AGENT_ANTENNE,
        AGENT_GUC,
        AGENT_COMMISSION_TERRAIN, // Renamed from COMMISSION_AHA_AF
        SERVICE_TECHNIQUE
    }

    // New enum for commission teams based on project types
    public enum EquipeCommission {
        FILIERES_VEGETALES("Équipe Filières Végétales", "Inspection terrain pour projets de production végétale"),
        FILIERES_ANIMALES("Équipe Filières Animales", "Inspection terrain pour projets de production animale"),
        AMENAGEMENT_HYDRO_AGRICOLE("Équipe Aménagement Hydro-Agricole", "Inspection terrain pour projets d'infrastructure hydro-agricole");

        private final String designation;
        private final String description;

        EquipeCommission(String designation, String description) {
            this.designation = designation;
            this.description = description;
        }

        public String getDesignation() {
            return designation;
        }

        public String getDescription() {
            return description;
        }

        // Get team based on rubrique ID
        public static EquipeCommission getTeamForRubrique(Long rubriqueId) {
            switch (rubriqueId.intValue()) {
                case 1: return FILIERES_VEGETALES;
                case 2: return FILIERES_ANIMALES;
                case 3: return AMENAGEMENT_HYDRO_AGRICOLE;
                default: return FILIERES_VEGETALES; // Default fallback
            }
        }

        // Get team display name for UI
        public String getDisplayName() {
            switch (this) {
                case FILIERES_VEGETALES: return "Filières Végétales";
                case FILIERES_ANIMALES: return "Filières Animales";
                case AMENAGEMENT_HYDRO_AGRICOLE: return "Aménagement Hydro-Agricole";
                default: return this.designation;
            }
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return motDePasse;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "actif".equalsIgnoreCase(statut);
    }

    // Helper method to check if user is commission agent
    public boolean isCommissionAgent() {
        return this.role == UserRole.AGENT_COMMISSION_TERRAIN;
    }

    // Helper method to get team display info
    public String getEquipeDisplayName() {
        return equipeCommission != null ? equipeCommission.getDisplayName() : null;
    }
}