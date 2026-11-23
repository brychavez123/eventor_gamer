package cl.sumativa.eventosweb.web;

import cl.sumativa.eventosweb.client.BackendClient;
import cl.sumativa.eventosweb.client.Modelos;
import cl.sumativa.eventosweb.security.JwtAutenticacion;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Controller
public class PaginasController {

    private static final Logger log = LoggerFactory.getLogger(PaginasController.class);

    private final BackendClient backend;
    private final EnlacesCompartir enlaces;

    public PaginasController(BackendClient backend, EnlacesCompartir enlaces) {
        this.backend = backend;
        this.enlaces = enlaces;
    }

    /** [Pública] Inicio: eventos recientes, populares, banners y acceso al login. */
    @GetMapping("/")
    public String inicio(Model model) {
        try {
            model.addAttribute("recientes", backend.recientes());
            model.addAttribute("populares", backend.populares());
            model.addAttribute("banners", backend.banners());
        } catch (RestClientException e) {
            log.warn("Backend no disponible al cargar el inicio: {}", e.getClass().getSimpleName());
            model.addAttribute("recientes", List.of());
            model.addAttribute("populares", List.of());
            model.addAttribute("banners", List.of());
            model.addAttribute("avisoBackend", true);
        }
        return "inicio";
    }

    /** [Pública] Búsqueda y filtrado de eventos. */
    @GetMapping("/buscar")
    public String buscar(@Valid @ModelAttribute("filtro") FiltroBusqueda filtro, BindingResult errores, Model model) {
        try {
            model.addAttribute("opciones", backend.filtros());
            if (errores.hasErrors()) {
                model.addAttribute("filtroInvalido", true);
                model.addAttribute("resultados", List.of());
            } else {
                model.addAttribute("resultados", backend.buscar(filtro));
            }
        } catch (RestClientException e) {
            log.warn("Backend no disponible en búsqueda: {}", e.getClass().getSimpleName());
            model.addAttribute("opciones", Modelos.Filtros.vacios());
            model.addAttribute("resultados", List.of());
            model.addAttribute("avisoBackend", true);
        }
        model.addAttribute("buscado", !filtro.estaVacio());
        return "buscar";
    }

    /** [Privada] Detalle del evento: requiere sesión; se llama al backend con el JWT del usuario. */
    @GetMapping("/evento/{id}")
    public String detalle(@PathVariable long id, Authentication auth, Model model) {
        if (id <= 0) {
            throw new BackendClient.EventoNoEncontradoException();
        }
        String jwt = ((JwtAutenticacion) auth).getJwt();
        model.addAttribute("evento", backend.detalle(id, jwt));
        // Semana 5: galería de la comunidad, comentarios/valoraciones y contador de compartidos
        model.addAttribute("medios", backend.medios(id, jwt));
        model.addAttribute("comentarios", backend.comentarios(id, jwt));
        model.addAttribute("compartidos", backend.resumenCompartidos(id, jwt));
        model.addAttribute("urlEvento", enlaces.urlEvento(id));
        if (!model.containsAttribute("comentarioForm")) {
            model.addAttribute("comentarioForm", new ComentarioForm());
        }
        return "detalle";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
