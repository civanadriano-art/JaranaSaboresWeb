# Uso en PC, tablet y celular

## Configuración incluida
- Web/Spring Boot: puerto 8081
- Spring Boot escucha en: 0.0.0.0
- MySQL: localhost:3307
- Usuario MySQL: root
- Contraseña MySQL: root
- Base: restaurante_db

## En la PC servidor
1. Inicia MySQL Server.
2. Ejecuta `JaranaSaboresApplication.java` desde VS Code.
3. Comprueba en la PC: `http://localhost:8081`.
4. Ejecuta `ipconfig` y anota la Dirección IPv4.

## En tablet o celular
1. Conecta el dispositivo al mismo Wi-Fi que la PC.
2. Si la IPv4 de la PC es, por ejemplo, `192.168.1.35`, abre: `http://192.168.1.35:8081`.
3. Inicia sesión normalmente.

## Firewall
Permite Java/Spring Boot en redes privadas o abre una regla TCP de entrada para el puerto 8081.

## Importante
No abras el puerto 3307 de MySQL a la red para los celulares. Los dispositivos deben comunicarse únicamente con Spring Boot en el puerto 8081.
