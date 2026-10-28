USE restaurante_db;

-- Corrige ventas históricas que quedaron en 0 minutos.
-- Solo actúa cuando el pedido tiene hora de inicio de preparación y hora de listo.
UPDATE ventas v
JOIN pedidos p ON p.numero = v.numero_pedido
SET v.minutos_cocina = GREATEST(
    1,
    CEIL(TIMESTAMPDIFF(SECOND, p.fecha_hora_inicio_preparacion, p.fecha_hora_listo) / 60.0)
)
WHERE p.fecha_hora_inicio_preparacion IS NOT NULL
  AND p.fecha_hora_listo IS NOT NULL
  AND (v.minutos_cocina IS NULL OR v.minutos_cocina <= 0);

SELECT
    v.numero_pedido,
    p.fecha_hora_inicio_preparacion,
    p.fecha_hora_listo,
    v.minutos_cocina
FROM ventas v
JOIN pedidos p ON p.numero = v.numero_pedido
ORDER BY v.numero_pedido DESC;
