USE laboratorio_db;

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