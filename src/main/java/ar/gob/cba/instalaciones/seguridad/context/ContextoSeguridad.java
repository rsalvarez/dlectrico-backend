package ar.gob.cba.instalaciones.seguridad.context;

import ar.gob.cba.instalaciones.seguridad.dto.UsuarioAutenticado;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ContextoSeguridad {

    public UsuarioAutenticado usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return usuario;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
    }

    public Long empresaIdActual() {
        return usuarioActual().empresaId();
    }

    public Long usuarioIdActual() {
        return usuarioActual().usuarioId();
    }
}