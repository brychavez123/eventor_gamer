package cl.sumativa.eventosapi.web;

import cl.sumativa.eventosapi.dto.Dtos;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Manejo centralizado de errores: el cliente recibe mensajes genéricos y el detalle
 * técnico (stack trace, SQL, clases) queda solo en el log del servidor (A05 / A09).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<Dtos.Error> peticionInvalida(Exception e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Parámetros inválidos");
    }

    /** Cuerpo JSON mal formado o con un valor fuera de la lista (por ejemplo un destino inexistente). */
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestPartException.class,
            MultipartException.class})
    public ResponseEntity<Dtos.Error> cuerpoInvalido(Exception e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos");
    }

    /** Se devuelven solo los mensajes definidos por nosotros en las anotaciones, nunca detalles internos. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Dtos.Error> validacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst().orElse("Datos de entrada inválidos");
        return respuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Dtos.Error> archivoGrande(Exception e) {
        return respuesta(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño permitido (fotos 5 MB, videos 20 MB)");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Dtos.Error> metodoNoPermitido(Exception e) {
        return respuesta(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Dtos.Error> noEncontrado(Exception e) {
        return respuesta(HttpStatus.NOT_FOUND, "Recurso no encontrado");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Dtos.Error> estado(ResponseStatusException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        return respuesta(status, e.getReason() == null ? status.getReasonPhrase() : e.getReason());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Dtos.Error> inesperado(Exception e) {
        log.error("Error no controlado", e);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    private static ResponseEntity<Dtos.Error> respuesta(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(new Dtos.Error(status.value(), mensaje));
    }
}
