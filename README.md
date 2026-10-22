# Reserva Campus

Sistema web para consultar y reservar espacios institucionales de la Universidad Antonio José Camacho. Es un proyecto académico del curso **Fundamentos Web**.

**Encargado:** Esteban Pineda Rodríguez.

## Propósito

Reserva Campus centraliza la información de los espacios universitarios y facilita la consulta de disponibilidad y la gestión de reservas. También permite administrar espacios, responsables y bloqueos de agenda según los permisos de cada cuenta.

## Funcionalidades principales

- Consultar y filtrar espacios por nombre, tipo, ubicación, capacidad y estado.
- Buscar espacios disponibles para un horario y consultar sus agendas.
- Crear reservas, consultar el historial propio y cancelar reservas permitidas.
- Administrar espacios y tipos de espacio.
- Asignar responsables a espacios y limitar su gestión a los espacios asignados.
- Crear y cancelar bloqueos de agenda.
- Administrar cuentas, estados y roles.
- Consultar reportes de uso y registros de auditoría.
- Consultar el perfil de la cuenta y cerrar sesión.

## Roles y permisos

Una cuenta puede tener varios roles.

- **USER:** consulta espacios y disponibilidad, solicita reservas y gestiona sus propias reservas.
- **MANAGER:** administra el estado y los bloqueos de los espacios asignados. También puede usar las funciones de reserva disponibles para su cuenta.
- **ADMINISTRATOR:** administra el catálogo, las cuentas y sus roles; también consulta información global, reportes y auditoría.

Los roles definidos actualmente son `USER`, `MANAGER` y `ADMINISTRATOR`.

## Organización del sistema

El proyecto está compuesto por una interfaz web y una API REST:

- **Interfaz web:** ubicada en `reserva_espacios/frontend`. Contiene las páginas de inicio, espacios, reservas, perfil y administración. Está desarrollada con HTML, CSS y JavaScript, y consume la API.
- **Backend:** ubicado en `rsu_auth-service`. Contiene la API Java, las reglas del negocio, la autenticación y el acceso a los datos.

## Tecnologías y datos

- **Frontend:** HTML, CSS y JavaScript.
- **Backend:** Java 21, Spring Boot, Spring Security, Spring Data JPA e Hibernate.
- **Autenticación:** tokens JWT.
- **Base de datos:** H2 persistente en el entorno local y configuración disponible para PostgreSQL.
- **Migraciones:** Flyway administra los cambios del esquema y los datos iniciales.

La base H2 local guarda sus datos en un archivo, por lo que las cuentas, reservas y demás registros se conservan entre reinicios. El token de sesión se almacena en el navegador.

## Relación con Fundamentos Web

Como proyecto del curso **Fundamentos Web**, integra la construcción de páginas web con la comunicación entre frontend y backend. También aplica conceptos de diseño de interfaces, formularios, consumo de servicios REST, persistencia de datos y control de acceso por roles.
