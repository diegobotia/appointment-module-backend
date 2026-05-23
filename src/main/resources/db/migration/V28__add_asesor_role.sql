-- Rol Asesor (call center IPS): operación de citas sin panel /api/v1/admin/**
INSERT INTO core.roles (nombre, descripcion)
VALUES ('Asesor', 'Call center: gestión del ciclo de vida de citas')
ON CONFLICT (nombre) DO UPDATE SET descripcion = EXCLUDED.descripcion;
