# Servicio de autenticación y reservas de espacios

Backend del sistema de reserva de espacios institucionales. La interfaz estática se mantiene en el proyecto frontend separado.

## Incluye

- Backend Java 21 y Spring Boot con punto de salud y autenticación JWT.
- Esquema MariaDB versionado con Flyway para el backend; XAMPP/phpMyAdmin es la base de datos local.
- Datos iniciales de catálogo.
- Notas de arquitectura y backlog técnico.

## Ejecutar con MySQL/MariaDB de XAMPP

Inicia MySQL desde el panel de XAMPP y crea la base `space_reservation` desde phpMyAdmin si todavía no existe.
La configuración predeterminada del backend conecta a `localhost:3306`, base `space_reservation`, usuario
`root` y contraseña vacía. Si tus datos son distintos, define `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` y
`DB_PASSWORD` en la ejecución de Eclipse.

No es necesario activar un perfil para usar MySQL. Al iniciar `RsuAuthServiceApplication`, Flyway crea las
tablas pendientes y carga el catálogo. Consulta `GET http://localhost:8080/api/v1/health` para verificar el
arranque.

## Autenticación local

Registra una cuenta local con `POST /api/v1/auth/register` y usa el `accessToken` que devuelve como
`Authorization: Bearer <token>` para consultar `GET /api/v1/users/me`.

```json
{
  "name": "Ada Lovelace",
  "email": "ada@example.edu",
  "password": "A-long-local-password-2026"
}
```

Las nuevas cuentas reciben el rol `USER`. La clave JWT predeterminada solo es válida para desarrollo
local; antes de desplegar, define `APP_SECURITY_JWT_SECRET` con un valor aleatorio de al menos 32 bytes.

Para preparar la cuenta administradora local, define temporalmente `APP_BOOTSTRAP_ADMIN_NAME`,
`APP_BOOTSTRAP_ADMIN_EMAIL` y `APP_BOOTSTRAP_ADMIN_PASSWORD` (mínimo 12 caracteres) en la configuración
de ejecución de Eclipse y arranca el backend una vez. Si el correo ya pertenece a una cuenta, se le
agrega el rol `ADMINISTRATOR` sin quitar sus otros roles. Luego puedes quitar esas variables de Eclipse;
la cuenta y sus roles quedan guardados en la base de datos.

## Catálogo público

- `GET /api/v1/space-types` lista los tipos de espacio.
- `GET /api/v1/spaces` lista los espacios activos. Acepta los filtros opcionales `query`, `type` y
  `minimumCapacity`.
- `GET /api/v1/spaces/availability` encuentra espacios libres para un periodo y capacidad opcional.
- `GET /api/v1/spaces/{id}/agenda?date=AAAA-MM-DD` consulta la agenda diaria sin exponer datos personales.

## Administración

- Un usuario puede tener más de un rol; las asignaciones se guardan en `usuario_roles`.
- `GET /api/v1/users` y las rutas `PATCH /api/v1/users/{id}/roles` y
  `PATCH /api/v1/users/{id}/managed-spaces` y `PATCH /api/v1/users/{id}/status` están reservadas al rol
  `ADMINISTRATOR`. Desactivar una cuenta invalida sus tokens existentes; el sistema impide desactivar al último
  administrador activo.
- Los administradores pueden crear y editar tipos y espacios, consultar agenda global, indicadores de uso y
  auditoría. Cambiar el estado de un espacio conserva sus reservas previas.
- Los usuarios con rol `MANAGER` pueden consultar y cambiar el estado de espacios asignados, además de administrar
  bloqueos únicamente para los espacios registrados en `espacio_responsables`. Estas restricciones también se
  aplican en la API. Si un usuario tiene tanto `ADMINISTRATOR` como `MANAGER`, conserva ambos roles y permisos.
- `GET /api/v1/spaces/manage` permite a administradores filtrar el catálogo por nombre/ubicación, tipo, capacidad
  mínima y estado. Para responsables, la misma ruta devuelve exclusivamente sus espacios asignados.

## Reservas

Las rutas de reservas requieren un token de una cuenta `USER`, `MANAGER` o `ADMINISTRATOR`:

- `POST /api/v1/reservations` crea una reserva y rechaza conflictos de horario y exceso de capacidad.
- `GET /api/v1/reservations/me` lista las reservas de la cuenta autenticada.
- `PATCH /api/v1/reservations/{id}/cancel` conserva el historial y permite cancelar al propietario o a un administrador.

## Bloqueos administrativos y auditoría

Un usuario con rol `ADMINISTRATOR` puede crear un bloqueo con `POST /api/v1/space-blocks`, listar bloqueos con
`GET /api/v1/space-blocks` y cancelarlo mediante `PATCH /api/v1/space-blocks/{id}/cancel`. Los usuarios con
rol `MANAGER` tienen las mismas acciones solo en espacios asignados. Un bloqueo activo rechaza nuevas reservas
solapadas. Las creaciones y cancelaciones de reservas y bloqueos, y las operaciones administrativas, se registran
en `auditorias`.

Al iniciar `RsuAuthServiceApplication`, Flyway crea las tablas y carga los roles y espacios iniciales. Todas
las tablas de la aplicación usan IDs numéricos y las tablas de relación
`usuario_roles` y `espacio_responsables` también tienen su propio `id`; los IDs de rol son 1–4 y los demás se
generan automáticamente. En las tablas creadas por el sistema, `id` es la primera columna. Las migraciones
convierten las claves existentes y mantienen usuarios, espacios, asignaciones y relaciones. Si una base ya
tenía tablas antes de activar Flyway, la migración se aplica a sus datos sin borrar la base.

## Despliegue HTTPS

El perfil `prod` usa MariaDB y requiere `DB_URL`, `DB_USER`, `DB_PASSWORD`, `APP_SECURITY_JWT_SECRET` (mínimo
32 bytes) y `APP_WEB_ALLOWED_ORIGINS` (orígenes HTTPS exactos de la interfaz). Arráncalo con
`SPRING_PROFILES_ACTIVE=prod`. El perfil fuerza HTTPS en las solicitudes y procesa `X-Forwarded-*`; despliega la
API detrás de un proxy TLS de confianza que termine HTTPS y reemplace esas cabeceras. No expongas directamente el
puerto HTTP del servicio. Configura el frontend con la URL HTTPS de la API mediante
`localStorage.setItem("reservationApiBaseUrl", "https://api.ejemplo.edu")`.

Las pruebas automatizadas usan una base H2 en memoria aislada. Los criterios que dependen de un servidor real
(certificado TLS, velocidad bajo carga y navegadores/dispositivos compatibles) se deben comprobar antes de aceptar
el despliegue; el registro está en `docs/verification-checklist.md`.

## Integración local con el frontend

El frontend estático se conecta por defecto a `http://localhost:8080`. El backend permite por defecto
los orígenes de XAMPP en los puertos HTTP habitual y 9090 (`http://localhost`, `http://127.0.0.1`,
`http://localhost:9090` y `http://127.0.0.1:9090`), además de los puertos 5500 y 8000 usados por
servidores de desarrollo. Para usar otro origen (por ejemplo, `http://localhost:8081`), configura
`APP_WEB_ALLOWED_ORIGINS` como una lista separada por comas antes
de iniciar el servicio. Comprueba que la API esté activa abriendo `http://localhost:8080/api/v1/health`;
debe responder con `status: UP`.

## Estructura

```text
src/main/java/       Código de la API
src/main/resources/  Configuración y migraciones Flyway
docs/                Arquitectura y plan de implementación
```
