USE laboratorio_db;
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