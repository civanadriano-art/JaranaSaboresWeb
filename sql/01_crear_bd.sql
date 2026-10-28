-- =============================================================
-- LA JARANA DE SABORES - MySQL 8+
-- Frontend: HTML + CSS + JavaScript + Bootstrap
-- Backend: Java + Spring Boot
-- =============================================================
CREATE DATABASE IF NOT EXISTS restaurante_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE restaurante_db;

CREATE TABLE IF NOT EXISTS usuarios (
  id_usuario INT AUTO_INCREMENT PRIMARY KEY,
  nombre_usuario VARCHAR(50) NOT NULL UNIQUE,
  nombre VARCHAR(100),
  password_hash VARCHAR(255) NOT NULL,
  password_sal VARCHAR(255) NOT NULL DEFAULT 'BCrypt',
  rol VARCHAR(20) NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  intentos_fallidos INT NOT NULL DEFAULT 0,
  bloqueado_hasta DATETIME NULL,
  ultimo_acceso DATETIME NULL,
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS clientes (
  id_cliente INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  telefono VARCHAR(9) NOT NULL,
  fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_cliente_telefono (telefono)
);

CREATE TABLE IF NOT EXISTS productos (
  codigo INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL UNIQUE,
  categoria VARCHAR(80) DEFAULT 'Platos de fondo',
  precio DECIMAL(8,2) NOT NULL,
  disponible BOOLEAN NOT NULL DEFAULT TRUE,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  CHECK (precio >= 0)
);

CREATE TABLE IF NOT EXISTS pedidos (
  numero INT AUTO_INCREMENT PRIMARY KEY,
  id_cliente INT NOT NULL,
  estado VARCHAR(30) NOT NULL DEFAULT 'Pendiente',
  fecha_hora_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_hora_inicio_preparacion DATETIME NULL,
  fecha_hora_listo DATETIME NULL,
  fecha_hora_despacho DATETIME NULL,
  CONSTRAINT fk_pedido_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente),
  INDEX idx_pedido_estado (estado),
  INDEX idx_pedido_fecha (fecha_hora_registro)
);

CREATE TABLE IF NOT EXISTS detalle_pedido (
  id_detalle INT AUTO_INCREMENT PRIMARY KEY,
  numero_pedido INT NOT NULL,
  codigo_producto INT NOT NULL,
  cantidad INT NOT NULL,
  precio_unitario DECIMAL(8,2) NOT NULL,
  observacion VARCHAR(255),
  CONSTRAINT fk_detalle_pedido FOREIGN KEY (numero_pedido) REFERENCES pedidos(numero),
  CONSTRAINT fk_detalle_producto FOREIGN KEY (codigo_producto) REFERENCES productos(codigo),
  CHECK (cantidad > 0),
  CHECK (precio_unitario >= 0)
);

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
  CONSTRAINT fk_pago_operador FOREIGN KEY (id_usuario_operador) REFERENCES usuarios(id_usuario),
  CHECK (importe_total >= 0),
  CHECK (vuelto >= 0)
);

CREATE TABLE IF NOT EXISTS ventas (
  id_venta BIGINT AUTO_INCREMENT PRIMARY KEY,
  numero_pedido INT NOT NULL UNIQUE,
  fecha_hora_pedido DATETIME NOT NULL,
  fecha_hora_despacho DATETIME NOT NULL,
  total DECIMAL(10,2) NOT NULL,
  minutos_cocina BIGINT NULL,
  CONSTRAINT fk_venta_pedido FOREIGN KEY (numero_pedido) REFERENCES pedidos(numero)
);

CREATE TABLE IF NOT EXISTS pedidos_eliminados (
  id_eliminacion BIGINT AUTO_INCREMENT PRIMARY KEY,
  numero_pedido INT NOT NULL,
  motivo VARCHAR(255) NOT NULL,
  fecha_hora_eliminacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_eliminado_pedido FOREIGN KEY (numero_pedido) REFERENCES pedidos(numero)
);

CREATE TABLE IF NOT EXISTS eventos_auditoria (
  id_evento BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_usuario INT NULL,
  accion VARCHAR(100) NOT NULL,
  entidad VARCHAR(50),
  entidad_id VARCHAR(50),
  descripcion TEXT,
  fecha_evento DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_evento_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
  INDEX idx_evento_fecha (fecha_evento)
);

-- Los usuarios iniciales y platos de demostración son creados por Spring Boot
-- mediante DataInitializer para que las contraseñas se guarden con BCrypt.
