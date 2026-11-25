package ar.gob.cba.instalaciones.seguridad.repository;

import ar.gob.cba.instalaciones.seguridad.dto.SesionAuditDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class SesionUsuarioRepository {

    private final JdbcTemplate jdbc;

    public SesionUsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void cerrarSesionesAbiertasPorUsuario(Long usuarioId, Instant fin, String motivo) {
        jdbc.update(
                """
                UPDATE sesiones_usuario
                SET fin_sesion = ?, motivo_cierre = ?
                WHERE usuario_id = ? AND fin_sesion IS NULL
                """,
                Timestamp.from(fin), motivo, usuarioId
        );
    }

    public void registrarInicioSesion(
            String sessionTokenId,
            Long usuarioId,
            Long empresaId,
            Instant inicio,
            String ipOrigen,
            String userAgent
    ) {
        jdbc.update(
                """
                INSERT INTO sesiones_usuario
                    (session_token_id, usuario_id, empresa_id, inicio_sesion, ip_origen, user_agent)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                sessionTokenId, usuarioId, empresaId, Timestamp.from(inicio), ipOrigen, userAgent
        );
    }

    public int cerrarSesionPorTokenId(String sessionTokenId, Long usuarioId, Instant fin, String motivo) {
        return jdbc.update(
                """
                UPDATE sesiones_usuario
                SET fin_sesion = ?, motivo_cierre = ?
                WHERE session_token_id = ? AND usuario_id = ? AND fin_sesion IS NULL
                """,
                Timestamp.from(fin), motivo, sessionTokenId, usuarioId
        );
    }

    public boolean existeSesionActiva(String sessionTokenId) {
        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM sesiones_usuario
                WHERE session_token_id = ? AND fin_sesion IS NULL
                """,
                Integer.class,
                sessionTokenId
        );
        return count != null && count > 0;
    }

    public int cerrarSesionesExpiradas(long ttlMinutes, String motivo) {
        return jdbc.update(
                """
                UPDATE sesiones_usuario
                SET fin_sesion = inicio_sesion + (? * INTERVAL '1 minute'),
                    motivo_cierre = ?
                WHERE fin_sesion IS NULL
                  AND inicio_sesion + (? * INTERVAL '1 minute') <= NOW()
                """,
                ttlMinutes, motivo, ttlMinutes
        );
    }

    public List<SesionAuditDto> listarPorEmpresa(Long empresaId, int limite) {
        return jdbc.query(
                """
                SELECT s.id,
                       s.session_token_id,
                       s.usuario_id,
                       u.nombre_usuario,
                       (u.apellido || ', ' || u.nombre) AS nombre_completo,
                       s.empresa_id,
                       s.inicio_sesion,
                       s.fin_sesion,
                       s.duracion_segundos,
                       s.motivo_cierre,
                       s.ip_origen
                FROM sesiones_usuario s
                JOIN usuarios u ON u.id = s.usuario_id
                WHERE s.empresa_id = ?
                ORDER BY s.inicio_sesion DESC
                LIMIT ?
                """,
                (rs, rowNum) -> {
                    Timestamp finTs = rs.getTimestamp("fin_sesion");
                    long duracion = rs.getLong("duracion_segundos");
                    Long duracionObj = rs.wasNull() ? null : duracion;
                    return new SesionAuditDto(
                            rs.getLong("id"),
                            rs.getString("session_token_id"),
                            rs.getLong("usuario_id"),
                            rs.getString("nombre_usuario"),
                            rs.getString("nombre_completo"),
                            rs.getLong("empresa_id"),
                            rs.getTimestamp("inicio_sesion").toInstant(),
                            finTs != null ? finTs.toInstant() : null,
                            duracionObj,
                            rs.getString("motivo_cierre"),
                            rs.getString("ip_origen")
                    );
                },
                empresaId, limite
        );
    }
}