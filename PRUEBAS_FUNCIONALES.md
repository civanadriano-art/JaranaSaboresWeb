# Pruebas funcionales — La Jarana de Sabores

## 1. Gestión de usuarios

Ingresar como `admin / admin123` y abrir **Usuarios**.

1. Crear un usuario con rol Cajero o Cocinero y una contraseña temporal de 8 o más caracteres.
2. Cerrar sesión e ingresar con la cuenta nueva.
3. El sistema debe redirigir a **Cambiar contraseña** antes de permitir el uso normal.
4. Cambiar la contraseña y volver a ingresar con la nueva clave.
5. Desde Administrador, restablecer nuevamente la contraseña. Debe volver a quedar marcado como cambio pendiente.
6. Realizar 5 intentos fallidos de login. La cuenta debe quedar bloqueada por 15 minutos.
7. Desde Administrador, usar **Desbloquear**. La cuenta debe poder ingresar nuevamente.
8. Desactivar la cuenta. El acceso debe ser rechazado. Reactivarla y volver a comprobar.

## 2. Flujo completo del pedido

1. Ingresar como Cajero.
2. Registrar un pedido con cliente, un plato, observación opcional y costo extra opcional.
3. Confirmar en MySQL que aparece en `pedidos` y `detalle_pedido`.
4. Ingresar como Cocina y cambiar `Pendiente -> En preparación -> Listo`.
5. Intentar despachar antes del cobro: el backend debe responder que el pedido debe estar pagado.
6. Ingresar como Cajero y registrar un pago con `EFECTIVO`, `YAPE` o `PLIN`.
7. Confirmar en MySQL que existe un registro en `pagos`.
8. Despachar el pedido. Debe pasar a `DESPACHADO` y crear un registro en `ventas`.

Consultas útiles:

```sql
USE restaurante_db;
SELECT * FROM pedidos ORDER BY numero DESC;
SELECT * FROM detalle_pedido ORDER BY id_detalle DESC;
SELECT * FROM pagos ORDER BY id_pago DESC;
SELECT * FROM ventas ORDER BY id_venta DESC;
SELECT * FROM eventos_auditoria ORDER BY id_evento DESC;
```

## 3. Backup real

Ingresar como Administrador y abrir **Backup**.

1. Presionar **Verificar configuración**. Debe indicar `mysqldump OK` y `mysql OK`.
2. Presionar **Crear backup ahora**. Debe aparecer un archivo `.sql` dentro de la carpeta `backups`.
3. Crear un dato de prueba, por ejemplo un nuevo plato o pedido.
4. Presionar **Restaurar último backup** y confirmar.
5. El sistema crea primero una copia preventiva `pre_restauracion_...sql`.
6. Al finalizar, la base debe volver al estado del backup seleccionado y las estructuras AVL/listas/HashMap se recargan desde MySQL.

> La restauración reemplaza los datos actuales por los del backup. Realizarla solo con datos de prueba durante la validación.
