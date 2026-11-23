package cl.sumativa.eventosweb.client;

import cl.sumativa.eventosweb.web.FiltroBusqueda;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP hacia la capa backend. Los parámetros de búsqueda se agregan con UriBuilder
 * (codificados), y el JWT viaja en el header Authorization: Bearer.
 */
@Component
public class BackendClient {

    private static final String AUTH = "Authorization";
    private static final String BEARER = "Bearer ";

    private final RestClient http;

    public BackendClient(RestClient.Builder builder, @Value("${backend.url}") String backendUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.http = builder.baseUrl(backendUrl).requestFactory(factory).build();
    }

    /** Llama a la API de login de la semana 2 y devuelve el token JWT. */
    public String login(String usuario, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("user", usuario);
        form.add("encryptedPass", password);
        try {
            String token = http.post().uri("/login")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            if (token == null || token.isBlank()) {
                throw new AuthenticationServiceException("Respuesta de login vacía");
            }
            return token.trim();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                throw new LockedException("Cuenta bloqueada temporalmente");
            }
            throw new BadCredentialsException("Credenciales inválidas");
        } catch (RestClientException e) {
            throw new AuthenticationServiceException("Servicio de autenticación no disponible", e);
        }
    }

    public List<Modelos.EventoResumen> recientes() {
        return getLista("/api/publico/eventos/recientes", new ParameterizedTypeReference<>() { });
    }

    public List<Modelos.EventoResumen> populares() {
        return getLista("/api/publico/eventos/populares", new ParameterizedTypeReference<>() { });
    }

    public List<Modelos.Banner> banners() {
        return getLista("/api/publico/banners", new ParameterizedTypeReference<>() { });
    }

    public Modelos.Filtros filtros() {
        return http.get().uri("/api/publico/filtros").retrieve().body(Modelos.Filtros.class);
    }

    public List<Modelos.EventoResumen> buscar(FiltroBusqueda f) {
        return http.get().uri(b -> construirBusqueda(b, f))
                .retrieve()
                .body(new ParameterizedTypeReference<List<Modelos.EventoResumen>>() { });
    }

    /** Detalle privado: se envía el JWT del usuario autenticado. */
    public Modelos.EventoDetalle detalle(long id, String jwt) {
        return http.get().uri("/api/eventos/{id}", id)
                .header("Authorization", "Bearer " + jwt)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    int status = res.getStatusCode().value();
                    if (status == 401 || status == 403) {
                        throw new SesionBackendExpiradaException();
                    }
                    if (status == 404) {
                        throw new EventoNoEncontradoException();
                    }
                })
                .body(Modelos.EventoDetalle.class);
    }

    // ------------------------- Semana 5: APIs privadas (siempre con el JWT del usuario) -------------------------

    public Modelos.Comentarios comentarios(long eventoId, String jwt) {
        return http.get().uri("/api/eventos/{id}/comentarios", eventoId)
                .header(AUTH, BEARER + jwt)
                .retrieve().onStatus(HttpStatusCode::isError, BackendClient::manejarError)
                .body(Modelos.Comentarios.class);
    }

    public void comentar(long eventoId, int valoracion, String texto, String jwt) {
        http.post().uri("/api/eventos/{id}/comentarios", eventoId)
                .header(AUTH, BEARER + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("valoracion", valoracion, "texto", texto))
                .retrieve().onStatus(HttpStatusCode::isError, BackendClient::manejarError)
                .toBodilessEntity();
    }

    public void eliminarComentario(long eventoId, long comentarioId, String jwt) {
        http.delete().uri("/api/eventos/{id}/comentarios/{cid}", eventoId, comentarioId)
                .header(AUTH, BEARER + jwt)
                .retrieve().onStatus(HttpStatusCode::isError, BackendClient::manejarError)
                .toBodilessEntity();
    }

    public List<Modelos.Medio> medios(long eventoId, String jwt) {
        List<Modelos.Medio> lista = http.get().uri("/api/eventos/{id}/medios", eventoId)
                .header(AUTH, BEARER + jwt)
                .retrieve().onStatus(HttpStatusCode::isError, BackendClient::manejarError)
                .body(new ParameterizedTypeReference<List<Modelos.Medio>>() { });
        return lista == null ? List.of() : lista;
    }

    /** Envía el archivo como multipart. El nombre original del cliente NO se reenvía (se usa uno fijo). */
    public void subirMedio(long eventoId, byte[] datos, String descripcion, String jwt) {
        MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
        partes.add("descripcion", descripcion);
        partes.add("archivo", new ByteArrayResource(datos) {
            @Override
            public String getFilename() {
                return "archivo";
            }
        });
        http.post().uri("/api/eventos/{id}/medios", eventoId)
                .header(AUTH, BEARER + jwt)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(partes)
                .retrieve().onStatus(HttpStatusCode::isError, BackendClient::manejarError)
                .toBodilessEntity();
    }

    public Modelos.ContenidoMedio contenidoMedio(long medioId, String jwt) {
        ResponseEntity<byte[]> r = http.get().uri("/api/eventos/medios/{id}/contenido", medioId)
                .header(AUTH, BEARER + jwt)
                .retrieve().onStatus(HttpStatusCode::isError, BackendClient::manejarError)
                .toEntity(byte[].class);
        MediaType tipo = r.getHeaders().getContentType();
        if (r.getBody() == null || tipo == null) {
            throw new EventoNoEncontradoException();
        }
        return new Modelos.ContenidoMedio(r.getBody(), tipo.getType() + "/" + tipo.getSubtype());
    }

    public Modelos.ResumenCompartidos resumenCompartidos(long eventoId, String jwt) {
        return http.get().uri("/api/eventos/{id}/compartidos", eventoId)
                .header(AUTH, BEARER + jwt)
                .retrieve().onStatus(HttpStatusCode::isError, BackendClient::manejarError)
                .body(Modelos.ResumenCompartidos.class);
    }

    public void compartir(long eventoId, String destino, String mensaje, String jwt) {
        Map<String, Object> cuerpo = new java.util.HashMap<>();
        cuerpo.put("destino", destino);
        cuerpo.put("mensaje", mensaje);
        http.post().uri("/api/eventos/{id}/compartidos", eventoId)
                .header(AUTH, BEARER + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve().onStatus(HttpStatusCode::isError, BackendClient::manejarError)
                .toBodilessEntity();
    }

    public List<Modelos.Compartido> muro(String jwt) {
        List<Modelos.Compartido> lista = http.get().uri("/api/eventos/compartidos")
                .header(AUTH, BEARER + jwt)
                .retrieve().onStatus(HttpStatusCode::isError, BackendClient::manejarError)
                .body(new ParameterizedTypeReference<List<Modelos.Compartido>>() { });
        return lista == null ? List.of() : lista;
    }

    /** Traduce los códigos del backend a excepciones propias (el detalle del backend no se muestra al usuario). */
    private static void manejarError(HttpRequest req, ClientHttpResponse res) throws IOException {
        int status = res.getStatusCode().value();
        switch (status) {
            case 401 -> throw new SesionBackendExpiradaException();
            case 403 -> throw new AccesoDenegadoException();
            case 404 -> throw new EventoNoEncontradoException();
            default -> {
                if (status >= 500) {
                    throw new RestClientException("El backend respondió " + status);
                }
                throw new OperacionRechazadaException(status);
            }
        }
    }

    private <T> List<T> getLista(String ruta, ParameterizedTypeReference<List<T>> tipo) {
        List<T> lista = http.get().uri(ruta).retrieve().body(tipo);
        return lista == null ? List.of() : lista;
    }

    /** Cada valor se pasa como variable de plantilla: RestClient lo codifica completo (incluye &, +, #). */
    private static URI construirBusqueda(UriBuilder b, FiltroBusqueda f) {
        b.path("/api/publico/eventos/buscar");
        java.util.Map<String, Object> vars = new java.util.HashMap<>();
        agregar(b, vars, "nombre", f.getNombre());
        agregar(b, vars, "videojuego", f.getVideojuego());
        agregar(b, vars, "categoria", f.getCategoria());
        agregar(b, vars, "pais", f.getPais());
        agregar(b, vars, "ciudad", f.getCiudad());
        agregar(b, vars, "modalidad", f.getModalidad());
        agregar(b, vars, "fecha", f.getFecha() == null ? null : f.getFecha().toString());
        return b.build(vars);
    }

    private static void agregar(UriBuilder b, java.util.Map<String, Object> vars, String nombre, String valor) {
        if (valor != null && !valor.isBlank()) {
            b.queryParam(nombre, "{" + nombre + "}");
            vars.put(nombre, valor.trim());
        }
    }

    public static class SesionBackendExpiradaException extends RuntimeException { }

    public static class EventoNoEncontradoException extends RuntimeException { }

    public static class AccesoDenegadoException extends RuntimeException { }

    /** El backend rechazó la operación por datos inválidos (400), límite alcanzado (409) o archivo grande (413). */
    public static class OperacionRechazadaException extends RuntimeException {
        private final int status;

        public OperacionRechazadaException(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}
