INSERT INTO roles (id, nombre) VALUES
    ('00000000-0000-0000-0000-000000000001', 'ADMINISTRATOR'),
    ('00000000-0000-0000-0000-000000000002', 'MANAGER'),
    ('00000000-0000-0000-0000-000000000003', 'USER'),
    ('00000000-0000-0000-0000-000000000004', 'VISITOR');

INSERT INTO tipos_espacio (id, nombre, descripcion) VALUES
    ('10000000-0000-0000-0000-000000000001', 'Sala de reuniones', 'Espacio cerrado para trabajo colaborativo.'),
    ('10000000-0000-0000-0000-000000000002', 'Auditorio', 'Espacio amplio para presentaciones y eventos.'),
    ('10000000-0000-0000-0000-000000000003', 'Laboratorio de cómputo', 'Espacio de enseñanza con estaciones de trabajo.');

INSERT INTO espacios (id, tipo_espacio_id, nombre, ubicacion, capacidad, descripcion, estado) VALUES
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Sala Orión', 'Edificio A, piso 2', 12, 'Videoconferencia y pizarra.', 'ACTIVE'),
    ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'Auditorio Aurora', 'Edificio B, piso 1', 120, 'Proyección y escenario.', 'ACTIVE'),
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', 'Laboratorio Nova', 'Edificio C, piso 3', 30, 'Treinta estaciones de trabajo.', 'ACTIVE');
