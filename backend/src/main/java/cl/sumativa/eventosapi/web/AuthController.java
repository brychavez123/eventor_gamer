package cl.sumativa.eventosapi.web;

import cl.sumativa.eventosapi.domain.Usuario;
import cl.sumativa.eventosapi.dto.Dtos;
import cl.sumativa.eventosapi.repository.UsuarioRepository;
import cl.sumativa.eventosapi.security.JwtService;
import cl.sumativa.eventosapi.security.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * API de login (se mantiene el contrato de la semana 2: parámetros "user" y "encryptedPass",
 * respuesta = token JWT en texto).
 */
@RestController
public class AuthController {

    private static final Logger AUDITORIA = LoggerFactory.getLogger("AUDITORIA");

    /** Hash ficticio: se compara igual aunque el usuario no exista, para no revelar usuarios válidos por tiempo de respuesta. */
    private final String hashFicticio;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final LoginAttemptService intentos;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder encoder,
                          JwtService jwtService, LoginAttemptService intentos) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.intentos = intentos;
        this.hashFicticio = encoder.encode(java.util.UUID.randomUUID().toString());
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> login(@RequestParam("user") String user,
                                   @RequestParam("encryptedPass") String password,
                                   HttpServletRequest request) {
        String username = user == null ? "" : user.trim();
        String ip = request.getRemoteAddr();

        if (username.isEmpty() || username.length() > 50 || password == null || password.length() > 100) {
            return error(HttpStatus.BAD_REQUEST, "Credenciales inválidas");
        }
        if (intentos.estaBloqueado(username, ip)) {
            AUDITORIA.warn("LOGIN_BLOQUEADO usuario={} ip={}", limpiar(username), ip);
            return error(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos fallidos. Intente más tarde.");
        }

        Optional<Usuario> encontrado = usuarios.findByUsernameAndActivoTrue(username);
        String hash = encontrado.map(Usuario::getPasswordHash).orElse(hashFicticio);
        boolean ok = encoder.matches(password, hash) && encontrado.isPresent();

        if (!ok) {
            intentos.registrarFallo(username, ip);
            AUDITORIA.warn("LOGIN_FALLIDO usuario={} ip={}", limpiar(username), ip);
            // Mensaje genérico: no se indica si falló el usuario o la contraseña.
            return error(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
        }

        intentos.registrarExito(username, ip);
        Usuario u = encontrado.get();
        AUDITORIA.info("LOGIN_EXITOSO usuario={} ip={}", limpiar(username), ip);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header("Cache-Control", "no-store")
                .body(jwtService.generar(u.getUsername(), u.getRol()));
    }

    private static ResponseEntity<Dtos.Error> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(new Dtos.Error(status.value(), mensaje));
    }

    /** Evita inyección de líneas falsas en los logs (CWE-117). */
    private static String limpiar(String valor) {
        return valor.replaceAll("[\\r\\n\\t]", "_");
    }
}
