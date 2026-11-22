# La Jarana de Sabores — versión HTML/CSS/JS/Bootstrap

## Arquitectura

- Frontend: HTML5 + CSS3 + JavaScript + Bootstrap 5
- Backend: Java 21 + Spring Boot 3.5.6
- Persistencia: MySQL 8+
- Estructuras en Java: Árbol AVL + Lista enlazada + HashMap
- Patrón: MVC / arquitectura por capas (Controller, Service, Model, Repository)

Esta versión NO utiliza React ni TypeScript.

## Pantallas

- login.html
- dashboard.html
- pedidos.html
- cocina.html
- pagos.html
- catalogo.html
- historial.html
- reportes.html
- auditoria.html
- backup.html

## Puerto

La aplicación está configurada en:

http://localhost:8081

Se usa 8081 para evitar el conflicto que existía con el puerto 8080.

## 1. Preparar MySQL

En MySQL Workbench ejecuta:

sql/01_crear_bd.sql

Si YA tenías una versión anterior de restaurante_db, usa primero un backup y luego ejecuta:

sql/03_actualizacion_frontend_simple.sql

## 2. Configurar usuario/contraseña de MySQL

Archivo:

src/main/resources/application.properties

Por defecto:

spring.datasource.username=${DB_USER:root}
spring.datasource.password=${DB_PASSWORD:root}

Configuración preparada para este equipo: MySQL puerto 3307, usuario root y contraseña root. Puedes sobrescribirla con variables de entorno si luego cambias la configuración:

$env:DB_PASSWORD="TU_CLAVE"

O editar temporalmente application.properties para tu equipo local.

## 3. Ejecutar

Desde VS Code abre la carpeta donde está pom.xml y ejecuta:

src/main/java/pe/edu/utp/jarana/JaranaSaboresApplication.java

Usa Run Java / Run.

Si Maven está configurado también puedes usar:

mvn spring-boot:run

## 4. Abrir en navegador

http://localhost:8081/login.html

### Cuenta de administrador inicial

Al iniciar por primera vez, Spring Boot crea una cuenta de administrador (la contraseña se guarda cifrada con BCrypt):

- **Usuario:** `Esperanza`
- **Contraseña:** la definida en `app.admin.password` de `application.properties`

El sistema ya no crea cuentas de demostración. Si una base de datos antigua conserva `admin`, `cajero` o `cocina` con sus contraseñas originales, se desactivan automáticamente al iniciar (no se borran, para conservar el historial). Una cuenta con contraseña propia nunca se toca.

Desde **Usuarios** el administrador crea las cuentas del personal (cajeros y cocineros). Las contraseñas deben tener mínimo 8 caracteres y combinar letras y números, y una cuenta con contraseña temporal solo puede usar el sistema después de cambiarla. Recomendado: cambiar la contraseña inicial y, para no dejarla escrita en el código, definirla con las variables de entorno `ADMIN_USER`, `ADMIN_NOMBRE` y `ADMIN_PASSWORD` antes de iniciar.

## Roles

### CAJERO
- registrar y modificar pedidos pendientes
- cancelar pedidos pendientes no pagados
- registrar pagos
- despachar pedidos listos

### COCINERO
- consultar pedidos
- iniciar preparación
- marcar pedidos como listos

### ADMINISTRADOR
- acceso general
- catálogo
- reportes
- historial
- auditoría
- información de backup

## MySQL

Tablas principales:

- usuarios
- clientes
- productos
- pedidos
- detalle_pedido
- pagos
- ventas
- pedidos_eliminados
- eventos_auditoria

## Backup

Los scripts están en:

scripts/backup_mysql.bat
scripts/restaurar_mysql.bat

La pantalla backup.html no ejecuta comandos del sistema desde el navegador. Esto es intencional por seguridad.

## Acceso desde tablet o celular en la misma red Wi-Fi

La aplicación incluye `server.address=0.0.0.0`, por lo que Spring Boot puede aceptar conexiones desde otros equipos de la red local.

