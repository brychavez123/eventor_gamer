package cl.sumativa.eventosapi.repository;

import cl.sumativa.eventosapi.domain.Evento;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA genera consultas parametrizadas (PreparedStatement): la entrada del
 * usuario nunca se concatena en el SQL (mitigación de A03 Inyección).
 */
public interface EventoRepository extends JpaRepository<Evento, Long>, JpaSpecificationExecutor<Evento> {

    List<Evento> findAllByOrderByFechaPublicacionDesc(Pageable pageable);

    List<Evento> findAllByOrderByInscritosDesc(Pageable pageable);

    @EntityGraph(attributePaths = "fotos")
    @Query("select e from Evento e where e.id = :id")
    Optional<Evento> findDetalleById(Long id);

    @Query("select distinct e.categoria from Evento e order by e.categoria")
    List<String> categorias();

    @Query("select distinct e.videojuego from Evento e order by e.videojuego")
    List<String> videojuegos();

    @Query("select distinct e.pais from Evento e order by e.pais")
    List<String> paises();

    @Query("select distinct e.ciudad from Evento e order by e.ciudad")
    List<String> ciudades();
}
