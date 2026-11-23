package cl.sumativa.eventosapi.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comentario")
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id")
    private Long eventoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private Integer valoracion;
    private String texto;
    private LocalDateTime fecha;

    protected Comentario() { }

    public Comentario(Long eventoId, Usuario usuario, int valoracion, String texto, LocalDateTime fecha) {
        this.eventoId = eventoId;
        this.usuario = usuario;
        this.valoracion = valoracion;
        this.texto = texto;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public Long getEventoId() { return eventoId; }
    public Usuario getUsuario() { return usuario; }
    public Integer getValoracion() { return valoracion; }
    public String getTexto() { return texto; }
    public LocalDateTime getFecha() { return fecha; }
}
