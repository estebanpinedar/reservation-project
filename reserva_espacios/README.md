# Frontend de Reserva Campus

Interfaz estática en HTML, CSS y JavaScript que consume la API REST de `rsu_auth-service`.

## Ejecución local

1. Inicia el backend siguiendo su README. La interfaz espera la API en `http://localhost:8080`.
   Si sirves esta carpeta desde XAMPP en `http://localhost` o `http://localhost:9090`, el backend ya
   permite ese origen por defecto. Si Apache usa otro puerto, agrega ese origen a
   `APP_WEB_ALLOWED_ORIGINS` y reinicia el backend.
2. Desde esta carpeta, inicia un servidor estático:

   ```bash
   python3 -m http.server 5500
   ```

3. Abre `http://localhost:5500` en el navegador.

No abras los HTML como archivos `file://`: el navegador necesita un origen HTTP para enviar las
solicitudes a la API.

## Funciones conectadas

- Registro e inicio de sesión con JWT.
- Consulta y filtrado del catálogo por texto, tipo y capacidad mínima.
- Búsqueda de disponibilidad por horario, capacidad y agenda diaria de cada espacio.
- Creación de reservas, consulta filtrada de las propias reservas y cancelación.
- Administración de espacios, tipos, usuarios, estado de cuentas y varios roles por usuario.
- Filtros administrativos de espacios por texto, tipo, capacidad mínima y estado.
- Asignación de espacios a responsables; pueden cambiar el estado y administrar bloqueos solo en espacios asignados.
- Agenda global, reporte de uso y auditoría para administradores.
- Cierre de sesión y eliminación del token local.

El token se guarda en `localStorage` bajo la clave `reservationAccessToken`. La URL de API se puede
cambiar desde la consola del navegador con `localStorage.setItem("reservationApiBaseUrl", "https://servidor-api")`.

Para habilitar el panel de administración local, configura `APP_BOOTSTRAP_ADMIN_NAME`,
`APP_BOOTSTRAP_ADMIN_EMAIL` y `APP_BOOTSTRAP_ADMIN_PASSWORD` (12 caracteres como mínimo) en la ejecución
de Eclipse y reinicia el backend una vez. Se puede agregar el rol de administrador a una cuenta existente;
los demás roles de esa cuenta se conservan. Luego elimina esas variables de la configuración de ejecución.

El backend permite por defecto los orígenes locales `localhost` y `127.0.0.1` en los puertos 5500 y
8000. Para otro origen, configura `APP_WEB_ALLOWED_ORIGINS` con una lista separada por comas antes
de iniciar el backend.
