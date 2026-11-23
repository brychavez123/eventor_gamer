package cl.sumativa.eventosapi.config;

import cl.sumativa.eventosapi.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.CrossOriginEmbedderPolicyHeaderWriter;
import org.springframework.security.web.header.writers.CrossOriginOpenerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.CrossOriginResourcePolicyHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Seguridad de la API:
 *  - Rutas públicas: POST /login y GET /api/publico/**
 *  - Rutas privadas: /api/eventos/** (exigen JWT válido)
 *  - Sin sesión (STATELESS): no hay cookies, por eso CSRF no aplica a esta API.
 *  - Headers de seguridad (A05) cuando app.seguridad.endurecida=true.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.seguridad.endurecida:true}")
    private boolean endurecida;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/publico/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/eventos/**").authenticated()
                .anyRequest().denyAll())
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .logout(l -> l.disable())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> escribirError(res, HttpStatus.UNAUTHORIZED,
                        "Se requiere un token JWT válido"))
                .accessDeniedHandler((req, res, ex) -> escribirError(res, HttpStatus.FORBIDDEN,
                        "Acceso denegado")))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        if (endurecida) {
            http.headers(h -> h
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                .frameOptions(f -> f.deny())
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=(), payment=()"))
                .crossOriginOpenerPolicy(c -> c.policy(CrossOriginOpenerPolicyHeaderWriter.CrossOriginOpenerPolicy.SAME_ORIGIN))
                .crossOriginEmbedderPolicy(c -> c.policy(CrossOriginEmbedderPolicyHeaderWriter.CrossOriginEmbedderPolicy.REQUIRE_CORP))
                .crossOriginResourcePolicy(c -> c.policy(CrossOriginResourcePolicyHeaderWriter.CrossOriginResourcePolicy.SAME_ORIGIN)));
        } else {
            // Perfil "inseguro": reproduce el estado inicial (semana 2) solo para obtener la evidencia "antes".
            http.headers(h -> h.disable());
        }
        return http.build();
    }

    /** Contraseñas almacenadas con BCrypt (factor 12), nunca en texto plano (A02). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    private static void escribirError(jakarta.servlet.http.HttpServletResponse res, HttpStatus status, String mensaje)
            throws java.io.IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"status\":" + status.value() + ",\"mensaje\":\"" + mensaje + "\"}");
    }
}
