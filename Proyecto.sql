DROP DATABASE IF EXISTS laboratorio_db;
CREATE DATABASE laboratorio_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE laboratorio_db;

CREATE TABLE usuarios (
usuario_id INT AUTO_INCREMENT PRIMARY KEY,
nombre_completo VARCHAR(120) NOT NULL,
nombre_usuario VARCHAR(50) NOT NULL UNIQUE,
password_hash VARCHAR(255) NOT NULL,
algoritmo_hash ENUM('SHA2', 'ARGON2ID', 'BCRYPT') NOT NULL DEFAULT 'SHA2',
rol ENUM('Administrador', 'Operador', 'Consulta') NOT NULL DEFAULT 'operador',
estado_cuenta ENUM('ACTIVA', 'BLOQUEADA', 'INACTIVA') NOT NULL DEFAULT 'ACTIVA',
creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT chk_usuario_no_vacio CHECK (CHAR_LENGTH(TRIM(nombre_usuario)) >= 3)
) ENGINE=InnoDB;

CREATE TABLE categorias_articulos (
categoria_id INT AUTO_INCREMENT PRIMARY KEY,
nombre_categoria VARCHAR(80) NOT NULL UNIQUE,
descripcion VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE articulos_laboratorio (
articulo_id INT AUTO_INCREMENT PRIMARY KEY,
codigo_barras VARCHAR(64) NOT NULL UNIQUE,
nombre VARCHAR(120) NOT NULL,
tipo_articulo ENUM('REACTIVO', 'MATERIAL', 'EQUIPO') NOT NULL,
categoria_id INT NOT NULL,
unidad_medida VARCHAR(20) NOT NULL,
stock_actual DECIMAL(12,2) NOT NULL DEFAULT 0.00,
stock_minimo DECIMAL(12,2) NOT NULL DEFAULT 5.00,
fecha_caducidad DATE NULL,
ubicacion VARCHAR(100) NOT NULL DEFAULT 'Almacén General',
estado_disponibilidad ENUM('DISPONIBLE', 'BAJO_STOCK', 'AGOTADO', 'CADUCADO') NOT NULL DEFAULT 'DISPONIBLE',
actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
CONSTRAINT fk_articulo_categoria FOREIGN KEY (categoria_id)
REFERENCES categorias_articulos(categoria_id)
ON UPDATE CASCADE ON DELETE RESTRICT,
CONSTRAINT chk_stock_no_negativo CHECK (stock_actual >= 0),
CONSTRAINT chk_stock_min_no_negativo CHECK (stock_minimo >= 0),
CONSTRAINT chk_codigo_barras_valido CHECK (CHAR_LENGTH(TRIM(codigo_barras)) > 0)
) ENGINE=InnoDB;

CREATE TABLE movimientos_inventario (
movimiento_id INT AUTO_INCREMENT PRIMARY KEY,
tipo_movimiento ENUM('ENTRADA', 'SALIDA', 'PRESTAMO', 'DEVOLUCION') NOT NULL,
fecha_movimiento DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
usuario_id INT NOT NULL,
area_o_proyecto VARCHAR(120) NULL,
observaciones VARCHAR(255) NULL,
CONSTRAINT fk_movimiento_usuario FOREIGN KEY (usuario_id)
REFERENCES usuarios(usuario_id)
ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE detalle_movimientos (
detalle_id INT AUTO_INCREMENT PRIMARY KEY,
movimiento_id INT NOT NULL,
articulo_id INT NOT NULL,
cantidad DECIMAL(12,2) NOT NULL,
CONSTRAINT fk_detalle_movimiento FOREIGN KEY (movimiento_id)
REFERENCES movimientos_inventario(movimiento_id)
ON UPDATE CASCADE ON DELETE CASCADE,
CONSTRAINT fk_detalle_articulo FOREIGN KEY (articulo_id)
REFERENCES articulos_laboratorio(articulo_id)
ON UPDATE CASCADE ON DELETE RESTRICT,
CONSTRAINT chk_cantidad_positiva CHECK (cantidad > 0)
) ENGINE=InnoDB;

CREATE TABLE auditoria_cambios (
auditoria_id BIGINT AUTO_INCREMENT PRIMARY KEY,
tabla_afectada VARCHAR(64) NOT NULL,
tipo_operacion ENUM('INSERT', 'UPDATE', 'DELETE') NOT NULL,
fecha_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
usuario_responsable VARCHAR(100) NOT NULL,
informacion_anterior JSON NULL,
informacion_nueva JSON NULL
) ENGINE=InnoDB;

DELIMITER //

CREATE TRIGGER trg_val_codigo_barras_insert
BEFORE INSERT ON articulos_laboratorio
FOR EACH ROW
BEGIN
IF NEW.codigo_barras IS NULL OR TRIM(NEW.codigo_barras) = '' THEN
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = 'Error: El código de barras no puede estar vacío.';
END IF;
SET NEW.codigo_barras = TRIM(NEW.codigo_barras);
END//

CREATE TRIGGER trg_val_codigo_barras_update
BEFORE UPDATE ON articulos_laboratorio
FOR EACH ROW
BEGIN
IF NEW.codigo_barras IS NULL OR TRIM(NEW.codigo_barras) = '' THEN
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = 'Error: El código de barras no puede estar vacío.';
END IF;
SET NEW.codigo_barras = TRIM(NEW.codigo_barras);
END//

CREATE TRIGGER trg_val_detalle_movimiento_insert
BEFORE INSERT ON detalle_movimientos
FOR EACH ROW
BEGIN
DECLARE v_tipo_mov ENUM('ENTRADA', 'SALIDA', 'PRESTAMO', 'DEVOLUCION');
DECLARE v_stock_actual DECIMAL(12,2);

IF NEW.cantidad <= 0 THEN
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = 'Error: La cantidad debe ser mayor que cero.';
END IF;

SELECT tipo_movimiento INTO v_tipo_mov
FROM movimientos_inventario
WHERE movimiento_id = NEW.movimiento_id;

SELECT stock_actual INTO v_stock_actual
FROM articulos_laboratorio
WHERE articulo_id = NEW.articulo_id;

IF v_tipo_mov IN ('SALIDA', 'PRESTAMO') AND v_stock_actual < NEW.cantidad THEN
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = 'Error: Existencias insuficientes';
END IF;
END//

CREATE TRIGGER trg_val_stock_articulo_update
BEFORE UPDATE ON articulos_laboratorio
FOR EACH ROW
BEGIN
IF NEW.stock_actual < 0 THEN
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = 'Error: El stock no puede ser negativo.';
END IF;
END//

CREATE TRIGGER trg_aud_articulos_update
AFTER UPDATE ON articulos_laboratorio
FOR EACH ROW
BEGIN
DECLARE v_user VARCHAR(100);
SET v_user = COALESCE(@app_usuario, CURRENT_USER());

INSERT INTO auditoria_cambios (
tabla_afectada, tipo_operacion, usuario_responsable,
informacion_anterior, informacion_nueva
)
VALUES (
'articulos_laboratorio',
'UPDATE',
v_user,
JSON_OBJECT(
'articulo_id', OLD.articulo_id,
'codigo_barras', OLD.codigo_barras,
'stock_actual', OLD.stock_actual,
'ubicacion', OLD.ubicacion,
'estado_disponibilidad', OLD.estado_disponibilidad
),
JSON_OBJECT(
'articulo_id', NEW.articulo_id,
'codigo_barras', NEW.codigo_barras,
'stock_actual', NEW.stock_actual,
'ubicacion', NEW.ubicacion,
'estado_disponibilidad', NEW.estado_disponibilidad
)
);
END//

CREATE TRIGGER trg_aud_movimientos_insert
AFTER INSERT ON movimientos_inventario
FOR EACH ROW
BEGIN
DECLARE v_user VARCHAR(100);
SET v_user = COALESCE(@app_usuario, CURRENT_USER());

INSERT INTO auditoria_cambios (
tabla_afectada, tipo_operacion, usuario_responsable,
informacion_anterior, informacion_nueva
)
VALUES (
'movimientos_inventario',
'INSERT',
v_user,
NULL,
JSON_OBJECT(
'movimiento_id', NEW.movimiento_id,
'tipo_movimiento', NEW.tipo_movimiento,
'usuario_id', NEW.usuario_id,
'area_o_proyecto', NEW.area_o_proyecto,
'fecha_movimiento', NEW.fecha_movimiento
)
);
END//

CREATE FUNCTION fn_existencias_por_codigo(p_codigo_barras VARCHAR(64))
RETURNS DECIMAL(12,2)
DETERMINISTIC
READS SQL DATA
BEGIN
DECLARE v_stock DECIMAL(12,2) DEFAULT 0.00;

SELECT COALESCE(stock_actual, 0.00) INTO v_stock
FROM articulos_laboratorio
WHERE codigo_barras = p_codigo_barras
LIMIT 1;

RETURN v_stock;
END//

CREATE FUNCTION fn_dias_para_caducar(p_articulo_id INT)
RETURNS INT
DETERMINISTIC
READS SQL DATA
BEGIN
DECLARE v_fecha_cad DATE;

SELECT fecha_caducidad INTO v_fecha_cad
FROM articulos_laboratorio
WHERE articulo_id = p_articulo_id;

IF v_fecha_cad IS NULL THEN
RETURN NULL;
END IF;

RETURN DATEDIFF(v_fecha_cad, CURDATE());
END//

CREATE PROCEDURE sp_consultar_articulo_por_codigo(
IN p_codigo_barras VARCHAR(64),
OUT p_nombre VARCHAR(120),
OUT p_stock_actual DECIMAL(12,2),
OUT p_estado VARCHAR(20),
OUT p_dias_caducidad INT
)
BEGIN
DECLARE v_id INT;

SELECT articulo_id, nombre, stock_actual, estado_disponibilidad
INTO v_id, p_nombre, p_stock_actual, p_estado
FROM articulos_laboratorio
WHERE codigo_barras = p_codigo_barras;

SET p_dias_caducidad = fn_dias_para_caducar(v_id);
END//

CREATE PROCEDURE sp_registrar_movimiento_barcode(
IN p_codigo_barras VARCHAR(64),
IN p_tipo_movimiento ENUM('ENTRADA', 'SALIDA', 'PRESTAMO', 'DEVOLUCION'),
IN p_cantidad DECIMAL(12,2),
IN p_usuario_id INT,
IN p_area_o_proyecto VARCHAR(120),
IN p_observaciones VARCHAR(255),
OUT p_movimiento_id INT,
OUT p_nuevo_stock DECIMAL(12,2),
OUT p_mensaje VARCHAR(255)
)
BEGIN
DECLARE v_articulo_id INT;
DECLARE v_stock_actual DECIMAL(12,2);
DECLARE v_stock_minimo DECIMAL(12,2);
DECLARE v_fecha_cad DATE;
DECLARE v_nuevo_estado VARCHAR(20);

DECLARE EXIT HANDLER FOR SQLEXCEPTION
BEGIN
ROLLBACK;
SET p_movimiento_id = NULL;
SET p_nuevo_stock = NULL;
SET p_mensaje = 'Se cancelo el movimiento por error';
END;

START TRANSACTION;

SELECT articulo_id, stock_actual, stock_minimo, fecha_caducidad
INTO v_articulo_id, v_stock_actual, v_stock_minimo, v_fecha_cad
FROM articulos_laboratorio
WHERE codigo_barras = p_codigo_barras
FOR UPDATE;

IF v_articulo_id IS NULL THEN
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = 'No existe ese código de barras';
END IF;

IF p_tipo_movimiento IN ('SALIDA', 'PRESTAMO') AND v_stock_actual < p_cantidad THEN
SIGNAL SQLSTATE '45000'
SET MESSAGE_TEXT = 'Stock insuficiente';
END IF;

INSERT INTO movimientos_inventario (
tipo_movimiento, usuario_id, area_o_proyecto, observaciones
) VALUES (
p_tipo_movimiento, p_usuario_id, p_area_o_proyecto, p_observaciones
);

SET p_movimiento_id = LAST_INSERT_ID();

INSERT INTO detalle_movimientos (movimiento_id, articulo_id, cantidad)
VALUES (p_movimiento_id, v_articulo_id, p_cantidad);

IF p_tipo_movimiento IN ('ENTRADA', 'DEVOLUCION') THEN
SET p_nuevo_stock = v_stock_actual + p_cantidad;
ELSE
SET p_nuevo_stock = v_stock_actual - p_cantidad;
END IF;

IF v_fecha_cad IS NOT NULL AND v_fecha_cad < CURDATE() THEN
SET v_nuevo_estado = 'CADUCADO';
ELSEIF p_nuevo_stock <= 0 THEN
SET v_nuevo_estado = 'AGOTADO';
ELSEIF p_nuevo_stock <= v_stock_minimo THEN
SET v_nuevo_estado = 'BAJO_STOCK';
ELSE
SET v_nuevo_estado = 'DISPONIBLE';
END IF;

UPDATE articulos_laboratorio
SET stock_actual = p_nuevo_stock,
estado_disponibilidad = v_nuevo_estado
WHERE articulo_id = v_articulo_id;

COMMIT;

SET p_mensaje = 'Movimiento guardado y existencias actualizadas';
END//

CREATE PROCEDURE sp_actualizar_ubicacion_articulo(
IN p_codigo_barras VARCHAR(64),
IN p_nueva_ubicacion VARCHAR(100),
OUT p_filas_afectadas INT
)
BEGIN
UPDATE articulos_laboratorio
SET ubicacion = p_nueva_ubicacion
WHERE codigo_barras = p_codigo_barras;
SET p_filas_afectadas = ROW_COUNT();
END//

DELIMITER ;

SET @app_usuario = 'admin_lab';

INSERT INTO usuarios (nombre_completo, nombre_usuario, password_hash, algoritmo_hash, rol, estado_cuenta) VALUES
('Dr. Simi Lares', 'admin_lab', SHA2('AdminLab#2026', 256), 'SHA2', 'administrador', 'ACTIVA'),
('Pancho Pantera', 'op_quimica', SHA2('Quimica#2026', 256), 'SHA2', 'operador', 'ACTIVA'),
('Cesar Augusto', 'consulta_inv', SHA2('Consulta#2026', 256), 'SHA2', 'consulta', 'ACTIVA');

INSERT INTO categorias_articulos (nombre_categoria, descripcion) VALUES
('Reactivos 1uímicos', 'Soluciones, ácidos, sales y reactivos analíticos'),
('Cristalería y Material', 'Matraces, pipetas, vasos de precipitado y guantes'),
('Equipo de Laboratorio', 'Balanza, micropipetas y agitadores');

INSERT INTO articulos_laboratorio
(codigo_barras, nombre, tipo_articulo, categoria_id, unidad_medida, stock_actual, stock_minimo, fecha_caducidad, ubicacion, estado_disponibilidad)
VALUES
('7501001000011', 'Ácido', 'REACTIVO', 1, 'mL', 2500.00, 500.00, DATE_ADD(CURDATE(), INTERVAL 25 DAY), 'Gabinetes inflamables', 'DISPONIBLE'),
('7501001000028', 'Etanol', 'REACTIVO', 1, 'mL', 1800.00, 400.00, DATE_ADD(CURDATE(), INTERVAL 120 DAY), 'Gabinetes solventes', 'DISPONIBLE'),
('7501001000035', 'Biuret', 'REACTIVO', 1, 'mL', 120.00, 150.00, DATE_ADD(CURDATE(), INTERVAL 12 DAY), 'Refrigerador de reactivos', 'BAJO_STOCK'),
('7501001000042', 'Matraz', 'MATERIAL', 2, 'pieza', 30.00, 10.00, NULL, 'Cristalería', 'DISPONIBLE'),
('7501001000059', 'Micropipeta', 'EQUIPO', 3, 'pieza', 6.00, 2.00, NULL, 'Vitrina de equipos ', 'DISPONIBLE');

CALL sp_registrar_movimiento_barcode('7501001000011', 'ENTRADA', 500.00, 1, 'Recepción Proveedor', 'Lote XXXX', @mov1, @stk1, @msg1);
CALL sp_registrar_movimiento_barcode('7501001000028', 'SALIDA', 350.00, 2, 'Práctica química', 'Extracción de muestras', @mov2, @stk2, @msg2);
CALL sp_registrar_movimiento_barcode('7501001000035', 'SALIDA', 40.00, 2, 'Análisis de proteínas', 'Sesión matutina', @mov3, @stk3, @msg3);

CALL sp_registrar_movimiento_barcode('7501001000011', 'SALIDA', 99999.00, 2, 'Error en la prueba', 'Excede la existencia', @mov_err, @stk_err, @msg_err);
SELECT @mov_err AS mov_fallido, @stk_err AS stock_fallido, @msg_err AS mensaje_rollback;

WITH consumo_articulos AS (
SELECT a.articulo_id, a.codigo_barras, a.nombre, c.nombre_categoria, a.unidad_medida,
SUM(dm.cantidad) AS total_consumido,
COUNT(DISTINCT m.movimiento_id) AS num_movimientos
FROM articulos_laboratorio a
INNER JOIN categorias_articulos c ON a.categoria_id = c.categoria_id
INNER JOIN detalle_movimientos dm ON a.articulo_id = dm.articulo_id
INNER JOIN movimientos_inventario m ON dm.movimiento_id = m.movimiento_id
INNER JOIN usuarios u ON m.usuario_id = u.usuario_id
WHERE m.tipo_movimiento IN ('SALIDA', 'PRESTAMO')
GROUP BY a.articulo_id, a.codigo_barras, a.nombre, c.nombre_categoria, a.unidad_medida
HAVING SUM(dm.cantidad) > 0
)
SELECT codigo_barras, nombre, nombre_categoria, total_consumido, unidad_medida, num_movimientos,
DENSE_RANK() OVER (ORDER BY total_consumido DESC) AS ranking_consumo
FROM consumo_articulos
ORDER BY total_consumido DESC;

SELECT a.codigo_barras, a.nombre, c.nombre_categoria, a.stock_actual, a.unidad_medida, a.fecha_caducidad, 
fn_dias_para_caducar(a.articulo_id) AS dias_restantes, a.ubicacion
FROM articulos_laboratorio a
INNER JOIN categorias_articulos c ON a.categoria_id = c.categoria_id
WHERE a.fecha_caducidad IS NOT NULL
AND a.fecha_caducidad <= DATE_ADD(CURDATE(), INTERVAL 30 DAY)
ORDER BY dias_restantes ASC;

SELECT * FROM auditoria_cambios ORDER BY auditoria_id DESC;