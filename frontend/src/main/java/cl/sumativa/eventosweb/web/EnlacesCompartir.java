package cl.sumativa.eventosweb.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Construye los enlaces para compartir en redes sociales.
 *  - La URL pública del sitio viene de la configuración (app.url-publica), NO del header Host
 *    de la petición, que un atacante puede manipular (Host header injection).
 *  - Solo hay plantillas fijas por red social: el usuario no puede inyectar un destino arbitrario
 *    (sin redirección abierta).
 *  - Los valores se codifican con URLEncoder.
 */
@Component
public class EnlacesCompartir {

    private static final String TEXTO = "Mira este evento en Arena Estratega";

    private final String urlPublica;

    public EnlacesCompartir(@Value("${app.url-publica}") String urlPublica) {
        String url = urlPublica == null ? "" : urlPublica.trim();
        if (!url.matches("^https?://[A-Za-z0-9.\\-]+(:\\d+)?(/[A-Za-z0-9._\\-/]*)?$")) {
            throw new IllegalStateException("app.url-publica no es una URL válida");
        }
        this.urlPublica = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public String urlEvento(long eventoId) {
        return urlPublica + "/evento/" + eventoId;
    }

    /** Devuelve el enlace de la red social o null si el destino no es una red social. */
    public String enlace(String destino, long eventoId) {
        String url = codificar(urlEvento(eventoId));
        return switch (destino) {
            case "FACEBOOK" -> "https://www.facebook.com/sharer/sharer.php?u=" + url;
            case "X" -> "https://twitter.com/intent/tweet?text=" + codificar(TEXTO) + "&url=" + url;
            case "WHATSAPP" -> "https://api.whatsapp.com/send?text=" + codificar(TEXTO + ": " + urlEvento(eventoId));
            default -> null;
        };
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
