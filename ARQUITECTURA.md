# Arquitectura del sistema

NAVEGADOR
  HTML + CSS + JavaScript + Bootstrap
            |
          HTTP/JSON
            v
JAVA + SPRING BOOT
  Controller
      |
    Service
      |
  Modelo + reglas de negocio
      |
  AVL + listas enlazadas + HashMap
      |
   Repository / JPA
      |
     MySQL

## Ejemplo: registrar pedido

pedidos.html -> pedidos.js -> POST /api/pedidos -> PedidoController -> PedidoService -> AVL/ListaEnlazada -> PedidoRepository -> MySQL

## Ejemplo: pago

pagos.html -> pagos.js -> POST /api/pagos/pedido/{numero} -> PagoController -> PagoService -> PagoRepository -> MySQL

## Separación de responsabilidades

- HTML: estructura visual.
- CSS + Bootstrap: estilos y adaptación a pantallas.
- JavaScript: interacción y llamadas a API REST.
- Spring Boot: reglas, seguridad y endpoints.
- Estructuras Java: organización académica de pedidos, catálogo, eliminados y ventas.
- MySQL: almacenamiento persistente.
