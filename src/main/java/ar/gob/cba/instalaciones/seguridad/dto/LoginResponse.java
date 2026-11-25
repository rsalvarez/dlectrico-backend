package ar.gob.cba.instalaciones.seguridad.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

public record LoginResponse(
        String token,
        String sessionTokenId,
        @JsonIgnore
        Long usuarioId,
        @JsonIgnore
        Long empresaId,
        String empresaRazonSocial,
        String empresaLogoDataUrl,
        @JsonIgnore
        String nombreUsuario,
        @JsonIgnore
        String nombreCompleto,
        @JsonIgnore
        String rol,
        Instant inicioSesion,
        Instant expiraEn
) {}