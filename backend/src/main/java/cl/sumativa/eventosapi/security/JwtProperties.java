package cl.sumativa.eventosapi.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * El secreto se inyecta por variable de entorno (JWT_SECRET); no se deja escrito en el código.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expiracionMinutos, String emisor) { }
