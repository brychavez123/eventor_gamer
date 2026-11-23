package cl.sumativa.eventosweb.config;

import cl.sumativa.eventosweb.security.CustomAuthenticationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.CrossOriginEmbedderPolicyHeaderWriter;
import org.springframework.security.web.header.writers.CrossOriginOpenerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.CrossOriginResourcePolicyHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Rutas públicas: inicio (/), búsqueda (/buscar), login y recursos estáticos.
 * Rutas privadas: todo lo demás, en particular el detalle /evento/{id}.
 *
 * Correcciones respecto de la guía: se quitó @EnableWebSecurity(debug = true), que escribe
 * en el log las peticiones completas (A05/A09), y se agregaron headers de seguridad (A05).
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self'",
            "img-src 'self' data:",
            "font-src 'self'",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    @Value("${app.seguridad.endurecida:true}")
    private boolean endurecida;

    @Bean
    public AuthenticationManager authenticationManager(CustomAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/buscar", "/login", "/error", "/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", false)
                .failureHandler((req, res, ex) -> res.sendRedirect(req.getContextPath()
                        + (ex instanceof LockedException ? "/login?bloqueado" : "/login?error")))
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?salir")
                .invalidateHttpSession(true)
                .deleteCookies("EVENTOSSESSION")
                .permitAll())
            .sessionManagement(s -> s
                .sessionFixation(f -> f.changeSessionId())   // nuevo ID de sesión al autenticarse (A07)
                .maximumSessions(3));

        if (endurecida) {
            http.headers(h -> h
                .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                .frameOptions(f -> f.deny())
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicyHeader(p -> p.policy("camera=(), microphone=(), geolocation=(), payment=()"))
                .crossOriginOpenerPolicy(c -> c.policy(CrossOriginOpenerPolicyHeaderWriter.CrossOriginOpenerPolicy.SAME_ORIGIN))
                .crossOriginEmbedderPolicy(c -> c.policy(CrossOriginEmbedderPolicyHeaderWriter.CrossOriginEmbedderPolicy.REQUIRE_CORP))
                .crossOriginResourcePolicy(c -> c.policy(CrossOriginResourcePolicyHeaderWriter.CrossOriginResourcePolicy.SAME_ORIGIN)));
        } else {
            // Perfil "inseguro": estado inicial sin headers, solo para la evidencia "ANTES".
            http.headers(h -> h.disable());
        }
        return http.build();
    }
}
