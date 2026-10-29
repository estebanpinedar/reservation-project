-- Convert UUID entity keys to compact numeric IDs while preserving existing rows and links.
SET FOREIGN_KEY_CHECKS = 0;

CREATE TEMPORARY TABLE tmp_user_id_map (
    old_id CHAR(36) NOT NULL PRIMARY KEY,
    new_id BIGINT NOT NULL AUTO_INCREMENT UNIQUE
) ENGINE=MEMORY;
INSERT INTO tmp_user_id_map (old_id) SELECT id FROM usuarios ORDER BY id;

CREATE TEMPORARY TABLE tmp_space_id_map (
    old_id CHAR(36) NOT NULL PRIMARY KEY,
    new_id BIGINT NOT NULL AUTO_INCREMENT UNIQUE
) ENGINE=MEMORY;
INSERT INTO tmp_space_id_map (old_id) SELECT id FROM espacios ORDER BY id;

CREATE TEMPORARY TABLE tmp_reservation_id_map (
    old_id CHAR(36) NOT NULL PRIMARY KEY,
    new_id BIGINT NOT NULL AUTO_INCREMENT UNIQUE
) ENGINE=MEMORY;
INSERT INTO tmp_reservation_id_map (old_id) SELECT id FROM reservas ORDER BY id;

CREATE TEMPORARY TABLE tmp_block_id_map (
    old_id CHAR(36) NOT NULL PRIMARY KEY,
    new_id BIGINT NOT NULL AUTO_INCREMENT UNIQUE
) ENGINE=MEMORY;
INSERT INTO tmp_block_id_map (old_id) SELECT id FROM bloqueos ORDER BY id;

ALTER TABLE usuario_roles
    DROP FOREIGN KEY usuario_roles_usuario_fk,
    DROP FOREIGN KEY usuario_roles_rol_fk,
    DROP PRIMARY KEY;
ALTER TABLE espacio_responsables
    DROP FOREIGN KEY espacio_responsables_espacio_fk,
    DROP FOREIGN KEY espacio_responsables_usuario_fk,
    DROP PRIMARY KEY;
ALTER TABLE reservas
    DROP FOREIGN KEY reservas_usuario_fk,
    DROP FOREIGN KEY reservas_espacio_fk;
ALTER TABLE bloqueos
    DROP FOREIGN KEY bloqueos_espacio_fk,
    DROP FOREIGN KEY bloqueos_usuario_fk;
ALTER TABLE auditorias DROP FOREIGN KEY auditorias_actor_fk;

ALTER TABLE usuarios ADD COLUMN id_nuevo BIGINT NULL;
ALTER TABLE espacios ADD COLUMN id_nuevo BIGINT NULL;
ALTER TABLE reservas ADD COLUMN id_nuevo BIGINT NULL;
ALTER TABLE bloqueos ADD COLUMN id_nuevo BIGINT NULL;
ALTER TABLE usuario_roles ADD COLUMN usuario_id_nuevo BIGINT NULL;
ALTER TABLE espacio_responsables
    ADD COLUMN espacio_id_nuevo BIGINT NULL,
    ADD COLUMN usuario_id_nuevo BIGINT NULL;
ALTER TABLE reservas
    ADD COLUMN usuario_creador_id_nuevo BIGINT NULL,
    ADD COLUMN espacio_id_nuevo BIGINT NULL;
ALTER TABLE bloqueos
    ADD COLUMN espacio_id_nuevo BIGINT NULL,
    ADD COLUMN usuario_creador_id_nuevo BIGINT NULL;
ALTER TABLE auditorias
    ADD COLUMN actor_id_nuevo BIGINT NULL,
    ADD COLUMN entidad_id_nuevo BIGINT NULL;

UPDATE usuarios u JOIN tmp_user_id_map m ON m.old_id = u.id SET u.id_nuevo = m.new_id;
UPDATE espacios e JOIN tmp_space_id_map m ON m.old_id = e.id SET e.id_nuevo = m.new_id;
UPDATE reservas r JOIN tmp_reservation_id_map m ON m.old_id = r.id SET r.id_nuevo = m.new_id;
UPDATE bloqueos b JOIN tmp_block_id_map m ON m.old_id = b.id SET b.id_nuevo = m.new_id;
UPDATE usuario_roles ur JOIN tmp_user_id_map m ON m.old_id = ur.usuario_id
    SET ur.usuario_id_nuevo = m.new_id;
UPDATE espacio_responsables er
    JOIN tmp_space_id_map sm ON sm.old_id = er.espacio_id
    JOIN tmp_user_id_map um ON um.old_id = er.usuario_id
    SET er.espacio_id_nuevo = sm.new_id, er.usuario_id_nuevo = um.new_id;
UPDATE reservas r
    JOIN tmp_user_id_map um ON um.old_id = r.usuario_creador_id
    JOIN tmp_space_id_map sm ON sm.old_id = r.espacio_id
    SET r.usuario_creador_id_nuevo = um.new_id, r.espacio_id_nuevo = sm.new_id;
