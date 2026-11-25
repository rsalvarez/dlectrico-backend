package ar.gob.cba.instalaciones.seguridad.service.impl;

import ar.gob.cba.instalaciones.seguridad.dto.ActualizarLogoRequest;
import ar.gob.cba.instalaciones.seguridad.dto.LoginRequest;
import ar.gob.cba.instalaciones.seguridad.dto.LoginResponse;
import ar.gob.cba.instalaciones.seguridad.dto.SesionAuditDto;
import ar.gob.cba.instalaciones.seguridad.dto.UsuarioAutenticado;
import ar.gob.cba.instalaciones.seguridad.jwt.JwtService;
import ar.gob.cba.instalaciones.seguridad.repository.SesionUsuarioRepository;
import ar.gob.cba.instalaciones.seguridad.repository.UsuarioEmpresaRepository;
import ar.gob.cba.instalaciones.seguridad.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Set<String> MIME_PERMITIDOS = Set.of(
            "image/png", "image/jpeg", "image/webp", "image/svg+xml"
    );

    private final UsuarioEmpresaRepository usuarioRepo;
    private final SesionUsuarioRepository sesionRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final int maxLogoBytes;

    public AuthServiceImpl(
            UsuarioEmpresaRepository usuarioRepo,
            SesionUsuarioRepository sesionRepo,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${app.security.max-logo-bytes:307200}") int maxLogoBytes
    ) {
        this.usuarioRepo = usuarioRepo;
        this.sesionRepo = sesionRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.maxLogoBytes = maxLogoBytes;
    }

    @Transactional
    public LoginResponse login(LoginRequest req, String ipOrigen, String userAgent) {
        if (req == null
                || req.nombreUsuario() == null || req.nombreUsuario().isBlank()
                || req.password() == null || req.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar nombreUsuario y password");
        }

        var row = usuarioRepo.buscarPorNombreUsuario(req.nombreUsuario().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        if (!row.empresaActiva() || !row.usuarioActivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Empresa o usuario inactivo");
        }

        if (!passwordEncoder.matches(req.password(), row.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        Instant ahora = Instant.now();
        Instant expira = ahora.plus(jwtService.getTtlMinutes(), ChronoUnit.MINUTES);
        String sessionTokenId = UUID.randomUUID().toString();

        sesionRepo.cerrarSesionesAbiertasPorUsuario(row.usuarioId(), ahora, "NUEVO_LOGIN");
        sesionRepo.registrarInicioSesion(sessionTokenId, row.usuarioId(), row.empresaId(), ahora, ipOrigen, userAgent);

        UsuarioAutenticado authUser = new UsuarioAutenticado(
                row.usuarioId(),
                row.empresaId(),
                row.nombreUsuario(),
                row.rol(),
                sessionTokenId
        );

        String nombreCompleto = row.apellido() + ", " + row.nombre();
        String token = jwtService.generarToken(authUser, row.dni(), nombreCompleto, ahora, expira);
        String logoDataUrl = construirDataUrl(row.empresaLogoMimeType(), row.empresaLogoBase64());

        return new LoginResponse(
                token,
                sessionTokenId,
                row.usuarioId(),
                row.empresaId(),
                row.empresaRazonSocial(),
                logoDataUrl,
                row.nombreUsuario(),
                nombreCompleto,
                row.rol(),
                ahora,
                expira
        );
    }

    @Transactional
    @Override
    public void actualizarLogoEmpresa(UsuarioAutenticado usuario, ActualizarLogoRequest req) {
        if (req == null || req.base64() == null || req.base64().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El logo en base64 es obligatorio");
        }
        String mime = (req.mimeType() == null || req.mimeType().isBlank()) ? "image/png" : req.mimeType().trim().toLowerCase();
        if (!MIME_PERMITIDOS.contains(mime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de logo no soportado");
        }

        String rawBase64 = req.base64().contains(",")
                ? req.base64().substring(req.base64().indexOf(',') + 1)
                : req.base64().trim();

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(rawBase64);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Base64 inválido");
        }

        if (decoded.length > maxLogoBytes) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "El logo supera el tamaño máximo permitido para frontend (" + maxLogoBytes + " bytes)"
            );
        }

        usuarioRepo.actualizarLogoEmpresa(usuario.empresaId(), mime, rawBase64, usuario.nombreUsuario());
    }

    private String construirDataUrl(String mimeType, String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        if (base64.startsWith("data:")) {
            return base64;
        }
        String mime = (mimeType == null || mimeType.isBlank()) ? "image/png" : mimeType;
        return "data:" + mime + ";base64," + base64;
    }
    @Transactional
    @Override
    public void logout(UsuarioAutenticado usuario) {
        sesionRepo.cerrarSesionPorTokenId(usuario.sessionTokenId(), usuario.usuarioId(), Instant.now(), "LOGOUT");
    }

    @Override
    public List<SesionAuditDto> historialEmpresa(Long empresaId, int limite) {
        return sesionRepo.listarPorEmpresa(empresaId, Math.min(Math.max(limite, 1), 200));
    }

    @Scheduled(fixedDelayString = "${app.security.cleanup-interval-ms:300000}")
    @Transactional
    public void cerrarSesionesExpiradas() {
        sesionRepo.cerrarSesionesExpiradas(jwtService.getTtlMinutes(), "EXPIRADO");
    }
}