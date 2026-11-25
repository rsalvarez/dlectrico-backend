package ar.gob.cba.instalaciones.seguridad.dto;

public record UsuarioAutenticado(
        Long usuarioId,
        Long empresaId,
        String nombreUsuario,
        String rol,
        String sessionTokenId
) {}