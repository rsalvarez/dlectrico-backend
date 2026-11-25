package ar.gob.cba.instalaciones.seguridad.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UsuarioEmpresaRepository {

    public record UsuarioLoginRow(
            Long usuarioId,
            Long empresaId,
            String empresaCodigo,
            String empresaCuit,
            String empresaRazonSocial,
            String empresaPlanMembresia,
            String empresaLogoMimeType,
            String empresaLogoBase64,
            String dni,
            String apellido,
            String nombre,
            String nombreUsuario,
            String passwordHash,
            String rol,
            boolean usuarioActivo,
            boolean empresaActiva
    ) {}

    private final JdbcTemplate jdbc;

    public UsuarioEmpresaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UsuarioLoginRow> buscarPorNombreUsuario(String nombreUsuario) {
        String sql = """
                SELECT u.id AS usuario_id,
                       u.empresa_id,
                       e.codigo AS empresa_codigo,
                       e.cuit AS empresa_cuit,
                       e.razon_social AS empresa_razon_social,
                       e.plan_membresia AS empresa_plan_membresia,
                       e.logo_mime_type AS empresa_logo_mime_type,
                       e.logo_base64 AS empresa_logo_base64,
                       u.dni,
                       u.apellido,
                       u.nombre,
                       u.nombre_usuario,
                       u.password_hash,
                       u.rol,
                       u.activo AS usuario_activo,
                       e.activa AS empresa_activa
                FROM usuarios u
                JOIN empresas e ON e.id = u.empresa_id
                WHERE LOWER(u.nombre_usuario) = LOWER(?)
                """;

        return jdbc.query(
                sql,
                (rs, rowNum) -> new UsuarioLoginRow(
                        rs.getLong("usuario_id"),
                        rs.getLong("empresa_id"),
                        rs.getString("empresa_codigo"),
                        rs.getString("empresa_cuit"),
                        rs.getString("empresa_razon_social"),
                        rs.getString("empresa_plan_membresia"),
                        rs.getString("empresa_logo_mime_type"),
                        rs.getString("empresa_logo_base64"),
                        rs.getString("dni"),
                        rs.getString("apellido"),
                        rs.getString("nombre"),
                        rs.getString("nombre_usuario"),
                        rs.getString("password_hash"),
                        rs.getString("rol"),
                        rs.getBoolean("usuario_activo"),
                        rs.getBoolean("empresa_activa")
                ),
                nombreUsuario
        ).stream().findFirst();
    }

    public void actualizarLogoEmpresa(Long empresaId, String mimeType, String base64, String actualizadoPor) {
        jdbc.update(
                """
                UPDATE empresas
                SET logo_mime_type = ?,
                    logo_base64 = ?,
                    actualizado_en = NOW(),
                    actualizado_por = ?
                WHERE id = ?
                """,
                mimeType, base64, actualizadoPor, empresaId
        );
    }
}