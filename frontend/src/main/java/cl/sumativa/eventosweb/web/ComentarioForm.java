package cl.sumativa.eventosweb.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Formulario de comentario y valoración (validado en el servidor, además del navegador). */
public class ComentarioForm {

    @NotNull(message = "Selecciona una valoración de 1 a 5 estrellas.")
    @Min(value = 1, message = "La valoración mínima es 1 estrella.")
    @Max(value = 5, message = "La valoración máxima es 5 estrellas.")
    private Integer valoracion;

    @NotBlank(message = "Escribe un comentario.")
    @Size(max = 500, message = "El comentario admite máximo 500 caracteres.")
    private String texto;

    public Integer getValoracion() { return valoracion; }
    public void setValoracion(Integer valoracion) { this.valoracion = valoracion; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
}
