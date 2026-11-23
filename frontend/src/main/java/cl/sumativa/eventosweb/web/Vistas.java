package cl.sumativa.eventosweb.web;

import org.springframework.stereotype.Component;

/**
 * Utilidades usadas desde las plantillas (${@vistas...}) para no confiar ciegamente en las
 * URLs que vienen de la base de datos.
 */
@Component("vistas")
public class Vistas {

    private static final String IMAGEN_POR_DEFECTO = "img/eventos/torneo-1.jpg";

    /** Solo se aceptan rutas relativas a /img (evita imágenes de dominios externos o "javascript:"). */
    public String imagen(String ruta) {
        if (ruta == null || !ruta.matches("^img/[a-z0-9/_-]+\\.(svg|png|jpg|jpeg|webp)$")) {
            return "/" + IMAGEN_POR_DEFECTO;
        }
        return "/" + ruta;
    }

    /** Solo se muestran enlaces externos https:// (bloquea javascript:, data:, etc.). */
    public String enlaceSeguro(String url) {
        if (url == null || !url.matches("^https://[A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=%-]+$")) {
            return null;
        }
        return url;
    }

    public String modalidad(String m) {
        return "ONLINE".equals(m) ? "Online" : "Presencial";
    }

    /** Valoración como estrellas (★★★★☆). Acepta promedios con decimales. */
    public String estrellas(Number valor) {
        int llenas = valor == null ? 0 : (int) Math.round(Math.max(0, Math.min(5, valor.doubleValue())));
        return "★".repeat(llenas) + "☆".repeat(5 - llenas);
    }

    public String nombreDestino(String destino) {
        if (destino == null) {
            return "";
        }
        return switch (destino) {
            case "SITIO" -> "Muro de la comunidad";
            case "FACEBOOK" -> "Facebook";
            case "X" -> "X (Twitter)";
            case "WHATSAPP" -> "WhatsApp";
            default -> "";
        };
    }
}
