package cl.sumativa.eventosapi.domain;

import jakarta.persistence.*;

/** Bytes del archivo subido, en tabla aparte para no cargarlos al listar la galería. */
@Entity
@Table(name = "evento_medio_contenido")
public class MedioContenido {

    @Id
    @Column(name = "medio_id")
    private Long medioId;

    @Lob
    @Column(name = "datos")
    private byte[] datos;

    protected MedioContenido() { }

    public MedioContenido(Long medioId, byte[] datos) {
        this.medioId = medioId;
        this.datos = datos;
    }

    public Long getMedioId() { return medioId; }
    public byte[] getDatos() { return datos; }
}
