package ar.gob.cba.instalaciones.seguridad.dto;

public record ActualizarLogoRequest(
        String mimeType,
        String base64
) {}