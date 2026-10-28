USE restaurante_db;

SELECT 'usuarios' AS tabla, COUNT(*) AS registros FROM usuarios
UNION ALL SELECT 'clientes', COUNT(*) FROM clientes
UNION ALL SELECT 'productos', COUNT(*) FROM productos
UNION ALL SELECT 'pedidos', COUNT(*) FROM pedidos
UNION ALL SELECT 'detalle_pedido', COUNT(*) FROM detalle_pedido
UNION ALL SELECT 'pagos', COUNT(*) FROM pagos
UNION ALL SELECT 'ventas', COUNT(*) FROM ventas
UNION ALL SELECT 'pedidos_eliminados', COUNT(*) FROM pedidos_eliminados
UNION ALL SELECT 'eventos_auditoria', COUNT(*) FROM eventos_auditoria;

SELECT id_usuario, nombre_usuario, nombre, rol, activo, intentos_fallidos,
       bloqueado_hasta, ultimo_acceso, debe_cambiar_password
FROM usuarios
ORDER BY id_usuario;

SELECT p.numero, p.estado, c.nombre AS cliente, c.telefono,
       pg.metodo, pg.importe_total, pg.fecha_pago,
       v.id_venta, v.fecha_hora_despacho
FROM pedidos p
JOIN clientes c ON c.id_cliente = p.id_cliente
LEFT JOIN pagos pg ON pg.numero_pedido = p.numero
LEFT JOIN ventas v ON v.numero_pedido = p.numero
ORDER BY p.numero DESC;
