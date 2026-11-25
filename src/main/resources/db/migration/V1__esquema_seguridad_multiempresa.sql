CREATE TABLE empresas (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    razon_social VARCHAR(180) NOT NULL,
    cuit VARCHAR(20) NOT NULL UNIQUE,
    plan_membresia VARCHAR(40) NOT NULL DEFAULT 'BASICO',
    logo_mime_type VARCHAR(60),
    logo_base64 TEXT,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por VARCHAR(100) NOT NULL DEFAULT 'SISTEMA',
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_por VARCHAR(100) NOT NULL DEFAULT 'SISTEMA'
);

CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    dni VARCHAR(25) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    nombre_usuario VARCHAR(80) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(40) NOT NULL DEFAULT 'OPERADOR',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por VARCHAR(100) NOT NULL DEFAULT 'SISTEMA',
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_por VARCHAR(100) NOT NULL DEFAULT 'SISTEMA',
    CONSTRAINT uq_usuario_por_empresa UNIQUE (empresa_id, nombre_usuario),
    CONSTRAINT uq_dni_por_empresa UNIQUE (empresa_id, dni)
);

CREATE INDEX idx_usuarios_empresa_activo ON usuarios(empresa_id, activo);

CREATE TABLE sesiones_usuario (
    id BIGSERIAL PRIMARY KEY,
    session_token_id VARCHAR(64) NOT NULL UNIQUE,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    empresa_id BIGINT NOT NULL REFERENCES empresas(id),
    inicio_sesion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fin_sesion TIMESTAMPTZ,
    duracion_segundos BIGINT GENERATED ALWAYS AS (
        CASE
            WHEN fin_sesion IS NOT NULL
                THEN GREATEST(0, EXTRACT(EPOCH FROM (fin_sesion - inicio_sesion))::BIGINT)
            ELSE NULL
        END
    ) STORED,
    motivo_cierre VARCHAR(30),
    ip_origen VARCHAR(64),
    user_agent VARCHAR(255)
);

CREATE INDEX idx_sesiones_usuario_inicio ON sesiones_usuario(usuario_id, inicio_sesion DESC);
CREATE INDEX idx_sesiones_empresa_inicio ON sesiones_usuario(empresa_id, inicio_sesion DESC);
CREATE INDEX idx_sesiones_abiertas ON sesiones_usuario(fin_sesion) WHERE fin_sesion IS NULL;