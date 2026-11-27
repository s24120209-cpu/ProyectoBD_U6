USE laboratorio_db;

CREATE ROLE IF NOT EXISTS 'rol_lab_admin';
CREATE ROLE IF NOT EXISTS 'rol_lab_quimico';
CREATE ROLE IF NOT EXISTS 'rol_lab_consulta';

GRANT ALL PRIVILEGES ON laboratorio_db.* TO 'rol_lab_admin' WITH GRANT OPTION;

GRANT SELECT ON laboratorio_db.articulos_laboratorio TO 'rol_lab_quimico';
GRANT SELECT ON laboratorio_db.categorias_articulos TO 'rol_lab_quimico';
GRANT EXECUTE ON PROCEDURE laboratorio_db.sp_registrar_movimiento_barcode TO 'rol_lab_quimico';
GRANT EXECUTE ON FUNCTION laboratorio_db.fn_dias_para_caducar TO 'rol_lab_quimico';

GRANT SELECT ON laboratorio_db.articulos_laboratorio TO 'rol_lab_consulta';
GRANT SELECT ON laboratorio_db.categorias_articulos TO 'rol_lab_consulta';
GRANT EXECUTE ON FUNCTION laboratorio_db.fn_existencias_por_codigo TO 'rol_lab_consulta';
GRANT EXECUTE ON PROCEDURE laboratorio_db.sp_consultar_articulo_por_codigo TO 'rol_lab_consulta';

CREATE USER IF NOT EXISTS 'lab_admin_app'@'localhost' IDENTIFIED BY 'AdminBD#2026';
CREATE USER IF NOT EXISTS 'lab_quimico_app'@'localhost' IDENTIFIED BY 'QuimicoBD#2026';
CREATE USER IF NOT EXISTS 'lab_consulta_app'@'localhost' IDENTIFIED BY 'ConsultaBD#2026';

GRANT 'rol_lab_admin' TO 'lab_admin_app'@'localhost';
GRANT 'rol_lab_quimico' TO 'lab_quimico_app'@'localhost';
GRANT 'rol_lab_consulta' TO 'lab_consulta_app'@'localhost';

SET DEFAULT ROLE 'rol_lab_admin' TO 'lab_admin_app'@'localhost';
SET DEFAULT ROLE 'rol_lab_quimico' TO 'lab_quimico_app'@'localhost';
SET DEFAULT ROLE 'rol_lab_consulta' TO 'lab_consulta_app'@'localhost';

FLUSH PRIVILEGES;