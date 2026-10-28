-- LA JARANA DE SABORES
-- Actualización: teléfono opcional en clientes
USE restaurante_db;

ALTER TABLE clientes
  MODIFY telefono VARCHAR(9) NULL;
