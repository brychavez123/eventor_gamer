package cl.sumativa.eventosapi.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "evento_compartido")
public class EventoCompartido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id")
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    private DestinoCompartir destino;

    private String mensaje;
    private LocalDateTime fecha;

    protected EventoCompartido() { }

    public EventoCompartido(Evento evento, Usuario usuario, DestinoCompartir destino, String mensaje,
                            LocalDateTime fecha) {
        this.evento = evento;
        this.usuario = usuario;
        this.destino = destino;
        this.mensaje = mensaje;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public Evento getEvento() { return evento; }
    public Usuario getUsuario() { return usuario; }
    public DestinoCompartir getDestino() { return destino; }
    public String getMensaje() { return mensaje; }
    public LocalDateTime getFecha() { return fecha; }
}
