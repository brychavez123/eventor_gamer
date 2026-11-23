package cl.sumativa.eventosapi.web;

import cl.sumativa.eventosapi.dto.Dtos;
import cl.sumativa.eventosapi.repository.EventoRepository;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** API privada: detalle de eventos. SecurityConfig exige JWT válido para /api/eventos/**. */
@RestController
@RequestMapping("/api/eventos")
@Validated
@Transactional(readOnly = true)
public class EventoPrivadoController {

    private final EventoRepository eventos;

    public EventoPrivadoController(EventoRepository eventos) {
        this.eventos = eventos;
    }

    @GetMapping("/{id}")
    public Dtos.EventoDetalle detalle(@PathVariable @Positive Long id) {
        return eventos.findDetalleById(id)
                .map(Dtos.EventoDetalle::de)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
    }
}
