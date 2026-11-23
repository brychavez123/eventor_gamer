package cl.sumativa.eventosapi.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evento")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String videojuego;
    private String categoria;
    private String pais;
    private String ciudad;

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    private Modalidad modalidad;

    private String ubicacion;

    @Column(name = "enlace_acceso")
    private String enlaceAcceso;

    private String organizador;

    @Column(name = "descripcion_corta")
    private String descripcionCorta;

    private String descripcion;
    private String requisitos;
    private String inscripcion;
    private Integer cupos;
    private Integer inscritos;

    @Column(name = "imagen_portada")
    private String imagenPortada;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    @OneToMany(mappedBy = "evento", fetch = FetchType.LAZY)
    @OrderBy("orden ASC")
    private List<EventoFoto> fotos = new ArrayList<>();

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getVideojuego() { return videojuego; }
    public String getCategoria() { return categoria; }
    public String getPais() { return pais; }
    public String getCiudad() { return ciudad; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public Modalidad getModalidad() { return modalidad; }
    public String getUbicacion() { return ubicacion; }
    public String getEnlaceAcceso() { return enlaceAcceso; }
    public String getOrganizador() { return organizador; }
    public String getDescripcionCorta() { return descripcionCorta; }
    public String getDescripcion() { return descripcion; }
    public String getRequisitos() { return requisitos; }
    public String getInscripcion() { return inscripcion; }
    public Integer getCupos() { return cupos; }
    public Integer getInscritos() { return inscritos; }
    public String getImagenPortada() { return imagenPortada; }
    public LocalDateTime getFechaPublicacion() { return fechaPublicacion; }
    public List<EventoFoto> getFotos() { return fotos; }
}
