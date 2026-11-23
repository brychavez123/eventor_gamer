package cl.sumativa.eventosapi.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Foto o video subido por un usuario a un evento (solo metadatos; los bytes están en MedioContenido). */
@Entity
@Table(name = "evento_medio")
public class EventoMedio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id")
    private Long eventoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    private TipoMedio tipo;

    @Column(name = "content_type")
    private String contentType;

    private String descripcion;
    private Integer tamano;

    @Column(name = "fecha_subida")
    private LocalDateTime fechaSubida;

    protected EventoMedio() { }

    public EventoMedio(Long eventoId, Usuario usuario, TipoMedio tipo, String contentType,
                       String descripcion, int tamano, LocalDateTime fechaSubida) {
        this.eventoId = eventoId;
        this.usuario = usuario;
        this.tipo = tipo;
        this.contentType = contentType;
        this.descripcion = descripcion;
        this.tamano = tamano;
        this.fechaSubida = fechaSubida;
    }

    public Long getId() { return id; }
    public Long getEventoId() { return eventoId; }
    public Usuario getUsuario() { return usuario; }
    public TipoMedio getTipo() { return tipo; }
    public String getContentType() { return contentType; }
    public String getDescripcion() { return descripcion; }
    public Integer getTamano() { return tamano; }
    public LocalDateTime getFechaSubida() { return fechaSubida; }
}
