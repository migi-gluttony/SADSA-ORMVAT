package ormvat.sadsa.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Allow authentication endpoints without authentication
                        .requestMatchers("/api/auth/**").permitAll()

                        // Role-specific endpoints
                        .requestMatchers("/api/agent_antenne/**").hasAnyRole("AGENT_ANTENNE")
                        .requestMatchers("/api/agent_guc/**").hasRole("AGENT_GUC")
                        .requestMatchers("/api/agent_commission/**").hasRole("AGENT_COMMISSION_TERRAIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Common dossier endpoints - accessible by multiple roles
                        .requestMatchers("/api/dossiers/**")
                        .hasAnyRole("AGENT_ANTENNE", "AGENT_GUC", "AGENT_COMMISSION_TERRAIN", "ADMIN")

                        .requestMatchers("/api/documents/**")
                        .hasAnyRole("AGENT_ANTENNE", "AGENT_GUC", "AGENT_COMMISSION_TERRAIN", "ADMIN")
                        .requestMatchers("/api/fiche-approbation/**")
                        .hasAnyRole("AGENT_ANTENNE", "AGENT_GUC", "AGENT_COMMISSION_TERRAIN", "ADMIN")

                        // Any other request requires authentication
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}