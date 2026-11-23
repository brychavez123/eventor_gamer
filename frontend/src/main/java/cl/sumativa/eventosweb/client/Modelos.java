package cl.sumativa.eventosweb.client;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Modelos que devuelve la API del backend. */
public final class Modelos {

    private Modelos() { }

    public record EventoResumen(Long id, String nombre, String videojuego, String categoria,
                                String pais, String ciudad, LocalDateTime fechaHora, String modalidad,
                                String descripcionCorta, String imagenPortada, Integer inscritos) { }

    public record EventoDetalle(Long id, String nombre, String videojuego, String categoria,
                                String pais, String ciudad, LocalDateTime fechaHora, String modalidad,
                                String ubicacion, String enlaceAcceso, String organizador,
                                String descripcion, String requisitos, String inscripcion,
                                Integer cupos, Integer inscritos, String imagenPortada,
                                List<Foto> fotos) { }

    public record Foto(String url, String textoAlternativo) { }

    public record Banner(String titulo, String texto, String imagen, String enlace) { }

    // ---------- Semana 5: comentarios, fotos/videos y compartidos ----------

    public record Comentario(Long id, String autor, Integer valoracion, String texto, LocalDateTime fecha,
                             boolean propio) { }

    public record Comentarios(double promedio, int total, List<Comentario> comentarios) { }

    public record Medio(Long id, String tipo, String contentType, String descripcion, String autor,
                        LocalDateTime fecha) { }

    public record ContenidoMedio(byte[] datos, String contentType) { }

    public record Compartido(Long id, Long eventoId, String eventoNombre, String destino, String autor,
                             String mensaje, LocalDateTime fecha) { }

    public record ResumenCompartidos(long total, Map<String, Long> porDestino) { }

    public record Filtros(List<String> categorias, List<String> videojuegos,
                          List<String> paises, List<String> ciudades, List<String> modalidades) {
        public static Filtros vacios() {
            return new Filtros(List.of(), List.of(), List.of(), List.of(), List.of("PRESENCIAL", "ONLINE"));
        }
    }
}
