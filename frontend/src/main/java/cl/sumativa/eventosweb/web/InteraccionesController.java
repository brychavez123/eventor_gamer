package cl.sumativa.eventosweb.web;

import cl.sumativa.eventosweb.client.BackendClient;
import cl.sumativa.eventosweb.client.Modelos;
import cl.sumativa.eventosweb.security.JwtAutenticacion;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * Páginas y acciones privadas de la semana 5 (requieren sesión: WebSecurityConfig deja como
 * privada toda ruta que no sea /, /buscar, /login y recursos estáticos):
 *  - POST /evento/{id}/medios                    agregar foto o video
 *  - GET  /media/{id}                             mostrar foto o video (pasa por el frontend con el JWT)
 *  - POST /evento/{id}/comentarios                comentar y valorar
 *  - POST /evento/{id}/comentarios/{cid}/eliminar eliminar comentario propio
 *  - POST /evento/{id}/compartir                  compartir en el muro o en redes sociales
 *  - GET  /compartidos                            muro de la comunidad
 *
 * Todos los formularios POST llevan token CSRF (Thymeleaf lo agrega con th:action) y usan el patrón
 * Post/Redirect/Get para no reenviar datos al recargar la página.
 */
@Controller
public class InteraccionesController {

    private static final long MAX_BYTES_FOTO = 5L * 1024 * 1024;
    private static final long MAX_BYTES_VIDEO = 20L * 1024 * 1024;
    private static final Map<String, MediaType> TIPOS_MEDIO = Map.of(
            "image/jpeg", MediaType.IMAGE_JPEG,
            "image/png", MediaType.IMAGE_PNG,
            "video/mp4", MediaType.parseMediaType("video/mp4"),
            "video/webm", MediaType.parseMediaType("video/webm"));

    private final BackendClient backend;
    private final EnlacesCompartir enlaces;

    public InteraccionesController(BackendClient backend, EnlacesCompartir enlaces) {
        this.backend = backend;
        this.enlaces = enlaces;
    }

    // ===================================== Comentarios =====================================

    @PostMapping("/evento/{id}/comentarios")
    public String comentar(@PathVariable long id,
                           @Valid @ModelAttribute("comentarioForm") ComentarioForm form, BindingResult errores,
                           Authentication auth, RedirectAttributes ra) {
        validarId(id);
        if (errores.hasErrors()) {
            ra.addFlashAttribute("errorComentario", primerError(errores));
            ra.addFlashAttribute("comentarioForm", form);
        } else {
            try {
                backend.comentar(id, form.getValoracion(), form.getTexto().strip(), jwt(auth));
                ra.addFlashAttribute("okComentario", "¡Gracias! Tu comentario fue publicado.");
            } catch (BackendClient.OperacionRechazadaException e) {
                ra.addFlashAttribute("errorComentario", "No pudimos publicar el comentario. Revisa los datos.");
                ra.addFlashAttribute("comentarioForm", form);
            }
        }
        return "redirect:/evento/" + id + "#comentarios";
    }

    @PostMapping("/evento/{id}/comentarios/{comentarioId}/eliminar")
    public String eliminarComentario(@PathVariable long id, @PathVariable long comentarioId,
                                     Authentication auth, RedirectAttributes ra) {
        validarId(id);
        validarId(comentarioId);
        try {
            backend.eliminarComentario(id, comentarioId, jwt(auth));
            ra.addFlashAttribute("okComentario", "Tu comentario fue eliminado.");
        } catch (BackendClient.AccesoDenegadoException e) {
            ra.addFlashAttribute("errorComentario", "Solo puedes eliminar tus propios comentarios.");
        } catch (BackendClient.EventoNoEncontradoException e) {
            ra.addFlashAttribute("errorComentario", "El comentario ya no existe.");
        }
        return "redirect:/evento/" + id + "#comentarios";
    }

    // =================================== Fotos y videos ===================================

