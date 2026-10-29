INSERT INTO roles (id, nombre) VALUES
    ('00000000-0000-0000-0000-000000000001', 'ADMINISTRATOR'),
    ('00000000-0000-0000-0000-000000000002', 'MANAGER'),
    ('00000000-0000-0000-0000-000000000003', 'USER'),
    ('00000000-0000-0000-0000-000000000004', 'VISITOR');

INSERT INTO tipos_espacio (id, nombre, descripcion) VALUES
    ('10000000-0000-0000-0000-000000000001', 'Sala De Reuniones', 'Espacio cerrado para trabajo colaborativo.'),
    ('10000000-0000-0000-0000-000000000002', 'Auditorio', 'Espacio amplio para presentaciones y eventos.'),
    ('10000000-0000-0000-0000-000000000003', 'Laboratorio De Cómputo', 'Espacio de enseñanza con estaciones de trabajo.');

INSERT INTO espacios (id, tipo_espacio_id, nombre, ubicacion, capacidad, descripcion, estado) VALUES
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Sala Multimedial - Biblioteca Jairo Panesso Tascón', 'Biblioteca Central · Sede Norte', 12, 'Espacio multimedial de la Biblioteca Universitaria Centro Cultural Jairo Panesso Tascón.', 'ACTIVE'),
    ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 'Aula Máxima Tulio Ramírez Rojas', 'Unicamacho Sur · Cl. 25 #127-220, Vía Cali-Jamundí', 396, 'Auditorio con proyección, sistema de sonido y acceso mediante rampas.', 'ACTIVE'),
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', 'Laboratorio De Sistemas - Sede Norte', 'Sede Norte · Av. 6N #28N-102', 30, 'Laboratorio de sistemas en la instalación principal de la UNIAJC.', 'ACTIVE');
