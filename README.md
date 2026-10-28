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

Usuarios de demostración (Spring Boot los crea con BCrypt):

- admin / admin123
- cajero / cajero123
- cocina / cocina123

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
