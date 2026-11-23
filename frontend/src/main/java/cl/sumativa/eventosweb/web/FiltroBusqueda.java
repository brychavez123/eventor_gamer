package cl.sumativa.eventosweb.web;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Filtros del formulario de búsqueda, validados en el servidor (no solo en el navegador). */
public class FiltroBusqueda {

    @Size(max = 60)
    private String nombre;

    @Size(max = 60)
    private String videojuego;

    @Size(max = 40)
    private String categoria;

    @Size(max = 40)
    private String pais;

    @Size(max = 40)
    private String ciudad;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fecha;

    @Pattern(regexp = "^(PRESENCIAL|ONLINE)?$")
    private String modalidad;

    public boolean estaVacio() {
        return vacio(nombre) && vacio(videojuego) && vacio(categoria) && vacio(pais)
                && vacio(ciudad) && fecha == null && vacio(modalidad);
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getVideojuego() { return videojuego; }
    public void setVideojuego(String videojuego) { this.videojuego = videojuego; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getModalidad() { return modalidad; }
    public void setModalidad(String modalidad) { this.modalidad = modalidad; }
}
