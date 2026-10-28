-- Ejecutar solo si ya existe restaurante_db de una versión anterior.
USE restaurante_db;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS nombre VARCHAR(100) NULL AFTER nombre_usuario;
ALTER TABLE productos ADD COLUMN IF NOT EXISTS categoria VARCHAR(80) DEFAULT 'Platos de fondo' AFTER nombre;
ALTER TABLE productos ADD COLUMN IF NOT EXISTS disponible BOOLEAN NOT NULL DEFAULT TRUE AFTER precio;

CREATE TABLE IF NOT EXISTS pagos (
  id_pago BIGINT AUTO_INCREMENT PRIMARY KEY,
  numero_pedido INT NOT NULL UNIQUE,
  metodo VARCHAR(20) NOT NULL,
  importe_total DECIMAL(10,2) NOT NULL,
  importe_recibido DECIMAL(10,2) NULL,
  vuelto DECIMAL(10,2) NOT NULL DEFAULT 0,
  id_usuario_operador INT NOT NULL,
  referencia VARCHAR(100),
  fecha_pago DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pago_pedido FOREIGN KEY (numero_pedido) REFERENCES pedidos(numero),
  CONSTRAINT fk_pago_operador FOREIGN KEY (id_usuario_operador) REFERENCES usuarios(id_usuario)
);

CREATE TABLE IF NOT EXISTS eventos_auditoria (
  id_evento BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_usuario INT NULL,
  accion VARCHAR(100) NOT NULL,
  entidad VARCHAR(50),
  entidad_id VARCHAR(50),
  descripcion TEXT,
  fecha_evento DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_evento_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);