1. Ejecuta `JaranaSaboresApplication.java` en la PC servidor.
2. En PowerShell ejecuta `ipconfig` y busca la Dirección IPv4, por ejemplo `192.168.1.35`.
3. Conecta la tablet/celular al mismo Wi-Fi.
4. Abre en su navegador: `http://192.168.1.35:8081`
5. Si Windows pregunta por Java en el Firewall, permite el acceso en redes privadas. Si no aparece la pregunta, crea una regla de entrada TCP para el puerto 8081.

MySQL sigue siendo local en la PC servidor y usa el puerto 3307; las tablets y celulares NO se conectan directamente a MySQL. Se conectan a Spring Boot.

La interfaz ya incluye viewport, rejillas Bootstrap, tablas desplazables, menú lateral móvil y controles táctiles adaptados para pantallas pequeñas.


## Backup desde el panel web

El administrador puede crear una copia de seguridad y restaurar la última copia desde `backup.html`. El backend utiliza `mysqldump` y `mysql`. En Windows intenta detectar automáticamente las herramientas instaladas de MySQL; también se pueden definir `MYSQLDUMP_PATH` y `MYSQL_PATH`. Los archivos `.sql` se guardan en la carpeta `backups/`, excluida de Git.

## Ajustes recientes
- El teléfono del cliente es opcional. Si se ingresa, debe contener entre 7 y 9 dígitos.
- Los pedidos activos ya no tienen un límite fijo de 50.
- Si la base ya existía, ejecutar `sql/04_telefono_opcional.sql`.


## Actualización: extras y pago antes del despacho

- Las observaciones de cada plato siguen siendo opcionales.
- Cada detalle puede incluir un costo extra opcional.
- El total del pedido y el cobro incluyen el costo extra.
- Un pedido listo no puede despacharse hasta que tenga un pago registrado.
- Medios de pago disponibles: Efectivo, Yape y Plin.
- Para una base ya creada, ejecutar `sql/05_extra_y_pago_antes_despacho.sql`.


## Gestión de usuarios y recuperación de contraseña

El administrador dispone de `/usuarios.html` para crear, activar/desactivar, desbloquear y restablecer contraseñas. Las contraseñas se almacenan con BCrypt. Una contraseña restablecida se considera temporal y obliga al usuario a cambiarla en el siguiente inicio de sesión. Después de 5 intentos fallidos, la cuenta queda bloqueada durante 15 minutos o hasta que un administrador la desbloquee.

## Pruebas

Consulta `PRUEBAS_FUNCIONALES.md` para validar el flujo completo de pedido, cobro, despacho, usuarios y backup/restauración.

## Reportes mejorados
La pantalla **Reportes e indicadores** incluye filtros rápidos (Hoy, Esta semana, Este mes y Todos), rango personalizado, filtro por Efectivo/Yape/Plin, total vendido, pedidos despachados, ticket promedio, plato más vendido, tiempo promedio de preparación, ventas por día, distribución por método de pago y detalle de ventas. El reporte puede imprimirse/guardarse como PDF desde el navegador y exportarse a un archivo **Excel (.xlsx)** generado en el servidor con Apache POI con tres hojas: *Resumen* (indicadores, ventas por método de pago con porcentaje y ventas por día), *Ventas* (detalle con el total calculado) y *Platos* (ranking de platos por cantidad e importe), listas para imprimir. La exportación respeta los filtros activos y queda registrada en Auditoría.

### Librerías empresariales utilizadas
| Librería | Uso empresarial | Dónde se aplica en el proyecto |
|---|---|---|
| **Apache POI** | Reportes empresariales | `ReporteExcelService`: Excel `.xlsx` con hojas Resumen, Ventas y Platos |
| **Logback** | Monitoreo de sistemas | Logs en `logs/jarana.log` con el usuario de cada acción; `LogContextoFilter` |
| **Apache Commons Lang** | Validaciones y utilidades | `StringUtils` en pedidos, usuarios y búsquedas (limpieza y validación de texto) |
| **Google Guava** | Optimización de código | `Multiset` para el ranking de platos (`ReporteService`) y `Stopwatch` para medir el tiempo del reporte |
| **Seguridad Java** | Protección de información | Spring Security y BCrypt, bloqueo de cuentas, cookie de sesión `SameSite=Lax`, errores internos sin detalles técnicos (solo una referencia) |

