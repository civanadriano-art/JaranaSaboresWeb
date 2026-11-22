# Pruebas de seguridad — La Jarana de Sabores

Curso Integrador I: Sistemas-Software · Unidad 4: Pruebas, Despliegue y Mantenimiento · Sesión 28

## 1. Resumen

| Dato | Valor |
|---|---|
| Sistema probado | La Jarana de Sabores (Spring Boot 3.5.6, Java 21, MySQL 8) |
| Entorno | Sistema ejecutado localmente con una base MySQL real y datos de prueba |
| Pruebas automatizadas | **78** (script `pruebas/pruebas_seguridad.py`) |
| Resultado final | **71 conformes · 7 pendientes** |
| Hallazgos corregidos | 5 (INC-01 a INC-05) |
| Hallazgos pendientes | 7 (INC-06 a INC-12), 1 observación aceptada (INC-13) |

Antes de las correcciones, **16 de las pruebas fallaban**; después, quedan **7** y ninguna es un error de código: son decisiones de diseño o de despliegue que requieren acuerdo del equipo (HTTPS, política de bloqueo, etc.).

## 2. Plan de pruebas

Siguiendo los tipos de la clase: **autenticación, autorización y roles, validación de entradas, gestión de sesiones, protección de datos y manejo de errores**; se agregaron **integridad del flujo de pedidos, CSRF/CORS y fuerza bruta**.

| ID | Funcionalidad | Prueba | Resultado esperado |
|---|---|---|---|
| PS-01 | Inicio de sesión | Contraseña incorrecta | Acceso rechazado |
| PS-02 | Inicio de sesión | Usuario inexistente | Misma respuesta que PS-01 (no revela si existe) |
| PS-03 | Inicio de sesión | Inyección SQL en el usuario | Acceso rechazado |
| PS-05 | Bloqueo de cuenta | 5 intentos fallidos y uno correcto | Cuenta bloqueada |
| PS-09 | Roles | Matriz 41 recursos × 4 perfiles (anónimo, cajero, cocinero, administrador) | 100 % conforme a las reglas |
| PS-10 a PS-13 | Roles | Cajero/cocinero intentan acciones de administrador | Acceso denegado (403) |
| PS-14 a PS-18 | Sesiones | Cuenta con contraseña temporal usa la API directamente | Bloqueado hasta cambiarla |
| PS-20 a PS-39b | Validación | Datos inválidos, límites, XSS, inyección SQL, contraseñas débiles | Error 400, sin guardar datos |
| PS-40 a PS-50 | Integridad | Flujo pedido → cocina → pago → despacho y sus reglas | Se rechazan las operaciones inválidas |
| PS-51 a PS-57 | Sesiones | Cookie, fijación de sesión, cierre de sesión, expiración | Sesión protegida |
| PS-58 a PS-64 | Protección de datos | Hash de contraseñas, cabeceras, archivos internos, logs, HTTPS | Sin fugas de información |
| PS-65 a PS-68 | Manejo de errores | Rutas inexistentes, métodos inválidos, errores internos | Mensajes seguros y códigos correctos |
| PS-69 a PS-71 | CSRF / CORS | Peticiones desde otros sitios | Rechazadas |
| PS-72 a PS-74 | Fuerza bruta | Intentos masivos, bloqueo abusivo, enumeración de cuentas | Sistema resiste |

## 3. Resultados y evidencias

La columna **Antes** indica cómo estaba el sistema antes de las correcciones.