UPDATE bloqueos b
    JOIN tmp_space_id_map sm ON sm.old_id = b.espacio_id
    JOIN tmp_user_id_map um ON um.old_id = b.usuario_creador_id
    SET b.espacio_id_nuevo = sm.new_id, b.usuario_creador_id_nuevo = um.new_id;
UPDATE auditorias a JOIN tmp_user_id_map m ON m.old_id = a.actor_id
    SET a.actor_id_nuevo = m.new_id;
UPDATE auditorias a JOIN tmp_user_id_map m ON a.entidad = 'USER' AND m.old_id = a.entidad_id
    SET a.entidad_id_nuevo = m.new_id;
UPDATE auditorias a JOIN tmp_space_id_map m ON a.entidad = 'SPACE' AND m.old_id = a.entidad_id
    SET a.entidad_id_nuevo = m.new_id;
UPDATE auditorias a JOIN tmp_reservation_id_map m ON a.entidad = 'RESERVATION' AND m.old_id = a.entidad_id
    SET a.entidad_id_nuevo = m.new_id;
UPDATE auditorias a JOIN tmp_block_id_map m ON a.entidad = 'BLOCK' AND m.old_id = a.entidad_id
    SET a.entidad_id_nuevo = m.new_id;

ALTER TABLE usuarios
    DROP PRIMARY KEY,
    DROP COLUMN id,
    CHANGE COLUMN id_nuevo id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (id);
ALTER TABLE espacios
    DROP PRIMARY KEY,
    DROP COLUMN id,
    CHANGE COLUMN id_nuevo id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (id);
ALTER TABLE reservas
    DROP PRIMARY KEY,
    DROP COLUMN id,
    CHANGE COLUMN id_nuevo id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (id);
ALTER TABLE bloqueos
    DROP PRIMARY KEY,
    DROP COLUMN id,
    CHANGE COLUMN id_nuevo id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (id);

ALTER TABLE usuario_roles
    DROP COLUMN usuario_id,
    CHANGE COLUMN usuario_id_nuevo usuario_id BIGINT NOT NULL,
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST,
    ADD UNIQUE KEY usuario_roles_usuario_rol_uq (usuario_id, rol_id);
ALTER TABLE espacio_responsables
    DROP COLUMN espacio_id,
    DROP COLUMN usuario_id,
    CHANGE COLUMN espacio_id_nuevo espacio_id BIGINT NOT NULL,
    CHANGE COLUMN usuario_id_nuevo usuario_id BIGINT NOT NULL,
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST,
    ADD UNIQUE KEY espacio_responsables_espacio_usuario_uq (espacio_id, usuario_id);
ALTER TABLE reservas
    DROP COLUMN usuario_creador_id,
    DROP COLUMN espacio_id,
    CHANGE COLUMN usuario_creador_id_nuevo usuario_creador_id BIGINT NOT NULL,
    CHANGE COLUMN espacio_id_nuevo espacio_id BIGINT NOT NULL;
ALTER TABLE bloqueos
    DROP COLUMN espacio_id,
    DROP COLUMN usuario_creador_id,
    CHANGE COLUMN espacio_id_nuevo espacio_id BIGINT NOT NULL,
    CHANGE COLUMN usuario_creador_id_nuevo usuario_creador_id BIGINT NOT NULL;
ALTER TABLE auditorias
    DROP COLUMN actor_id,
    DROP COLUMN entidad_id,
    CHANGE COLUMN actor_id_nuevo actor_id BIGINT NULL,
    CHANGE COLUMN entidad_id_nuevo entidad_id BIGINT NULL;

ALTER TABLE roles MODIFY COLUMN id BIGINT NOT NULL FIRST;
ALTER TABLE tipos_espacio MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST;

ALTER TABLE usuario_roles
    ADD CONSTRAINT usuario_roles_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    ADD CONSTRAINT usuario_roles_rol_fk FOREIGN KEY (rol_id) REFERENCES roles(id);
ALTER TABLE espacio_responsables
    ADD CONSTRAINT espacio_responsables_espacio_fk FOREIGN KEY (espacio_id) REFERENCES espacios(id),
    ADD CONSTRAINT espacio_responsables_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuarios(id);
ALTER TABLE reservas
    ADD CONSTRAINT reservas_usuario_fk FOREIGN KEY (usuario_creador_id) REFERENCES usuarios(id),
    ADD CONSTRAINT reservas_espacio_fk FOREIGN KEY (espacio_id) REFERENCES espacios(id);
ALTER TABLE bloqueos
    ADD CONSTRAINT bloqueos_espacio_fk FOREIGN KEY (espacio_id) REFERENCES espacios(id),
    ADD CONSTRAINT bloqueos_usuario_fk FOREIGN KEY (usuario_creador_id) REFERENCES usuarios(id);
ALTER TABLE auditorias ADD CONSTRAINT auditorias_actor_fk FOREIGN KEY (actor_id) REFERENCES usuarios(id);

DROP TEMPORARY TABLE tmp_user_id_map;
DROP TEMPORARY TABLE tmp_space_id_map;
DROP TEMPORARY TABLE tmp_reservation_id_map;
DROP TEMPORARY TABLE tmp_block_id_map;
SET FOREIGN_KEY_CHECKS = 1;
