package cl.sumativa.eventosapi.repository;

import cl.sumativa.eventosapi.domain.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    @Query("select c from Comentario c join fetch c.usuario where c.eventoId = :eventoId order by c.fecha desc")
    List<Comentario> listarPorEvento(Long eventoId);

    @Query("select avg(c.valoracion) from Comentario c where c.eventoId = :eventoId")
    Double promedio(Long eventoId);

    /** Busca el comentario dentro del evento indicado: evita borrar comentarios de otro evento cambiando la URL. */
    @Query("select c from Comentario c join fetch c.usuario where c.id = :id and c.eventoId = :eventoId")
    Optional<Comentario> buscarEnEvento(Long id, Long eventoId);
}
