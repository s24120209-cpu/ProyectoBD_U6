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
