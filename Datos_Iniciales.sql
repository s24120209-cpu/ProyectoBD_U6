USE laboratorio_db;

SET @app_usuario = 'admin_lab';

INSERT INTO usuarios (nombre_completo, nombre_usuario, password_hash, algoritmo_hash, rol, estado_cuenta) VALUES
('Dr. Simi Lares', 'admin_lab', SHA2('AdminLab#2026', 256), 'SHA2', 'administrador', 'ACTIVA'),
('Pancho Pantera', 'op_quimica', SHA2('Quimica#2026', 256), 'SHA2', 'operador', 'ACTIVA'),
('Cesar Augusto', 'consulta_inv', SHA2('Consulta#2026', 256), 'SHA2', 'consulta', 'ACTIVA');

INSERT INTO categorias_articulos (nombre_categoria, descripcion) VALUES
('Reactivos Químicos', 'Soluciones, ácidos, sales y reactivos analíticos'),
('Cristalería y Material', 'Matraces pipetas, vasos de precipitado y guantes'),
('Equipo de Laboratorio', 'Balanza micropipetas y agitadores');

INSERT INTO articulos_laboratorio
(codigo_barras, nombre, tipo_articulo, categoria_id, unidad_medida, stock_actual, stock_minimo, fecha_caducidad, ubicacion, estado_disponibilidad)
VALUES
('7501001000011', 'Ácido', 'REACTIVO', 1, 'mL', 2500.00, 500.00, DATE_ADD(CURDATE(), INTERVAL 25 DAY), 'Gabinetes inflamables', 'DISPONIBLE'),
('7501001000028', 'Etanol', 'REACTIVO', 1, 'mL', 1800.00, 400.00, DATE_ADD(CURDATE(), INTERVAL 120 DAY), 'Gabinetes solventes', 'DISPONIBLE'),
('7501001000035', 'Biuret', 'REACTIVO', 1, 'mL', 120.00, 150.00, DATE_ADD(CURDATE(), INTERVAL 12 DAY), 'Refrigerador de reactivos', 'BAJO_STOCK'),
('7501001000042', 'Matraz', 'MATERIAL', 2, 'pieza', 30.00, 10.00, NULL, 'Cristalería', 'DISPONIBLE'),
('7501001000059', 'Micropipeta', 'EQUIPO', 3, 'pieza', 6.00, 2.00, NULL, 'Vitrina de equipos', 'DISPONIBLE');