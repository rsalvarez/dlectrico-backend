package ar.gob.cba.instalaciones.seguridad.service;

import ar.gob.cba.instalaciones.seguridad.dto.ActualizarLogoRequest;
import ar.gob.cba.instalaciones.seguridad.dto.LoginRequest;
import ar.gob.cba.instalaciones.seguridad.dto.LoginResponse;
import ar.gob.cba.instalaciones.seguridad.dto.SesionAuditDto;
import ar.gob.cba.instalaciones.seguridad.dto.UsuarioAutenticado;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AuthService {
    @Transactional
    LoginResponse login(LoginRequest req, String ipOrigen, String userAgent);

    @Transactional
    void actualizarLogoEmpresa(UsuarioAutenticado usuario, ActualizarLogoRequest req);

    @Transactional
    void logout(UsuarioAutenticado usuario);

    List<SesionAuditDto> historialEmpresa(Long empresaId, int limite);
}
