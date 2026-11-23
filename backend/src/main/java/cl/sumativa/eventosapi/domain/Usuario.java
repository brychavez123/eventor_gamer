package cl.sumativa.eventosapi.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    /** Hash BCrypt, nunca la contraseña en texto plano. */
    @Column(name = "password_hash")
    private String passwordHash;

    private String nombre;
    private String rol;
    private Boolean activo;

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getNombre() { return nombre; }
    public String getRol() { return rol; }
    public Boolean getActivo() { return activo; }
}