Las versiones están centralizadas en las propiedades del `pom.xml`.

### Pruebas de seguridad
El informe `PRUEBAS_SEGURIDAD.md` contiene el plan de pruebas, los resultados con evidencias, el registro de incidencias (INC-01 a INC-13) y las tareas pendientes. Las pruebas se pueden repetir con `pruebas/pruebas_seguridad.py` (usar solo una base de datos de pruebas).

### Registro de eventos (logs)
El sistema escribe logs con Logback en consola y en `logs/jarana.log` (rotación a 5 MB, se conservan 14 días). Cada línea indica quién realizó la acción (por ejemplo `[cajero]`). Se registran los cambios de estado de los pedidos, los pagos, los inicios de sesión correctos y fallidos, los bloqueos de cuenta, la administración de usuarios y del catálogo, los backups y los errores internos. Se puede cambiar la ruta con la variable de entorno `LOG_FILE`.

El tiempo de preparación se calcula desde el inicio de preparación hasta que el pedido se marca Listo. Cuando una prueba dura menos de un minuto, se registra como 1 minuto para evitar reportes con `0 min`.

Si ya existen ventas antiguas con `0 min`, ejecutar una sola vez `sql/10_corregir_tiempos_reportes.sql` en MySQL Workbench. El script recalcula el tiempo únicamente cuando el pedido conserva las fechas de inicio de preparación y de Listo.

## Gestión de usuarios mejorada
La pantalla de administración de usuarios incorpora búsqueda por nombre/usuario, filtros por rol y estado, indicadores de cuentas activas/bloqueadas/inactivas, confirmación de contraseñas temporales y acciones contextuales. La cuenta conectada no puede desactivarse desde la interfaz y el backend impide desactivar al último administrador activo.

## Catálogo mejorado
La pantalla de catálogo incluye indicadores de total, disponibles y agotados; búsqueda por nombre/código; filtros por categoría y disponibilidad; alta/edición en modal; y cambio rápido entre disponible y agotado. No se muestra una columna ni filtro de estado.

### Historial y actividad administrativa

La pantalla **Historial** permite buscar y filtrar pedidos cancelados y restaurar un pedido específico al estado Pendiente. La restauración individual está restringida al rol Administrador y queda registrada en auditoría.

La pantalla **Actividad** presenta las 100 acciones más recientes con nombres amigables, filtros por usuario/acción/fecha, indicadores generales, detalle de cada evento y paginación de 20 registros por página.

## Panel por rol y navegación segura

El panel principal adapta sus indicadores y prioridades al rol autenticado (Caja, Cocina o Administrador). Los elementos con `data-role` permanecen ocultos hasta que `/api/auth/me` confirma la sesión, evitando el parpadeo visual de opciones no autorizadas. Spring Security también restringe directamente las páginas de cada rol.


### Disponibilidad de platos desde Cocina
El rol COCINERO puede marcar platos como **Agotados** o **Disponibles** desde el Panel de cocina. Los platos agotados dejan de estar disponibles para nuevos pedidos de Caja y el backend también rechaza su registro aunque el formulario del cajero estuviera abierto previamente.


## Cuadros de confirmación del sistema

Las acciones sensibles ya no utilizan `alert()`, `confirm()` ni `prompt()` del navegador. Catálogo, Historial, Usuarios, Cobros, Backup y Pedidos usan modales propios con el diseño de La Jarana de Sabores. El motivo de cancelación de pedidos se captura también dentro de un modal del sistema.

## Atención prioritaria de Caja
El Inicio del rol CAJERO incluye un bloque de atención prioritaria para pedidos en estado LISTO. Los pedidos listos sin pago se muestran primero con acceso directo a Cobros. Los pedidos ya pagados aparecen después con acción directa de despacho. El tiempo de espera se resalta visualmente a partir de 10 y 20 minutos.
