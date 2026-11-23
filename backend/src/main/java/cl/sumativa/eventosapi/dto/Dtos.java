package cl.sumativa.eventosapi.dto;

import cl.sumativa.eventosapi.domain.Banner;
import cl.sumativa.eventosapi.domain.Comentario;
import cl.sumativa.eventosapi.domain.DestinoCompartir;
import cl.sumativa.eventosapi.domain.Evento;
import cl.sumativa.eventosapi.domain.EventoCompartido;
import cl.sumativa.eventosapi.domain.EventoFoto;
import cl.sumativa.eventosapi.domain.EventoMedio;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTOs de salida. Se expone solo lo necesario: nunca se serializan entidades JPA
 * completas (evita fuga de datos y exposición de campos internos).
 */
public final class Dtos {

    private Dtos() { }

    /** Información resumida: es lo único que ve un visitante no autenticado. */
    public record EventoResumen(Long id, String nombre, String videojuego, String categoria,
                                String pais, String ciudad, LocalDateTime fechaHora, String modalidad,
                                String descripcionCorta, String imagenPortada, Integer inscritos) {
        public static EventoResumen de(Evento e) {
            return new EventoResumen(e.getId(), e.getNombre(), e.getVideojuego(), e.getCategoria(),
                    e.getPais(), e.getCiudad(), e.getFechaHora(), e.getModalidad().name(),
                    e.getDescripcionCorta(), e.getImagenPortada(), e.getInscritos());
        }
    }

    /** Detalle completo: solo para usuarios autenticados con JWT válido. */
    public record EventoDetalle(Long id, String nombre, String videojuego, String categoria,
                                String pais, String ciudad, LocalDateTime fechaHora, String modalidad,
                                String ubicacion, String enlaceAcceso, String organizador,
                                String descripcion, String requisitos, String inscripcion,
                                Integer cupos, Integer inscritos, String imagenPortada,
                                List<Foto> fotos) {
        public static EventoDetalle de(Evento e) {
            return new EventoDetalle(e.getId(), e.getNombre(), e.getVideojuego(), e.getCategoria(),
                    e.getPais(), e.getCiudad(), e.getFechaHora(), e.getModalidad().name(),
                    e.getUbicacion(), e.getEnlaceAcceso(), e.getOrganizador(), e.getDescripcion(),
                    e.getRequisitos(), e.getInscripcion(), e.getCupos(), e.getInscritos(),
                    e.getImagenPortada(), e.getFotos().stream().map(Foto::de).toList());
        }
    }

    public record Foto(String url, String textoAlternativo) {
        public static Foto de(EventoFoto f) {
            return new Foto(f.getUrl(), f.getTextoAlternativo());
        }
    }

    public record BannerDto(String titulo, String texto, String imagen, String enlace) {
        public static BannerDto de(Banner b) {
            return new BannerDto(b.getTitulo(), b.getTexto(), b.getImagen(), b.getEnlace());
        }
    }

    public record Filtros(List<String> categorias, List<String> videojuegos,
                          List<String> paises, List<String> ciudades, List<String> modalidades) { }

    public record Error(int status, String mensaje) { }

    // ----------------------------------------------------------------------------------
    // Semana 5: fotos/videos, comentarios con valoración y eventos compartidos
    // ----------------------------------------------------------------------------------

    /** Entrada de un comentario. El autor NO viene en el cuerpo: se toma del JWT. */
    public record NuevoComentario(
            @NotNull(message = "La valoración es obligatoria")
            @Min(value = 1, message = "La valoración mínima es 1")
            @Max(value = 5, message = "La valoración máxima es 5") Integer valoracion,
            @NotBlank(message = "El comentario es obligatorio")
            @Size(max = 500, message = "El comentario admite máximo 500 caracteres") String texto) { }

    /** Entrada para compartir. El destino es un enum: cualquier otro valor se rechaza con 400. */
    public record NuevoCompartido(
            @NotNull(message = "El destino es obligatorio") DestinoCompartir destino,
            @Size(max = 280, message = "El mensaje admite máximo 280 caracteres") String mensaje) { }

    public record ComentarioDto(Long id, String autor, Integer valoracion, String texto,
                                LocalDateTime fecha, boolean propio) {
        public static ComentarioDto de(Comentario c, String usuarioActual) {
            String autor = c.getUsuario().getUsername();
            return new ComentarioDto(c.getId(), autor, c.getValoracion(), c.getTexto(), c.getFecha(),
                    autor.equals(usuarioActual));
        }
    }

    public record Comentarios(double promedio, int total, List<ComentarioDto> comentarios) { }

    /** Metadatos del medio (sin los bytes). El contenido se obtiene en /api/eventos/medios/{id}/contenido. */
    public record MedioDto(Long id, String tipo, String contentType, String descripcion, String autor,
                           LocalDateTime fecha) {
        public static MedioDto de(EventoMedio m) {
            return new MedioDto(m.getId(), m.getTipo().name(), m.getContentType(), m.getDescripcion(),
                    m.getUsuario().getUsername(), m.getFechaSubida());
        }
    }

    public record CompartidoDto(Long id, Long eventoId, String eventoNombre, String destino, String autor,
                                String mensaje, LocalDateTime fecha) {
        public static CompartidoDto de(EventoCompartido c) {
            return new CompartidoDto(c.getId(), c.getEvento().getId(), c.getEvento().getNombre(),
                    c.getDestino().name(), c.getUsuario().getUsername(), c.getMensaje(), c.getFecha());
        }
    }

    public record ResumenCompartidos(long total, Map<String, Long> porDestino) { }
}
