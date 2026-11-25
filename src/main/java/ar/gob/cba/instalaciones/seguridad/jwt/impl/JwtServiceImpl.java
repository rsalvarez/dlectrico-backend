package ar.gob.cba.instalaciones.seguridad.jwt.impl;

import ar.gob.cba.instalaciones.seguridad.dto.UsuarioAutenticado;
import ar.gob.cba.instalaciones.seguridad.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    private final SecretKey key;
    private final long ttlMinutes;

    public JwtServiceImpl(
            @Value("${app.security.jwt-secret}") String secret,
            @Value("${app.security.session-ttl-minutes:480}") long ttlMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMinutes = ttlMinutes;
    }
    @Override
    public long getTtlMinutes() {
        return ttlMinutes;
    }
    @Override
    public String generarToken(UsuarioAutenticado user, String dni, String nombreCompleto, Instant inicio, Instant expira) {
        return Jwts.builder()
                .id(user.sessionTokenId())
                .subject(String.valueOf(user.usuarioId()))
                .claim("empresaId", user.empresaId())
                .claim("username", user.nombreUsuario())
                .claim("rol", user.rol())
                .claim("dni", dni)
                .claim("nombreCompleto", nombreCompleto)
                .issuedAt(Date.from(inicio))
                .expiration(Date.from(expira))
                .signWith(key)
                .compact();
    }
    @Override
    public UsuarioAutenticado parsearToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long usuarioId = Long.valueOf(claims.getSubject());
        Long empresaId = claims.get("empresaId", Long.class);
        String username = claims.get("username", String.class);
        String rol = claims.get("rol", String.class);
        String jti = claims.getId();

        return new UsuarioAutenticado(usuarioId, empresaId, username, rol, jti);
    }
}