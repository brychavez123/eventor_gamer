package cl.sumativa.eventosapi.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueo temporal tras intentos fallidos (A07 Fallas de identificación y autenticación):
 * 5 fallos para el mismo usuario o la misma IP bloquean el login durante 15 minutos,
 * lo que frena ataques de fuerza bruta y de "credential stuffing".
 */
@Service
public class LoginAttemptService {

    static final int MAX_INTENTOS = 5;
    static final Duration BLOQUEO = Duration.ofMinutes(15);

    private record Registro(int fallos, Instant ultimoFallo) { }

    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    public boolean estaBloqueado(String username, String ip) {
        return bloqueada("u:" + username) || bloqueada("ip:" + ip);
    }

    public void registrarFallo(String username, String ip) {
        sumar("u:" + username);
        sumar("ip:" + ip);
    }

    public void registrarExito(String username, String ip) {
        registros.remove("u:" + username);
        registros.remove("ip:" + ip);
    }

    private boolean bloqueada(String clave) {
        Registro r = registros.get(clave);
        if (r == null) {
            return false;
        }
        if (r.ultimoFallo().plus(BLOQUEO).isBefore(Instant.now())) {
            registros.remove(clave);
            return false;
        }
        return r.fallos() >= MAX_INTENTOS;
    }

    private void sumar(String clave) {
        registros.merge(clave, new Registro(1, Instant.now()),
                (viejo, nuevo) -> new Registro(viejo.fallos() + 1, Instant.now()));
    }
}
