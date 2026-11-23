package cl.sumativa.eventosapi.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "banner")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String texto;
    private String imagen;
    private String enlace;
    private Boolean activo;
    private Integer orden;

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getTexto() { return texto; }
    public String getImagen() { return imagen; }
    public String getEnlace() { return enlace; }
    public Boolean getActivo() { return activo; }
    public Integer getOrden() { return orden; }
}
