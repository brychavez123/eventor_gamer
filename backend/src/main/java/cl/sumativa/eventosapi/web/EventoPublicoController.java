package cl.sumativa.eventosapi.web;

import cl.sumativa.eventosapi.domain.Evento;
import cl.sumativa.eventosapi.domain.Modalidad;
import cl.sumativa.eventosapi.dto.Dtos;
import cl.sumativa.eventosapi.repository.BannerRepository;
import cl.sumativa.eventosapi.repository.EventoRepository;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static cl.sumativa.eventosapi.repository.EventoSpecifications.*;

/** API pública: inicio (recientes, populares, banners) y búsqueda de eventos. */
@RestController
@RequestMapping("/api/publico")
@Validated
@Transactional(readOnly = true)
public class EventoPublicoController {

    private static final int LIMITE_INICIO = 6;
    private static final int LIMITE_BUSQUEDA = 50;

    private final EventoRepository eventos;
    private final BannerRepository banners;

    public EventoPublicoController(EventoRepository eventos, BannerRepository banners) {
        this.eventos = eventos;
        this.banners = banners;
    }

    @GetMapping("/eventos/recientes")
    public List<Dtos.EventoResumen> recientes() {
        return eventos.findAllByOrderByFechaPublicacionDesc(PageRequest.of(0, LIMITE_INICIO))
                .stream().map(Dtos.EventoResumen::de).toList();
    }

    @GetMapping("/eventos/populares")
    public List<Dtos.EventoResumen> populares() {
        return eventos.findAllByOrderByInscritosDesc(PageRequest.of(0, LIMITE_INICIO))
                .stream().map(Dtos.EventoResumen::de).toList();
    }

    @GetMapping("/banners")
    public List<Dtos.BannerDto> banners() {
        return banners.findByActivoTrueOrderByOrdenAsc().stream().map(Dtos.BannerDto::de).toList();
    }

    @GetMapping("/filtros")
    public Dtos.Filtros filtros() {
        return new Dtos.Filtros(eventos.categorias(), eventos.videojuegos(), eventos.paises(),
                eventos.ciudades(), Arrays.stream(Modalidad.values()).map(Enum::name).toList());
    }

    /** Búsqueda con validación de entrada: longitudes máximas, enum y fecha ISO tipadas. */
    @GetMapping("/eventos/buscar")
    public List<Dtos.EventoResumen> buscar(
            @RequestParam(required = false) @Size(max = 60) String nombre,
            @RequestParam(required = false) @Size(max = 60) String videojuego,
            @RequestParam(required = false) @Size(max = 40) String categoria,
            @RequestParam(required = false) @Size(max = 40) String pais,
            @RequestParam(required = false) @Size(max = 40) String ciudad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Modalidad modalidad) {

        Specification<Evento> spec = Specification.allOf(
                contiene("nombre", nombre),
                contiene("videojuego", videojuego),
                igual("categoria", categoria),
                igual("pais", pais),
                igual("ciudad", ciudad),
                enFecha(fecha),
                modalidad(modalidad));

        return eventos.findAll(spec, PageRequest.of(0, LIMITE_BUSQUEDA, Sort.by("fechaHora")))
                .stream().map(Dtos.EventoResumen::de).toList();
    }
}
