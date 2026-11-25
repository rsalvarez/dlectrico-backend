-- Reemplaza la unicidad por empresa por unicidad global de nombre_usuario
-- para permitir login mobile solo con usuario y contraseña.
ALTER TABLE usuarios
    DROP CONSTRAINT IF EXISTS uq_usuario_por_empresa;

CREATE UNIQUE INDEX IF NOT EXISTS uq_usuarios_nombre_usuario_lower
    ON usuarios (LOWER(nombre_usuario));