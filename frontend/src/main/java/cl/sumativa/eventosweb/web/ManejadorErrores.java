package cl.sumativa.eventosweb.web;

import cl.sumativa.eventosweb.client.BackendClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

/**
 * Errores controlados: el usuario ve una página genérica, sin trazas ni nombres de clases (A05).
 */
@ControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);
    private static final java.util.regex.Pattern RUTA_MEDIOS = java.util.regex.Pattern.compile("/evento/(\\d{1,18})/medios$");

    /** El JWT expiró o fue rechazado: se cierra la sesión local y se pide iniciar sesión otra vez. */
    @ExceptionHandler(BackendClient.SesionBackendExpiradaException.class)
    public String sesionExpirada(HttpServletRequest req, HttpServletResponse res) {
        new SecurityContextLogoutHandler().logout(req, res, SecurityContextHolder.getContext().getAuthentication());
        return "redirect:/login?expirado";
    }

    @ExceptionHandler({BackendClient.EventoNoEncontradoException.class, MethodArgumentTypeMismatchException.class})
    public ModelAndView noEncontrado(HttpServletResponse res) {
        return error(res, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BackendClient.AccesoDenegadoException.class)
    public ModelAndView accesoDenegado(HttpServletResponse res) {
        return error(res, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(BackendClient.OperacionRechazadaException.class)
    public ModelAndView rechazada(HttpServletResponse res) {
        return error(res, HttpStatus.BAD_REQUEST);
    }

    /** Archivo sobre el límite del servidor: se vuelve al detalle del evento con un aviso. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object archivoGrande(HttpServletRequest req, HttpServletResponse res) {
        java.util.regex.Matcher m = RUTA_MEDIOS.matcher(req.getRequestURI());
        if (m.find()) {
            return "redirect:/evento/" + m.group(1) + "?archivoGrande#medios";
        }
        return error(res, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(RestClientException.class)
    public ModelAndView backendCaido(RestClientException e, HttpServletResponse res) {
        log.warn("Error al comunicarse con el backend: {}", e.getClass().getSimpleName());
        return error(res, HttpStatus.SERVICE_UNAVAILABLE);
    }

    private static ModelAndView error(HttpServletResponse res, HttpStatus status) {
        res.setStatus(status.value());
        ModelAndView mv = new ModelAndView("error");
        mv.addObject("status", status.value());
        mv.setStatus(status);
        return mv;
    }
}
