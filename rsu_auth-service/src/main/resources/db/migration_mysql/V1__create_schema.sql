CREATE TABLE roles (
    id CHAR(36) NOT NULL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE usuarios (
    id CHAR(36) NOT NULL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT usuarios_estado_check CHECK (estado IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE usuario_roles (
    usuario_id CHAR(36) NOT NULL,
    rol_id CHAR(36) NOT NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT usuario_roles_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT usuario_roles_rol_fk FOREIGN KEY (rol_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tipos_espacio (
    id CHAR(36) NOT NULL PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL UNIQUE,
    descripcion VARCHAR(250),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE espacios (
    id CHAR(36) NOT NULL PRIMARY KEY,
    tipo_espacio_id CHAR(36) NOT NULL,
    nombre VARCHAR(120) NOT NULL UNIQUE,
    ubicacion VARCHAR(160) NOT NULL,
    capacidad INTEGER NOT NULL,
    descripcion VARCHAR(500),
    estado VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT espacios_tipo_fk FOREIGN KEY (tipo_espacio_id) REFERENCES tipos_espacio(id),
    CONSTRAINT espacios_capacidad_check CHECK (capacidad > 0),
    CONSTRAINT espacios_estado_check CHECK (estado IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE espacio_responsables (
    espacio_id CHAR(36) NOT NULL,
    usuario_id CHAR(36) NOT NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (espacio_id, usuario_id),
    CONSTRAINT espacio_responsables_espacio_fk FOREIGN KEY (espacio_id) REFERENCES espacios(id),
    CONSTRAINT espacio_responsables_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reservas (
    id CHAR(36) NOT NULL PRIMARY KEY,
    usuario_creador_id CHAR(36) NOT NULL,
    espacio_id CHAR(36) NOT NULL,
    hora_inicio DATETIME(6) NOT NULL,
    hora_fin DATETIME(6) NOT NULL,
    motivo VARCHAR(250) NOT NULL,
    asistentes INTEGER NOT NULL,
    estado VARCHAR(20) NOT NULL,
    cancelled_at DATETIME(6),
    cancellation_reason VARCHAR(250),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT reservas_usuario_fk FOREIGN KEY (usuario_creador_id) REFERENCES usuarios(id),
    CONSTRAINT reservas_espacio_fk FOREIGN KEY (espacio_id) REFERENCES espacios(id),
    CONSTRAINT reservas_horas_check CHECK (hora_inicio < hora_fin),
    CONSTRAINT reservas_asistentes_check CHECK (asistentes > 0),
    CONSTRAINT reservas_estado_check CHECK (estado IN ('ACTIVE', 'CANCELLED', 'FINISHED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX reservas_espacio_periodo_idx ON reservas (espacio_id, hora_inicio, hora_fin);

CREATE TABLE bloqueos (
    id CHAR(36) NOT NULL PRIMARY KEY,
    espacio_id CHAR(36) NOT NULL,
    usuario_creador_id CHAR(36) NOT NULL,
    hora_inicio DATETIME(6) NOT NULL,
    hora_fin DATETIME(6) NOT NULL,
    motivo VARCHAR(250) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    cancelled_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT bloqueos_espacio_fk FOREIGN KEY (espacio_id) REFERENCES espacios(id),
    CONSTRAINT bloqueos_usuario_fk FOREIGN KEY (usuario_creador_id) REFERENCES usuarios(id),
    CONSTRAINT bloqueos_horas_check CHECK (hora_inicio < hora_fin),
    CONSTRAINT bloqueos_estado_check CHECK (estado IN ('ACTIVE', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX bloqueos_espacio_periodo_idx ON bloqueos (espacio_id, hora_inicio, hora_fin);

CREATE TABLE auditorias (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    actor_id CHAR(36),
    accion VARCHAR(100) NOT NULL,
    entidad VARCHAR(80) NOT NULL,
    entidad_id CHAR(36),
    detalle VARCHAR(1000),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT auditorias_actor_fk FOREIGN KEY (actor_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
