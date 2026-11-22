-- Borra SOLO los datos simulados creados por 11_datos_prueba_afluencia.sql
USE restaurante_db;
SET SQL_SAFE_UPDATES = 0;

DELETE v FROM ventas v
JOIN pedidos p ON p.numero = v.numero_pedido
JOIN clientes c ON c.id_cliente = p.id_cliente
WHERE c.nombre = 'PRUEBA AFLUENCIA';

DELETE p FROM pedidos p
JOIN clientes c ON c.id_cliente = p.id_cliente
WHERE c.nombre = 'PRUEBA AFLUENCIA';

DELETE FROM clientes WHERE nombre = 'PRUEBA AFLUENCIA';

SET SQL_SAFE_UPDATES = 1;
