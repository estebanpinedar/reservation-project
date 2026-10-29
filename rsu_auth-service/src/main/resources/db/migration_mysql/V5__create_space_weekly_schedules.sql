CREATE TABLE espacio_horarios (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    espacio_id BIGINT NOT NULL,
    dia_semana VARCHAR(12) NOT NULL,
    hora_apertura TIME NOT NULL,
    hora_cierre TIME NOT NULL,
    CONSTRAINT espacio_horarios_espacio_fk FOREIGN KEY (espacio_id) REFERENCES espacios(id),
    CONSTRAINT espacio_horarios_dia_uq UNIQUE (espacio_id, dia_semana),
    CONSTRAINT espacio_horarios_dia_check CHECK (dia_semana IN
        ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CONSTRAINT espacio_horarios_periodo_check CHECK (hora_apertura < hora_cierre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO espacio_horarios (espacio_id, dia_semana, hora_apertura, hora_cierre)
SELECT id, 'MONDAY', '07:00:00', '22:00:00' FROM espacios
UNION ALL SELECT id, 'TUESDAY', '07:00:00', '22:00:00' FROM espacios
UNION ALL SELECT id, 'WEDNESDAY', '07:00:00', '22:00:00' FROM espacios
UNION ALL SELECT id, 'THURSDAY', '07:00:00', '22:00:00' FROM espacios
UNION ALL SELECT id, 'FRIDAY', '07:00:00', '22:00:00' FROM espacios
UNION ALL SELECT id, 'SATURDAY', '08:00:00', '18:00:00' FROM espacios;