| ID | Tipo | Prueba | Esperado | Resultado | Antes |
|---|---|---|---|---|---|
| PS-00 | Autenticación | Login del administrador inicial | 302 → /dashboard.html | ✅ Conforme | Conforme |
| PS-01 | Autenticación | Contraseña incorrecta | 302 → /login.html?error | ✅ Conforme | Conforme |
| PS-02 | Autenticación | Usuario inexistente = misma respuesta (sin enumerar) | igual que PS-01 | ✅ Conforme | Conforme |
| PS-03 | Autenticación | Inyección SQL en el usuario del login | acceso rechazado | ✅ Conforme | Conforme |
| PS-04 | Datos | Contraseña guardada con BCrypt (no en texto plano) | $2a$… y sin la clave | ✅ Conforme | Conforme |
| PS-05 | Autenticación | Bloqueo tras 5 intentos; el 6º (incluso con clave correcta) se rechaza | 5×?error, luego ?bloqueado | ✅ Conforme | Conforme |
| PS-06 | Autenticación | El administrador puede desbloquear la cuenta | login permitido | ✅ Conforme | Conforme |
| PS-07 | Autenticación | Cuenta desactivada no puede ingresar | 302 → ?inactivo | ✅ Conforme | Conforme |
| PS-08 | Autenticación | Sin sesión: GET /dashboard.html | redirige a login / 401 | ✅ Conforme | Conforme |
| PS-08 | Autenticación | Sin sesión: GET /api/pedidos | redirige a login / 401 | ✅ Conforme | Conforme |
| PS-08 | Autenticación | Sin sesión: GET /api/usuarios | redirige a login / 401 | ✅ Conforme | Conforme |
| PS-09 | Autorización | Matriz rol × recurso (41 recursos × 4 perfiles) | 100% conforme a las reglas | ✅ Conforme | Conforme |
| PS-10 | Autorización | Cajero intenta crear usuarios | 403 | ✅ Conforme | Conforme |
| PS-11 | Autorización | Cocinero intenta registrar un pedido | 403 | ✅ Conforme | Conforme |
| PS-12 | Autorización | Cajero intenta ver reportes y auditoría | 403 | ✅ Conforme | Conforme |
| PS-13 | Autorización | Cajero/cocinero intentan restaurar la base de datos | 403 | ✅ Conforme | Conforme |
| PS-14 | Sesiones | Cuenta con contraseña TEMPORAL no puede operar la API hasta cambiarla | 403 | ✅ Conforme | Falló |
| PS-15 | Sesiones | …pero sí puede consultar /api/auth/me y cambiar su contraseña | 200 | ✅ Conforme | Prueba nueva |
| PS-16 | Validación | Cambiar a contraseña solo con letras | 400 | ✅ Conforme | Prueba nueva |
| PS-17 | Sesiones | Cambio de contraseña válido | 200 | ✅ Conforme | Prueba nueva |
| PS-18 | Sesiones | Tras cambiarla, la misma sesión ya puede operar | 201 | ✅ Conforme | Prueba nueva |
| PS-20 | Validación | Nombre con <script> (XSS) | 400 | ✅ Conforme | Conforme |
| PS-21 | Validación | Nombre con inyección SQL | 400 y las 9 tablas intactas | ✅ Conforme | Conforme |
| PS-22 | Validación | Teléfono inválido (letras / 20 dígitos) | 400 / 400 | ✅ Conforme | Conforme |
| PS-23 | Validación | Pedido sin platos | 400 | ✅ Conforme | Conforme |
| PS-24 | Validación | Cantidad 0 y negativa | 400 / 400 | ✅ Conforme | Conforme |
| PS-25 | Validación | Costo extra negativo | 400 | ✅ Conforme | Conforme |
| PS-26 | Validación | Plato inexistente | 400/404 | ✅ Conforme | Conforme |
| PS-27 | Validación | Cantidad absurda (1 000 000 de platos) | rechazada (límite razonable) | ✅ Conforme | Falló |
| PS-28 | Validación | Costo extra fuera de rango (99 999 999 999) | 400 | ✅ Conforme | Falló |
| PS-29 | Validación | Observación de 5 000 caracteres | 400 (validado) | ✅ Conforme | Falló |
| PS-30 | Validación | JSON malformado | 400 | ✅ Conforme | Falló |
| PS-31 | Validación | Usuario con contraseña de 7 caracteres | 400 | ✅ Conforme | Conforme |
| PS-32 | Validación | Nombre de usuario con caracteres inválidos | 400 | ✅ Conforme | Conforme |
| PS-33 | Validación | Usuario duplicado (sin importar mayúsculas) | 400/409 | ✅ Conforme | Conforme |
| PS-34 | Validación | Política de contraseñas: rechaza claves triviales (12345678) | 400 | ✅ Conforme | Falló |
| PS-35 | Validación | Nombre de usuario con HTML (el frontend lo escapa: ver PS-36) | aceptado o rechazado | ✅ Conforme | Conforme |
| PS-36 | Validación | Frontend escapa los datos antes de mostrarlos (revisión estática de 54 usos de … | 0 puntos sin escapar | ✅ Conforme | Prueba nueva |
| PS-37 | Validación | Contraseña solo con letras | 400 | ✅ Conforme | Prueba nueva |
| PS-38 | Validación | Plato con nombre de 500 caracteres | 400 | ✅ Conforme | Prueba nueva |
| PS-39 | Validación | Pedido con 51 líneas (máx. 50) | 400 | ✅ Conforme | Prueba nueva |
| PS-39b | Validación | Cantidad 100 (máx. 99) | 400 | ✅ Conforme | Prueba nueva |
| PS-40 | Integridad | Pedido válido se registra | 201 | ✅ Conforme | Conforme |
| PS-41 | Integridad | Despachar un pedido Pendiente | rechazado | ✅ Conforme | Conforme |
| PS-42 | Integridad | Despachar un pedido Listo SIN pago | rechazado | ✅ Conforme | Conforme |
| PS-43 | Integridad | Pago en efectivo menor al total | rechazado | ✅ Conforme | Conforme |
| PS-44 | Validación | Método de pago inválido (BITCOIN) | 400 | ✅ Conforme | Falló |
| PS-45 | Integridad | Pago correcto | 201 | ✅ Conforme | Conforme |
| PS-46 | Integridad | Cobrar dos veces el mismo pedido | rechazado | ✅ Conforme | Conforme |
| PS-47b | Validación | Motivo de cancelación de 5 000 caracteres | 400 | ✅ Conforme | Prueba nueva |
| PS-47 | Integridad | Cancelar un pedido ya pagado | rechazado | ✅ Conforme | Conforme |
| PS-48 | Integridad | Despachar con pago | 200 | ✅ Conforme | Conforme |
| PS-49 | Integridad | Cancelar un pedido ya despachado | rechazado | ✅ Conforme | Conforme |
| PS-50 | Integridad | Reporte con Guava Multiset sobre MySQL real | platosVendidos y plato más vendido | ✅ Conforme | Conforme |
| PS-51 | Sesiones | Cookie de sesión HttpOnly + SameSite | HttpOnly; SameSite=Lax | ✅ Conforme | Conforme |
| PS-52 | Sesiones | Cookie marcada Secure (HTTPS) | Secure | ⚠️ Pendiente | Conforme |
| PS-53 | Sesiones | Fijación de sesión: el ID cambia al iniciar sesión | ID distinto | ✅ Conforme | Conforme |
| PS-54 | Sesiones | Cerrar sesión redirige al login | 302 → /login.html?logout | ✅ Conforme | Conforme |
| PS-55 | Sesiones | La sesión cerrada ya no sirve (reutilizar cookie) | denegado | ✅ Conforme | Conforme |
| PS-56 | Sesiones | Cerrar sesión solo por POST (evita logout forzado) | GET no cierra sesión | ⚠️ Pendiente | Conforme |
| PS-57 | Sesiones | Expiración de sesión por inactividad | configurada | ✅ Conforme | Conforme |
| PS-58 | Datos | /api/usuarios no expone hash ni sal | sin passwordHash/passwordSal | ✅ Conforme | Conforme |
| PS-59 | Datos | Cabeceras de seguridad básicas | nosniff, DENY, no-store, same-origin | ✅ Conforme | Conforme |
| PS-60 | Datos | Content-Security-Policy (mitiga XSS) | presente | ⚠️ Pendiente | Conforme |
| PS-61 | Datos | No revela versión del servidor | sin cabecera Server | ✅ Conforme | Conforme |
| PS-62 | Datos | Archivos internos no accesibles por web (404) | todos 404 | ✅ Conforme | Respondía 500 |
| PS-63 | Datos | Las contraseñas no aparecen en los logs | ninguna | ✅ Conforme | Conforme |
| PS-64 | Datos | Tráfico cifrado (HTTPS) | HTTPS | ⚠️ Pendiente | Conforme |
| PS-65 | Errores | Ruta inexistente / parámetro inválido / método no permitido | 404 / 400 / 405 sin detalles técnicos | ✅ Conforme | Respondía 500 |
| PS-66 | Errores | Observación de 5 000 caracteres: no filtra datos internos | mensaje claro sin detalles técnicos | ✅ Conforme | Conforme |
| PS-67 | Errores | Cantidad 1 000 000: no filtra datos internos | sin detalles técnicos | ✅ Conforme | Conforme |
| PS-68 | Errores | Solo los errores reales (no 400/404/405) se registran como "error interno" en e… | 0 falsos errores internos | ✅ Conforme | Conforme |
| PS-69 | CSRF | POST de formulario entre sitios (form-urlencoded) con sesión | 415 (solo JSON) | ✅ Conforme | Falló |
| PS-70 | CSRF | POST text/plain (técnica de CSRF sin preflight) | 415 | ✅ Conforme | Falló |
| PS-71 | CSRF | CORS: un sitio externo no recibe permiso | sin Access-Control-Allow-Origin | ✅ Conforme | Conforme |
| PS-72 | Autenticación | 40 intentos seguidos desde un mismo origen: limitación por IP | alguno bloqueado (429) | ⚠️ Pendiente | Conforme |
| PS-73 | Autenticación | Un tercero NO debería poder bloquear la cuenta de un administrador | la cuenta sigue accesible | ⚠️ Pendiente | Conforme |
| PS-74 | Autenticación | Respuesta igual para cuenta bloqueada y cuenta inexistente (sin enumerar) | misma respuesta | ⚠️ Pendiente | Conforme |

### Evidencias del log (`logs/jarana.log`)

Cada línea muestra quién realizó la acción (`[usuario]`) y no contiene contraseñas (PS-63):

```
WARN  [sistema] UsuarioService - Intento de inicio de sesión fallido para 'bloqueo1' (3 de 5)
WARN  [sistema] UsuarioService - Cuenta 'bloqueo1' bloqueada por 15 minutos tras 5 intentos fallidos
INFO  [cajero1] PagoService   - Pago EFECTIVO registrado para el pedido #2 por S/ 10.00 (operador: cajero1)
INFO  [cajero1] PedidoService - Pedido #2 despachado; venta #1 por S/ 10.00
WARN  [Esperanza] ApiExceptionHandler - Solicitud rechazada (404): NoResourceFoundException
```

## 4. Registro de incidencias

### Corregidas

| Código | Nombre | Descripción | Severidad | Acción | Estado |
|---|---|---|---|---|---|
| INC-01 | Contraseña temporal solo exigida en el frontend | Una cuenta con contraseña temporal podía usar la API directamente (creó un pedido con respuesta 201) sin cambiarla | Media | Filtro `PasswordTemporalFilter`: solo permite `/api/auth/**` hasta cambiarla | Corregido |
| INC-02 | Errores HTTP estándar respondían 500 | Ruta inexistente, método no permitido, JSON malformado o enum inválido se devolvían como error interno 500 | Media | `ApiExceptionHandler` responde 400/404/405/415 según corresponda | Corregido |
| INC-03 | Entradas sin límites | Cantidad de 1 000 000 de platos aceptada; costo extra, observación (5 000 caracteres) y motivo de cancelación fuera de rango provocaban error 500 | Media | `@Max`, `@Size`, `@DecimalMax` en los DTO y validación del motivo (≤ 255) | Corregido |
| INC-04 | Contraseñas triviales aceptadas | Se permitía `12345678` o solo letras | Baja | La contraseña debe tener mínimo 8 caracteres y combinar letras y números | Corregido |
| INC-05 | Detalle técnico en errores internos | El error 500 devolvía el mensaje de la excepción al usuario | Media | Se devuelve solo una referencia (`ref. xxxxxxxx`); el detalle queda en el log | Corregido |

### Pendientes (requieren decisión del equipo)

| Código | Nombre | Descripción | Severidad | Acción propuesta | Estado |
|---|---|---|---|---|---|
| INC-06 | Tráfico sin cifrar | El sistema funciona por HTTP; la cookie de sesión no tiene `Secure` (PS-52, PS-64) | Media | Habilitar HTTPS (`server.ssl.*` o proxy inverso) y `server.servlet.session.cookie.secure=true` | Pendiente |
| INC-07 | Bloqueo de cuenta abusable | Un tercero puede bloquear 15 min cualquier cuenta, incluida la de un administrador, con 5 intentos fallidos (PS-73) | Media | Bloqueo progresivo, bloqueo por usuario + origen, o notificación/desbloqueo automático | Pendiente |
| INC-08 | Sin límite de intentos por origen | 40 intentos seguidos desde una misma máquina no fueron frenados (PS-72) | Media | Limitar peticiones de inicio de sesión por IP | Pendiente |
| INC-09 | Mensaje distinto para cuenta bloqueada o desactivada | Permite saber que una cuenta existe (PS-74); es una decisión de usabilidad vs. seguridad | Baja | Mostrar un mensaje genérico, o aceptar el riesgo en red local | Pendiente |
| INC-10 | Cierre de sesión por GET | Una página externa podría cerrar la sesión de un usuario (PS-56) | Baja | Exigir POST y cambiar el enlace del frontend | Pendiente |
| INC-11 | Sin Content-Security-Policy | No hay política CSP (PS-60); el frontend usa scripts y eventos en línea | Baja | Mover los scripts en línea a archivos y definir CSP | Pendiente |
| INC-12 | Configuración por defecto insegura | `application.properties` trae MySQL `root/root` y la contraseña inicial del administrador | Media | Definir `DB_PASSWORD`, `ADMIN_PASSWORD` por variables de entorno antes de publicar el repositorio o usar el sistema | Pendiente |

### Observación aceptada

| Código | Nombre | Descripción | Estado |
|---|---|---|---|
| INC-13 | CSRF desactivado | El sistema no usa token CSRF. Está mitigado porque la cookie es `SameSite=Lax`, la API solo acepta JSON (415 para formularios y `text/plain`) y no hay CORS abierto (PS-51, PS-69 a PS-71) | Aceptado |

## 5. Tareas pendientes

1. Habilitar HTTPS y cookie `Secure` (INC-06).
2. Decidir la política de bloqueo y el límite de intentos por origen (INC-07, INC-08, INC-09).
3. Exigir POST para cerrar sesión (INC-10).
4. Evaluar una política CSP (INC-11).
5. Quitar contraseñas por defecto de `application.properties` (INC-12).
6. Revisar dependencias con una herramienta de vulnerabilidades (por ejemplo OWASP Dependency-Check).
7. Probar la exportación a Excel con Apache POI real (no se ejecutó en este entorno).

## 6. Lista de cotejo

| Criterio | Sí | En proceso | No |
|---|---|---|---|
| Identifica módulos y clases | ✅ | | |
| Selecciona pruebas adecuadas | ✅ | | |
| Registra evidencias | ✅ | | |
| Propone correcciones | ✅ | | |
| Presenta el avance | | ✅ | |

## 7. Alcance y límites

- Se probó en una máquina local, con un solo origen de red y sin HTTPS.
- No se evaluaron vulnerabilidades de las librerías (CVE) ni se hizo una prueba de carga.
- El frontend se revisó con análisis estático: en 54 usos de `innerHTML` no se encontraron datos sin escapar.
- La exportación a Excel no se ejecutó, porque requiere la librería Apache POI.

## 8. Cómo repetir las pruebas

> **Usa solo una base de datos de pruebas**: el script crea usuarios y pedidos de prueba y bloquea cuentas.

1. Inicia el sistema con una base nueva.
2. Instala el requisito: `pip install requests` (y el cliente `mysql` para las comprobaciones en la base).
3. Desde la carpeta del proyecto:
   ```
   set ADMIN_PASSWORD=la_contraseña_del_administrador
   python pruebas/pruebas_seguridad.py
   ```
4. Revisa el resultado en `pruebas/resultados_seguridad.json`.
