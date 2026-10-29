-- Keep the existing records and relationships while replacing UUID catalog keys with compact numbers.
SET FOREIGN_KEY_CHECKS = 0;

CREATE TEMPORARY TABLE tmp_role_id_map (
    old_id CHAR(36) NOT NULL PRIMARY KEY,
    new_id BIGINT NOT NULL AUTO_INCREMENT UNIQUE
) ENGINE=MEMORY;

INSERT INTO tmp_role_id_map (old_id)
SELECT id
FROM roles
ORDER BY CASE nombre
    WHEN 'ADMINISTRATOR' THEN 1
    WHEN 'MANAGER' THEN 2
    WHEN 'USER' THEN 3
    WHEN 'VISITOR' THEN 4
    ELSE 5
END, nombre, id;

CREATE TEMPORARY TABLE tmp_space_type_id_map (
    old_id CHAR(36) NOT NULL PRIMARY KEY,
    new_id BIGINT NOT NULL AUTO_INCREMENT UNIQUE
) ENGINE=MEMORY;

INSERT INTO tmp_space_type_id_map (old_id)
SELECT id
FROM tipos_espacio
ORDER BY CASE id
    WHEN '10000000-0000-0000-0000-000000000001' THEN 1
    WHEN '10000000-0000-0000-0000-000000000002' THEN 2
    WHEN '10000000-0000-0000-0000-000000000003' THEN 3
    ELSE 4
END, id;

ALTER TABLE usuario_roles
    DROP FOREIGN KEY usuario_roles_usuario_fk;

ALTER TABLE usuario_roles
    DROP PRIMARY KEY,
    ADD COLUMN rol_id_nuevo BIGINT NOT NULL DEFAULT 0;

ALTER TABLE roles
    DROP PRIMARY KEY,
    ADD COLUMN id_nuevo BIGINT NOT NULL DEFAULT 0;

UPDATE roles r
JOIN tmp_role_id_map m ON m.old_id = r.id
SET r.id_nuevo = m.new_id;

UPDATE usuario_roles ur
JOIN tmp_role_id_map m ON m.old_id = ur.rol_id
SET ur.rol_id_nuevo = m.new_id;

ALTER TABLE roles
    DROP COLUMN id,
    CHANGE COLUMN id_nuevo id BIGINT NOT NULL,
    ADD PRIMARY KEY (id);

ALTER TABLE usuario_roles
    DROP COLUMN rol_id,
    CHANGE COLUMN rol_id_nuevo rol_id BIGINT NOT NULL,
    ADD PRIMARY KEY (usuario_id, rol_id),
    ADD CONSTRAINT usuario_roles_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    ADD CONSTRAINT usuario_roles_rol_fk FOREIGN KEY (rol_id) REFERENCES roles(id);

ALTER TABLE espacios
    DROP FOREIGN KEY espacios_tipo_fk,
    ADD COLUMN tipo_espacio_id_nuevo BIGINT NOT NULL DEFAULT 0;

ALTER TABLE tipos_espacio
    DROP PRIMARY KEY,
    ADD COLUMN id_nuevo BIGINT NOT NULL DEFAULT 0;

UPDATE tipos_espacio t
JOIN tmp_space_type_id_map m ON m.old_id = t.id
SET t.id_nuevo = m.new_id;

UPDATE espacios e
JOIN tmp_space_type_id_map m ON m.old_id = e.tipo_espacio_id
SET e.tipo_espacio_id_nuevo = m.new_id;

ALTER TABLE tipos_espacio
    ADD UNIQUE KEY tipos_espacio_id_nuevo_uq (id_nuevo);

ALTER TABLE tipos_espacio
    DROP COLUMN id,
    CHANGE COLUMN id_nuevo id BIGINT NOT NULL AUTO_INCREMENT,
    ADD PRIMARY KEY (id);

ALTER TABLE tipos_espacio
    DROP INDEX tipos_espacio_id_nuevo_uq;

ALTER TABLE espacios
    DROP COLUMN tipo_espacio_id,
    CHANGE COLUMN tipo_espacio_id_nuevo tipo_espacio_id BIGINT NOT NULL,
    ADD CONSTRAINT espacios_tipo_fk FOREIGN KEY (tipo_espacio_id) REFERENCES tipos_espacio(id);

DROP TEMPORARY TABLE tmp_role_id_map;
DROP TEMPORARY TABLE tmp_space_type_id_map;

SET FOREIGN_KEY_CHECKS = 1;
