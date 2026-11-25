-- Logo SVG mínimo (tamaño liviano para navbar/header del frontend)
INSERT INTO empresas (
    codigo, razon_social, cuit, plan_membresia, logo_mime_type, logo_base64, activa, creado_por, actualizado_por
)
VALUES (
    'EMP-001',
    'Instalaciones Eléctricas Córdoba SRL',
    '30-71234567-9',
    'PRO',
    'image/svg+xml',
    'PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjAiIGhlaWdodD0iMzYiIHZpZXdCb3g9IjAgMCAxMjAgMzYiPjxyZWN0IHdpZHRoPSIxMjAiIGhlaWdodD0iMzYiIHJ4PSI2IiBmaWxsPSIjMGYxNzJhIi8+PHRleHQgeD0iMTQiIHk9IjIzIiBmaWxsPSIjZmZmZmZmIiBmb250LWZhbWlseT0iQXJpYWwsIHNhbnMtc2VyaWYiIGZvbnQtc2l6ZT0iMTQiIGZvbnQtd2VpZ2h0PSJib2xkIj5JRUNfQ0JBPC90ZXh0Pjwvc3ZnPg==',
    TRUE,
    'FLYWAY_SEED',
    'FLYWAY_SEED'
)
ON CONFLICT (codigo) DO NOTHING;

-- Password inicial: "Admin1234!" (BCrypt cost 10)
INSERT INTO usuarios (
    empresa_id, dni, apellido, nombre, nombre_usuario, password_hash, rol, activo, creado_por, actualizado_por
)
SELECT
    e.id,
    '20123456',
    'González',
    'Martín',
    'mgonzalez',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN_EMPRESA',
    TRUE,
    'FLYWAY_SEED',
    'FLYWAY_SEED'
FROM empresas e
WHERE e.codigo = 'EMP-001'
ON CONFLICT (empresa_id, nombre_usuario) DO NOTHING;