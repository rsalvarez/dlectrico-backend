package ar.gob.cba.instalaciones.seguridad.api;

import ar.gob.cba.instalaciones.seguridad.dto.ActualizarLogoRequest;
import ar.gob.cba.instalaciones.seguridad.dto.LoginRequest;
import ar.gob.cba.instalaciones.seguridad.dto.LoginResponse;
import ar.gob.cba.instalaciones.seguridad.dto.SesionAuditDto;
import ar.gob.cba.instalaciones.seguridad.dto.UsuarioAutenticado;
import ar.gob.cba.instalaciones.seguridad.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        String ua = request.getHeader("User-Agent");
        LoginResponse resp = authService.login(req, ip, ua);
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + resp.token())
                .body(resp);
    }

    @PutMapping("/empresa/logo")
    public ResponseEntity<Void> actualizarLogo(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestBody ActualizarLogoRequest req
    ) {
        authService.actualizarLogoEmpresa(usuario, req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        authService.logout(usuario);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sesiones")
    public ResponseEntity<List<SesionAuditDto>> sesiones(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "50") int limite
    ) {
        return ResponseEntity.ok(authService.historialEmpresa(usuario.empresaId(), limite));
    }
}