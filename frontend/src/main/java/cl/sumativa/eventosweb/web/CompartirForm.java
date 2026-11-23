package cl.sumativa.eventosweb.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Formulario para compartir: el destino solo puede ser uno de la lista cerrada. */
public class CompartirForm {

    @NotBlank(message = "Selecciona dónde compartir.")
    @Pattern(regexp = "^(SITIO|FACEBOOK|X|WHATSAPP)$", message = "Destino no válido.")
    private String destino;

    @Size(max = 280, message = "El mensaje admite máximo 280 caracteres.")
    private String mensaje;

    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
