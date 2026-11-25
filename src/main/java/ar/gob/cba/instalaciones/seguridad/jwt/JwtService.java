package ar.gob.cba.instalaciones.seguridad.jwt;

import ar.gob.cba.instalaciones.seguridad.dto.UsuarioAutenticado;

import java.time.Instant;

public interface JwtService {
    long getTtlMinutes();

    String generarToken(UsuarioAutenticado user, String dni, String nombreCompleto, Instant inicio, Instant expira);

    UsuarioAutenticado parsearToken(String token);
}
