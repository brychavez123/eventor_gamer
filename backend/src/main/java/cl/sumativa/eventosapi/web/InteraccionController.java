package cl.sumativa.eventosapi.web;

import cl.sumativa.eventosapi.domain.Comentario;
import cl.sumativa.eventosapi.domain.DestinoCompartir;
import cl.sumativa.eventosapi.domain.Evento;
import cl.sumativa.eventosapi.domain.EventoCompartido;
import cl.sumativa.eventosapi.domain.EventoMedio;
import cl.sumativa.eventosapi.domain.MedioContenido;
import cl.sumativa.eventosapi.domain.Usuario;
import cl.sumativa.eventosapi.dto.Dtos;
import cl.sumativa.eventosapi.repository.ComentarioRepository;
import cl.sumativa.eventosapi.repository.EventoCompartidoRepository;
import cl.sumativa.eventosapi.repository.EventoMedioRepository;
import cl.sumativa.eventosapi.repository.EventoRepository;
import cl.sumativa.eventosapi.repository.MedioContenidoRepository;
import cl.sumativa.eventosapi.repository.UsuarioRepository;
import cl.sumativa.eventosapi.security.ValidadorArchivos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * APIs privadas de la semana 5 (todas bajo /api/eventos/**, por lo tanto exigen JWT válido):
 *  - Agregar fotos y videos a los eventos publicados.
 *  - Compartir eventos en el sitio (muro de la comunidad) y en redes sociales.
 *  - Comentar y valorar eventos.
 *
 * Buenas prácticas aplicadas:
 *  - El autor se obtiene del JWT, nunca de un parámetro enviado por el cliente (evita suplantación).
 *  - Solo el autor puede borrar su comentario (control de acceso a nivel de objeto, A01 / IDOR).
 *  - Validación de entrada con Bean Validation y validación del archivo por su firma real.
 *  - Límite de archivos por usuario y evento (evita llenar el almacenamiento, A04).
 *  - Registro de auditoría de las acciones (A09) sin datos sensibles.
 */
@RestController
@RequestMapping("/api/eventos")
@Validated
@Transactional(readOnly = true)
public class InteraccionController {

    private static final Logger AUDITORIA = LoggerFactory.getLogger("AUDITORIA");
    private static final int MAX_MEDIOS_POR_USUARIO_Y_EVENTO = 10;
    private static final int LIMITE_MURO = 30;
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png", "video/mp4", "video/webm");

    private final EventoRepository eventos;
    private final UsuarioRepository usuarios;
    private final ComentarioRepository comentarios;
    private final EventoMedioRepository medios;
    private final MedioContenidoRepository contenidos;
    private final EventoCompartidoRepository compartidos;
    private final ValidadorArchivos validador;

    public InteraccionController(EventoRepository eventos, UsuarioRepository usuarios,
                                 ComentarioRepository comentarios, EventoMedioRepository medios,
                                 MedioContenidoRepository contenidos, EventoCompartidoRepository compartidos,
                                 ValidadorArchivos validador) {
        this.eventos = eventos;
        this.usuarios = usuarios;
        this.comentarios = comentarios;
        this.medios = medios;
        this.contenidos = contenidos;
        this.compartidos = compartidos;
        this.validador = validador;
    }

    // ================================ Comentarios y valoraciones ================================

    @GetMapping("/{id}/comentarios")
    public Dtos.Comentarios listarComentarios(@PathVariable @Positive Long id, Authentication auth) {
        exigirEvento(id);
        List<Dtos.ComentarioDto> lista = comentarios.listarPorEvento(id).stream()
                .map(c -> Dtos.ComentarioDto.de(c, auth.getName())).toList();
        Double promedio = comentarios.promedio(id);
        double redondeado = promedio == null ? 0 : Math.round(promedio * 10) / 10.0;
        return new Dtos.Comentarios(redondeado, lista.size(), lista);
    }

    @PostMapping(value = "/{id}/comentarios", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Dtos.ComentarioDto> comentar(@PathVariable @Positive Long id,
                                                       @Valid @RequestBody Dtos.NuevoComentario nuevo,
                                                       Authentication auth) {
        exigirEvento(id);
        Usuario usuario = usuarioActual(auth);
        String texto = limpiarTexto(nuevo.texto());
        if (texto.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El comentario es obligatorio");
        }
        Comentario c = comentarios.save(new Comentario(id, usuario, nuevo.valoracion(), texto, LocalDateTime.now()));
        AUDITORIA.info("COMENTARIO_CREADO usuario={} evento={} comentario={}", usuario.getUsername(), id, c.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Dtos.ComentarioDto.de(c, usuario.getUsername()));
    }

    @DeleteMapping("/{id}/comentarios/{comentarioId}")
    @Transactional
    public ResponseEntity<Void> eliminarComentario(@PathVariable @Positive Long id,
                                                   @PathVariable @Positive Long comentarioId,
                                                   Authentication auth) {
        Comentario c = comentarios.buscarEnEvento(comentarioId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentario no encontrado"));
        if (!c.getUsuario().getUsername().equals(auth.getName())) {
            AUDITORIA.warn("ACCESO_DENEGADO accion=eliminar_comentario usuario={} comentario={}",
                    auth.getName(), comentarioId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el autor puede eliminar su comentario");
        }
        comentarios.delete(c);
        AUDITORIA.info("COMENTARIO_ELIMINADO usuario={} evento={} comentario={}", auth.getName(), id, comentarioId);
        return ResponseEntity.noContent().build();
    }

    // ===================================== Fotos y videos =====================================

    @GetMapping("/{id}/medios")
    public List<Dtos.MedioDto> listarMedios(@PathVariable @Positive Long id) {
        exigirEvento(id);
        return medios.listarPorEvento(id).stream().map(Dtos.MedioDto::de).toList();
    }

    @PostMapping(value = "/{id}/medios", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<Dtos.MedioDto> subirMedio(
            @PathVariable @Positive Long id,
            @RequestPart("archivo") MultipartFile archivo,
            @RequestParam("descripcion") @NotBlank @Size(max = 150) String descripcion,
            Authentication auth) {
        exigirEvento(id);
        Usuario usuario = usuarioActual(auth);
        if (medios.countByEventoIdAndUsuarioId(id, usuario.getId()) >= MAX_MEDIOS_POR_USUARIO_Y_EVENTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Alcanzaste el máximo de " + MAX_MEDIOS_POR_USUARIO_Y_EVENTO + " archivos para este evento");
        }
        String texto = limpiarTexto(descripcion);
        if (texto.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción es obligatoria");
        }

        ValidadorArchivos.ArchivoValidado valido = validador.validar(archivo);
        EventoMedio medio = medios.save(new EventoMedio(id, usuario, valido.tipo(), valido.contentType(),
                texto, valido.datos().length, LocalDateTime.now()));
        contenidos.save(new MedioContenido(medio.getId(), valido.datos()));

        AUDITORIA.info("MEDIO_SUBIDO usuario={} evento={} medio={} tipo={} bytes={}",
                usuario.getUsername(), id, medio.getId(), valido.contentType(), valido.datos().length);
        return ResponseEntity.status(HttpStatus.CREATED).body(Dtos.MedioDto.de(medio));
    }

    @GetMapping("/medios/{medioId}/contenido")
    public ResponseEntity<byte[]> contenidoMedio(@PathVariable @Positive Long medioId) {
        EventoMedio medio = medios.findById(medioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado"));
        MedioContenido contenido = contenidos.findById(medioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado"));
        if (!TIPOS_PERMITIDOS.contains(medio.getContentType())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(medio.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(contenido.getDatos());
    }

    // =================================== Compartir eventos ===================================

    @GetMapping("/{id}/compartidos")
    public Dtos.ResumenCompartidos resumenCompartidos(@PathVariable @Positive Long id) {
        exigirEvento(id);
        Map<String, Long> porDestino = new LinkedHashMap<>();
        for (DestinoCompartir d : DestinoCompartir.values()) {
            porDestino.put(d.name(), 0L);
        }
        long total = 0;
        for (Object[] fila : compartidos.contarPorDestino(id)) {
            long cantidad = ((Number) fila[1]).longValue();
            porDestino.put(((DestinoCompartir) fila[0]).name(), cantidad);
            total += cantidad;
        }
        return new Dtos.ResumenCompartidos(total, porDestino);
    }

    @PostMapping(value = "/{id}/compartidos", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Dtos.CompartidoDto> compartir(@PathVariable @Positive Long id,
                                                        @Valid @RequestBody Dtos.NuevoCompartido nuevo,
                                                        Authentication auth) {
        Evento evento = eventos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
        Usuario usuario = usuarioActual(auth);
        // El mensaje solo se publica en el muro del sitio; en redes sociales lo escribe el usuario allá.
        String mensaje = nuevo.destino() == DestinoCompartir.SITIO ? limpiarTexto(nuevo.mensaje()) : "";
        EventoCompartido c = compartidos.save(new EventoCompartido(evento, usuario, nuevo.destino(),
                mensaje.isEmpty() ? null : mensaje, LocalDateTime.now()));
        AUDITORIA.info("EVENTO_COMPARTIDO usuario={} evento={} destino={}", usuario.getUsername(), id, nuevo.destino());
        return ResponseEntity.status(HttpStatus.CREATED).body(Dtos.CompartidoDto.de(c));
    }

    /** Muro de la comunidad: últimos eventos compartidos en el sitio. */
    @GetMapping("/compartidos")
    public List<Dtos.CompartidoDto> muro() {
        return compartidos.recientesPorDestino(DestinoCompartir.SITIO, PageRequest.of(0, LIMITE_MURO))
                .stream().map(Dtos.CompartidoDto::de).toList();
    }

    // ======================================== Apoyo ========================================

    private void exigirEvento(Long id) {
        if (!eventos.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado");
        }
    }

    /** El usuario sale del JWT ya validado; si fue desactivado después de emitir el token, se rechaza. */
    private Usuario usuarioActual(Authentication auth) {
        return usuarios.findByUsernameAndActivoTrue(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no válido"));
    }

    /** Quita caracteres de control (excepto salto de línea) y espacios sobrantes. */
    private static String limpiarTexto(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("\r\n", "\n").replaceAll("[\\p{Cntrl}&&[^\n]]", "").strip();
    }
}
