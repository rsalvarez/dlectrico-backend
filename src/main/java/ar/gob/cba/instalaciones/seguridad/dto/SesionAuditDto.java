package ar.gob.cba.instalaciones.seguridad.dto;

import java.time.Instant;

public record SesionAuditDto(
        Long id,
        String sessionTokenId,
        Long usuarioId,
        String nombreUsuario,
        String nombreCompleto,
        Long empresaId,
        Instant inicioSesion,
        Instant finSesion,
        Long duracionSegundos,
        String motivoCierre,
        String ipOrigen
) {}