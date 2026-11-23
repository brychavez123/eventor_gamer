package cl.sumativa.eventosweb.security;

import cl.sumativa.eventosweb.client.BackendClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Autentica contra la API de login del backend (mecanismo de la semana 2).
 *
 * Correcciones respecto de la guía:
 *  - Ya no se imprime la contraseña ni el token con System.out (A09: datos sensibles en logs).
 *  - El token se guarda por usuario (JwtAutenticacion), no en un singleton compartido (A01).
 */
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationProvider.class);

    private final BackendClient backend;

    public CustomAuthenticationProvider(BackendClient backend) {
        this.backend = backend;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String usuario = authentication.getName() == null ? "" : authentication.getName().trim();
        Object credenciales = authentication.getCredentials();
        if (usuario.isEmpty() || credenciales == null || credenciales.toString().isEmpty()) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        String jwt = backend.login(usuario, credenciales.toString());
        log.info("Inicio de sesión correcto para el usuario {}", usuario.replaceAll("[\\r\\n\\t]", "_"));
        return new JwtAutenticacion(usuario, jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
