-- Rename the original sample records without changing their IDs, so existing
-- reservations and assignments continue to reference the same spaces.
UPDATE espacios
SET nombre = 'AULA MÁXIMA TULIO RAMÍREZ ROJAS',
    ubicacion = 'UNICAMACHO SUR · CL. 25 #127-220, VÍA CALI-JAMUNDÍ',
    capacidad = 396,
    descripcion = 'Auditorio con proyección, sistema de sonido y acceso mediante rampas.'
WHERE id = '20000000-0000-0000-0000-000000000002';

UPDATE espacios
SET nombre = 'LABORATORIO DE SISTEMAS - SEDE NORTE',
    ubicacion = 'SEDE NORTE · AV. 6N #28N-102',
    descripcion = 'Laboratorio de sistemas en la instalación principal de la UNIAJC.'
WHERE id = '20000000-0000-0000-0000-000000000003';

UPDATE espacios
SET nombre = 'SALA MULTIMEDIAL - BIBLIOTECA JAIRO PANESSO TASCÓN',
    ubicacion = 'BIBLIOTECA CENTRAL · SEDE NORTE',
    descripcion = 'Espacio multimedial de la Biblioteca Universitaria Centro Cultural Jairo Panesso Tascón.'
WHERE id = '20000000-0000-0000-0000-000000000001';
