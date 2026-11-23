package cl.sumativa.eventosapi.repository;

import cl.sumativa.eventosapi.domain.DestinoCompartir;
import cl.sumativa.eventosapi.domain.EventoCompartido;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventoCompartidoRepository extends JpaRepository<EventoCompartido, Long> {

    @Query("select c from EventoCompartido c join fetch c.usuario join fetch c.evento "
            + "where c.destino = :destino order by c.fecha desc")
    List<EventoCompartido> recientesPorDestino(DestinoCompartir destino, Pageable pageable);

    @Query("select c.destino, count(c) from EventoCompartido c where c.evento.id = :eventoId group by c.destino")
    List<Object[]> contarPorDestino(Long eventoId);
}
