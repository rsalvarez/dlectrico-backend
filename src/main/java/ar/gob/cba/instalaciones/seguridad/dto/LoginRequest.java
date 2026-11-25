package ar.gob.cba.instalaciones.seguridad.dto;

public record LoginRequest(
        String nombreUsuario,
        String password
) {}