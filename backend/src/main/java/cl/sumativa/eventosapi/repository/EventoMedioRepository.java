package cl.sumativa.eventosapi.repository;

import cl.sumativa.eventosapi.domain.EventoMedio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventoMedioRepository extends JpaRepository<EventoMedio, Long> {

    @Query("select m from EventoMedio m join fetch m.usuario where m.eventoId = :eventoId order by m.fechaSubida asc")
    List<EventoMedio> listarPorEvento(Long eventoId);

    long countByEventoIdAndUsuarioId(Long eventoId, Long usuarioId);
}
