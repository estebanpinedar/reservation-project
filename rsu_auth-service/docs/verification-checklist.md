# Verificación de requisitos

Este registro separa las comprobaciones automatizadas del código de las que requieren ejecutar el sistema en un
entorno real. La especificación de referencia es `03_Requerimientos_Sistema_Web_Reserva_Espacios.docx`.

## Comprobado con pruebas automatizadas

- Autenticación, perfil protegido y rechazo de credenciales inválidas.
- Catálogo filtrado por nombre/ubicación, tipo y capacidad; la consulta de administración también acepta estado.
- Creación, solapamiento, cancelación e historial de reservas.
- Un bloqueo activo impide reservas y su cancelación libera el horario.
- Dos solicitudes simultáneas para el mismo espacio y periodo producen una sola reserva.
- Administración de cuentas: un administrador puede cambiar estado, los tokens de una cuenta inactiva dejan de
  funcionar y el último administrador activo no se puede desactivar.
- Un responsable puede cambiar el estado de un espacio asignado y la API deniega cambios a espacios ajenos.
- El panel obtiene los espacios asignados incluidos los inactivos, para permitir habilitarlos de nuevo.

Ejecutar con `mvn test` desde `rsu_auth-service`. Las pruebas utilizan H2 en memoria y no modifican la base de datos
local persistente.

## Requiere evidencia del entorno de aceptación

| Requisito | Estado | Evidencia pendiente |
| --- | --- | --- |
| HTTPS en despliegue (RNF-01) | Preparado en perfil `prod`; despliegue no realizado | Abrir frontend y API con HTTPS, confirmar redirección/rechazo de HTTP y certificado válido detrás del proxy TLS. |
| Entradas seguras y mensajes claros (RNF-01, RNF-03) | Validación en backend y renderizado con `textContent`; revisar casos manuales | Probar datos inválidos, textos largos y caracteres HTML en formularios. |
| Accesibilidad WCAG 2.1 AA (RNF-04) | Formularios con etiquetas asociadas y estados anunciados; auditoría pendiente | Recorrer teclado, comprobar foco/contraste y ejecutar auditoría automatizada de accesibilidad. |
| Respuesta normal inferior a 3 segundos (RNF-05) | No medida con una carga representativa | Medir los flujos principales con datos y concurrencia similares al uso esperado. |
| Chrome, Edge, Firefox y móvil (RNF-06) | Diseño adaptable implementado; validación cruzada pendiente | Guardar evidencia de catálogo, login, reserva y administración en los tres navegadores y un móvil. |
| Persistencia H2/PostgreSQL (RNF-11) | Configuraciones y migraciones disponibles; H2 probado en memoria para tests | Reiniciar la app local y confirmar persistencia H2; ejecutar migraciones en instancia PostgreSQL de aceptación. |
| Casos e historias con evidencia funcional (RNF-12) | Casos centrales automatizados; no hay acta de aceptación por historia | Adjuntar capturas o resultados de aceptación para cada historia del documento. |
