package cl.sumativa.eventosapi.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "evento_foto")
public class EventoFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id")
    private Evento evento;

    private String url;

    @Column(name = "texto_alternativo")
    private String textoAlternativo;

    private Integer orden;

    public Long getId() { return id; }
    public String getUrl() { return url; }
    public String getTextoAlternativo() { return textoAlternativo; }
    public Integer getOrden() { return orden; }
}
