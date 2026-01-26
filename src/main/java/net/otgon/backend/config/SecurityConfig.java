package net.otgon.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    // Read active profile to determine CORS settings
    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // CORS based on environment
        if ("prod".equals(activeProfile)) {
            configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        } else {
            configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:8081",      // Metro bundler
                "http://10.0.2.2:8081",       // Android emulator
                "http://172.20.10.2:8081",    // Physical device 
                "http://192.168.*.*:8081"     // Any local network device
            ));
        }
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        // PUBLIC ENDPOINTS (no authentication required)
                        .requestMatchers("/api/register", "/api/login").permitAll()
                        .requestMatchers("/api/wallet/redeem").permitAll()  // NFC payments
                        .requestMatchers("/api/health").permitAll()  // Health check
                        .requestMatchers("/actuator/health").permitAll()  // Spring actuator
                        
                        // AUTHENTICATED ENDPOINTS (JWT required)
                        .requestMatchers("/api/transactions").authenticated()
                        .requestMatchers("/api/wallet/**").authenticated()
                        .requestMatchers("/api/device/**").authenticated()
                        .requestMatchers("/api/userinfo").authenticated()
                        
                        // ALL OTHER REQUESTS
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> 
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}