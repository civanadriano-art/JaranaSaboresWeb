-- OPCIONAL: usuario limitado para la aplicación. Ejecutar como administrador de MySQL.
-- Cambia la contraseña antes de usarlo.
CREATE USER IF NOT EXISTS 'app_restaurante'@'localhost' IDENTIFIED BY 'Cambiar_Esta_Clave_2026!';
GRANT SELECT, INSERT, UPDATE, DELETE ON restaurante_db.* TO 'app_restaurante'@'localhost';
FLUSH PRIVILEGES;
