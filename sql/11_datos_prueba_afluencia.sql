-- =============================================================
-- DATOS DE PRUEBA para el reporte "Afluencia y personal"  (MySQL 8+)
-- Genera pedidos DESPACHADOS de los últimos 56 días (8 semanas) con este patrón por día:
--   Lunes a Jueves: 8 pedidos   Viernes: 18   Sábado: 22   Domingo: 20
-- Resultado esperado en Reportes > Afluencia y personal (8 semanas):
--   Se recomienda reforzar el personal los días: Viernes, Sábado, Domingo.
-- Todos los pedidos pertenecen al cliente 'PRUEBA AFLUENCIA' para poder borrarlos después
-- con 12_borrar_datos_prueba_afluencia.sql. Son datos SIMULADOS: no los uses como datos reales.
-- =============================================================
USE restaurante_db;

INSERT INTO clientes (nombre, telefono) VALUES ('PRUEBA AFLUENCIA', NULL);
SET @cli = LAST_INSERT_ID();

DROP TEMPORARY TABLE IF EXISTS tmp_pedidos_prueba;
CREATE TEMPORARY TABLE tmp_pedidos_prueba AS
WITH RECURSIVE dias(n) AS (
  SELECT 1 UNION ALL SELECT n + 1 FROM dias WHERE n < 56
),
cuantos(k) AS (
  SELECT 1 UNION ALL SELECT k + 1 FROM cuantos WHERE k < 22
)
SELECT DATE_SUB(CURDATE(), INTERVAL d.n DAY) AS dia, c.k AS k
FROM dias d
JOIN cuantos c
  ON c.k <= CASE DAYOFWEEK(DATE_SUB(CURDATE(), INTERVAL d.n DAY))  -- 1=Dom, 2=Lun ... 6=Vie, 7=Sáb
              WHEN 6 THEN 18
              WHEN 7 THEN 22
              WHEN 1 THEN 20
              ELSE 8
            END;

INSERT INTO pedidos (id_cliente, estado, fecha_hora_registro, fecha_hora_inicio_preparacion,
                     fecha_hora_listo, fecha_hora_despacho)
SELECT @cli, 'Despachado',
       TIMESTAMP(dia, '19:00:00') + INTERVAL (k * 9) MINUTE,
       TIMESTAMP(dia, '19:00:00') + INTERVAL (k * 9 + 2) MINUTE,
       TIMESTAMP(dia, '19:00:00') + INTERVAL (k * 9 + 14 + (k % 7)) MINUTE,
       TIMESTAMP(dia, '19:00:00') + INTERVAL (k * 9 + 17 + (k % 7)) MINUTE
FROM tmp_pedidos_prueba;

INSERT INTO ventas (numero_pedido, fecha_hora_pedido, fecha_hora_despacho, total, minutos_cocina)
SELECT numero, fecha_hora_registro, fecha_hora_despacho, 20 + (numero % 6) * 4,
       TIMESTAMPDIFF(MINUTE, fecha_hora_inicio_preparacion, fecha_hora_listo)
FROM pedidos
WHERE id_cliente = @cli;

DROP TEMPORARY TABLE IF EXISTS tmp_pedidos_prueba;

-- Verificación: debe mostrar pedidos por día de la semana
SELECT DAYNAME(fecha_hora_pedido) AS dia, COUNT(*) AS pedidos, COUNT(DISTINCT DATE(fecha_hora_pedido)) AS dias_distintos
FROM ventas v JOIN pedidos p ON p.numero = v.numero_pedido
WHERE p.id_cliente = @cli
GROUP BY DAYOFWEEK(fecha_hora_pedido), DAYNAME(fecha_hora_pedido)
ORDER BY DAYOFWEEK(fecha_hora_pedido);
