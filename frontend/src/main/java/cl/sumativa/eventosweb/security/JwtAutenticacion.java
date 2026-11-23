package cl.sumativa.eventosweb.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Autenticación del usuario que guarda SU propio JWT dentro de su sesión.
 *
 * Corrección respecto de la guía: allí el token se guardaba en un TokenStore @Component
 * (singleton), es decir, UN token compartido por todos los usuarios. Si el usuario A iniciaba
 * sesión y luego el B, A pasaba a usar el token de B (A01 Control de acceso roto / A07).
 * Aquí cada sesión tiene su token y la contraseña no se conserva en memoria.
 */
public class JwtAutenticacion extends UsernamePasswordAuthenticationToken {

    private final String jwt;

    public JwtAutenticacion(String usuario, String jwt, Collection<? extends GrantedAuthority> authorities) {
        super(usuario, null, authorities);
        this.jwt = jwt;
    }

    public String getJwt() {
        return jwt;
    }
}
