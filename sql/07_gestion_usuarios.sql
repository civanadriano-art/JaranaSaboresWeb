USE restaurante_db;

SET @columna_existe := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'usuarios'
      AND COLUMN_NAME = 'debe_cambiar_password'
);

SET @sql := IF(
    @columna_existe = 0,
    'ALTER TABLE usuarios ADD COLUMN debe_cambiar_password BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT ''La columna debe_cambiar_password ya existe'''
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE usuarios
SET debe_cambiar_password = FALSE
WHERE nombre_usuario IN ('admin','cajero','cocina');
