package com.aarondevs.luminis.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.jwt.enabled", havingValue = "true")
public class JwtService {
    private final SecretKey clave;
    private final long duracionMilisegundos;

    public JwtService(
            @Value("${app.jwt.secret}") String secreto,
            @Value("${app.jwt.expiration-hours}") long horasExpiracion) {
        if (secreto == null || secreto.length() < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        if (horasExpiracion <= 0) {
            throw new IllegalStateException("JWT_EXPIRATION_HOURS debe ser mayor que cero");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.duracionMilisegundos = Math.multiplyExact(horasExpiracion, 3_600_000L);
    }

    public String generarJwt(String subject, Map<String, Object> claims) {
        return generarJwt(subject, claims, UUID.randomUUID().toString());
    }

    public String generarJwt(String subject, Map<String, Object> claims, String jti) {
        Date expedido = new Date();
        return Jwts.builder().setClaims(claims).setSubject(subject).setId(jti)
                .setIssuedAt(expedido)
                .setExpiration(new Date(expedido.getTime() + duracionMilisegundos))
                .signWith(clave, SignatureAlgorithm.HS256).compact();
    }

    public Claims obtenerClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(clave).build()
                .parseClaimsJws(token).getBody();
    }
}
