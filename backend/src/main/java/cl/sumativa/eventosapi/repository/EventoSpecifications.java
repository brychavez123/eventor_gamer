package cl.sumativa.eventosapi.repository;

import cl.sumativa.eventosapi.domain.Evento;
import cl.sumativa.eventosapi.domain.Modalidad;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Filtros de búsqueda construidos con Criteria API. Los valores viajan como parámetros
 * enlazados (bind parameters), nunca concatenados en la consulta.
 */
public final class EventoSpecifications {

    private EventoSpecifications() { }

    public static Specification<Evento> contiene(String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String patron = "%" + escaparLike(valor.trim().toLowerCase()) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get(campo)), patron, '\\');
    }

    public static Specification<Evento> igual(String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get(campo)), valor.trim().toLowerCase());
    }

    public static Specification<Evento> modalidad(Modalidad modalidad) {
        if (modalidad == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("modalidad"), modalidad);
    }

    public static Specification<Evento> enFecha(LocalDate fecha) {
        if (fecha == null) {
            return null;
        }
        return (root, query, cb) -> cb.between(root.get("fechaHora"),
                fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay().minusNanos(1));
    }

    /** Evita que '%' o '_' escritos por el usuario se interpreten como comodines. */
    private static String escaparLike(String valor) {
        return valor.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
