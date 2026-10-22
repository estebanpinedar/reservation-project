# Arquitectura inicial

## Alcance

El sistema gestiona espacios institucionales, disponibilidad, reservas, cancelaciones, bloqueos, roles y auditoría. El control físico de acceso, la facturación y la integración obligatoria con calendarios externos quedan fuera de alcance.

## Módulos previstos

| Módulo | Responsabilidad |
| --- | --- |
| authentication | Inicio de sesión, token y protección de rutas. |
| users | Usuarios, roles y asignaciones. |
| spaces | Tipos de espacio, catálogo y responsables. |
| availability | Agenda y disponibilidad derivada. |
| reservations | Creación, consulta, cancelación y conflictos. |
| blocks | Bloqueos autorizados. |
| administration | Auditoría, agenda global y reportes. |

## Reglas principales

- Un usuario puede tener uno o varios roles; la relación se conserva en `usuario_roles`.
- La asignación de espacios a responsables se conserva en `espacio_responsables` y no sustituye sus otros roles.
- Los nombres de espacios y tipos se almacenan en mayúsculas.
- Solo un espacio `ACTIVE` recibe nuevas reservas.
- El inicio debe preceder al fin y los asistentes no pueden superar la capacidad.
- Dos reservas activas del mismo espacio no pueden solaparse.
- Un bloqueo activo impide reservas en su período.
- Las cancelaciones conservan el historial.
- La disponibilidad se calcula desde reservas y bloqueos activos; no se duplica en una tabla.
- El perfil local usa H2 en archivo; el perfil PostgreSQL se mantiene para otros entornos.