    @PostMapping(value = "/evento/{id}/medios", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String subirMedio(@PathVariable long id,
                             @RequestParam(value = "archivo", required = false) MultipartFile archivo,
                             @RequestParam(value = "descripcion", required = false) String descripcion,
                             Authentication auth, RedirectAttributes ra) {
        validarId(id);
        String texto = descripcion == null ? "" : descripcion.strip();
        String error = validarArchivo(archivo);
        if (error == null && texto.isEmpty()) {
            error = "Escribe una descripción para el archivo.";
        } else if (error == null && texto.length() > 150) {
            error = "La descripción admite máximo 150 caracteres.";
        }

        if (error == null) {
            try {
                backend.subirMedio(id, archivo.getBytes(), texto, jwt(auth));
                ra.addFlashAttribute("okMedio", "¡Archivo agregado a la galería del evento!");
                return "redirect:/evento/" + id + "#medios";
            } catch (BackendClient.OperacionRechazadaException e) {
                error = switch (e.getStatus()) {
                    case 409 -> "Alcanzaste el máximo de 10 archivos para este evento.";
                    case 413 -> "El archivo supera el tamaño permitido (fotos 5 MB, videos 20 MB).";
                    default -> "El archivo no es válido. Solo se aceptan imágenes JPG o PNG y videos MP4 o WebM.";
                };
            } catch (IOException e) {
                error = "No pudimos leer el archivo. Intenta nuevamente.";
            }
        }
        ra.addFlashAttribute("errorMedio", error);
        ra.addFlashAttribute("descripcionMedio", texto);
        return "redirect:/evento/" + id + "#medios";
    }

    /**
     * Entrega la foto o video pidiéndolo al backend con el JWT del usuario. Solo se aceptan los
     * tipos de la lista blanca; el navegador no puede "adivinar" otro tipo (nosniff de Spring Security).
     * Al devolver un Resource, Spring atiende las peticiones Range (avance del video).
     */
    @GetMapping("/media/{medioId}")
    public ResponseEntity<Resource> medio(@PathVariable long medioId, Authentication auth) {
        validarId(medioId);
        Modelos.ContenidoMedio contenido = backend.contenidoMedio(medioId, jwt(auth));
        MediaType tipo = TIPOS_MEDIO.get(contenido.contentType());
        if (tipo == null) {
            throw new BackendClient.EventoNoEncontradoException();
        }
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().build().toString())
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(new ByteArrayResource(contenido.datos()));
    }

    // ===================================== Compartir =====================================

    @PostMapping("/evento/{id}/compartir")
    public String compartir(@PathVariable long id,
                            @Valid @ModelAttribute("compartirForm") CompartirForm form, BindingResult errores,
                            Authentication auth, RedirectAttributes ra) {
        validarId(id);
        if (errores.hasErrors()) {
            ra.addFlashAttribute("errorCompartir", primerError(errores));
            return "redirect:/evento/" + id + "#compartir";
        }
        String mensaje = form.getMensaje() == null ? null : form.getMensaje().strip();
        try {
            backend.compartir(id, form.getDestino(), mensaje, jwt(auth));
        } catch (BackendClient.OperacionRechazadaException e) {
            ra.addFlashAttribute("errorCompartir", "No pudimos compartir el evento. Intenta nuevamente.");
            return "redirect:/evento/" + id + "#compartir";
        }

        if ("SITIO".equals(form.getDestino())) {
            ra.addFlashAttribute("okCompartir", "Publicaste el evento en el muro de la comunidad.");
        } else {
            // La CSP (form-action 'self') no permite redirigir el formulario a otro dominio:
            // se muestra un enlace que el usuario abre en una pestaña nueva.
            ra.addFlashAttribute("okCompartir", "¡Listo! Termina de publicar en la red social.");
            ra.addFlashAttribute("enlaceExterno", enlaces.enlace(form.getDestino(), id));
            ra.addFlashAttribute("redExterna", form.getDestino());
        }
        return "redirect:/evento/" + id + "#compartir";
    }

    @GetMapping("/compartidos")
    public String muro(Authentication auth, Model model) {
        model.addAttribute("publicaciones", backend.muro(jwt(auth)));
        return "compartidos";
    }

    // ======================================== Apoyo ========================================

    /** Validación rápida en el frontend; la validación definitiva (firma real del archivo) la hace el backend. */
    private static String validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return "Selecciona una foto o un video.";
        }
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        boolean esFoto = nombre.endsWith(".jpg") || nombre.endsWith(".jpeg") || nombre.endsWith(".png");
        boolean esVideo = nombre.endsWith(".mp4") || nombre.endsWith(".webm");
        if (!esFoto && !esVideo) {
            return "Formato no permitido. Solo se aceptan JPG, PNG, MP4 o WebM.";
        }
        if (esFoto && archivo.getSize() > MAX_BYTES_FOTO) {
            return "La imagen supera el máximo de 5 MB.";
        }
        if (esVideo && archivo.getSize() > MAX_BYTES_VIDEO) {
            return "El video supera el máximo de 20 MB.";
        }
        return null;
    }

    /** Solo se muestran los mensajes definidos en las anotaciones (nunca el texto técnico de conversión). */
    private static String primerError(BindingResult errores) {
        FieldError e = errores.getFieldError();
        if (e == null || e.isBindingFailure() || e.getDefaultMessage() == null) {
            return "Los datos enviados no son válidos.";
        }
        return e.getDefaultMessage();
    }

    private static void validarId(long id) {
        if (id <= 0) {
            throw new BackendClient.EventoNoEncontradoException();
        }
    }

    private static String jwt(Authentication auth) {
        return ((JwtAutenticacion) auth).getJwt();
    }
}
