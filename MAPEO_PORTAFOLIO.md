# Mapeo entre portafolio y código

| Portafolio | Implementación |
|---|---|
| Frontend HTML5/CSS3/JavaScript/Bootstrap | `src/main/resources/static/` |
| Backend Java + Spring Boot | `src/main/java/pe/edu/utp/jarana/` |
| MVC / arquitectura en capas | `controller`, `service`, `model`, `repository` |
| AVL de pedidos pendientes | `EstructurasDatosService.pedidosPendientes` |
| AVL de pedidos eliminados | `EstructurasDatosService.pedidosEliminados` |
| AVL catálogo por nombre | `EstructurasDatosService.catalogoPorNombre` |
| HashMap por código | `EstructurasDatosService.platosPorCodigo` |
| Listas enlazadas | `ListaEnlazada` y uso en `PedidoService` / `ReporteService` |
| MySQL | repositorios Spring Data JPA |
| FIFO | `ArbolAVL.obtenerMenor()` por número de pedido |
| LIFO de restauración | `ArbolAVL.obtenerMayor()` por id de eliminación |
| Estados | `EstadoPedido` |
| Ventas / reportes | `Venta`, `ReporteService`, `/api/reportes` |
| Roles | Spring Security + `RolUsuario` |
