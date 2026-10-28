USE restaurante_db;

ALTER TABLE detalle_pedido
  ADD COLUMN costo_extra DECIMAL(8,2) NOT NULL DEFAULT 0.00 AFTER observacion;

-- Los métodos de pago se almacenan en VARCHAR y el backend acepta:
-- EFECTIVO, YAPE y PLIN.
