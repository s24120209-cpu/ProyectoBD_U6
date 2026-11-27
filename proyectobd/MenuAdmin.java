package proyectobd;

import proyectobd.Login;
import proyectobd.Conexion;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class MenuAdmin extends JFrame {
    private final UsuarioSesion sesion;
    private final DefaultTableModel modeloTabla = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {return false;
        }
    };
    //elemntos de acciones 
    private final JTable tabla = new JTable(modeloTabla);
    private final JTextField txtFiltro = new JTextField(18);
    private final JComboBox<String> cmbFiltro = new JComboBox<>(
    new String[]{"Todos", "DISPONIBLE", "BAJO_STOCK", "AGOTADO", "CADUCADO"});
    private String vistaActual = "PRODUCTOS";

    public MenuAdmin(UsuarioSesion sesion) {
        // configuracion basica de las paguinas 
        this.sesion = sesion;
        setTitle("Menú Admin - " + sesion.getNombreCompleto());
        setSize(1180, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initUI();
        cargarProductos();
        txtFiltro.requestFocusInWindow();
    }

    private void initUI() {
        setLayout(new BorderLayout(8, 8));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);

        // inicializacion de componentes
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 4));
        JButton btnMostrarProd = new JButton("Mostrar Productos");
        JButton btnIngresarProd = new JButton("Ingresar Producto");
        JButton btnModificarProd = new JButton("Modificar Producto");
        JButton btnBorrarProd = new JButton("Borrar Producto");
        JButton btnMovimiento = new JButton("Entrada / Salida");
        JButton btnHistorial = new JButton("Historial");
        JButton btnCaducidades = new JButton("Caducidades");
        JButton btnAuditoria = new JButton("Auditoría");
        JButton btnUsuarios = new JButton("Usuarios");
        JButton btnCerrarSesion = new JButton("Cerrar Sesión");
        //añadir elementos al panel 
        panelBotones.add(btnMostrarProd);
        panelBotones.add(btnIngresarProd);
         panelBotones.add(btnModificarProd);
        panelBotones.add(btnBorrarProd);
        panelBotones.add(btnMovimiento);
      panelBotones.add(btnHistorial);
        panelBotones.add(btnCaducidades);
        panelBotones.add(btnAuditoria);
        panelBotones.add(btnUsuarios);
        panelBotones.add(btnCerrarSesion);
        // darle caracteristicas al panel principal
        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton btnFiltrar = new JButton("Filtrar");
        panelFiltros.add(new JLabel("Código / Nombre:"));
          panelFiltros.add(txtFiltro);
        panelFiltros.add(new JLabel("Estado:"));
        panelFiltros.add(cmbFiltro);
        panelFiltros.add(btnFiltrar);
        JPanel panelNorte = new JPanel(new GridLayout(2, 1));
        panelNorte.add(panelBotones);
        panelNorte.add(panelFiltros);
        add(panelNorte, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        txtFiltro.addActionListener(e -> buscarConEscaner());
        btnFiltrar.addActionListener(e -> filtrarProductos());
        btnMostrarProd.addActionListener(e -> cargarProductos());
        btnIngresarProd.addActionListener(e -> ingresarProducto());
        btnModificarProd.addActionListener(e -> modificarProducto());
        btnBorrarProd.addActionListener(e -> borrarProducto());
        btnMovimiento.addActionListener(e -> registrarMovimiento());
        btnHistorial.addActionListener(e -> cargarHistorialMovimientos());
        btnCaducidades.addActionListener(e -> cargarCaducidades());
        btnAuditoria.addActionListener(e -> cargarAuditoria());
        btnUsuarios.addActionListener(e -> menuUsuarios());
        btnCerrarSesion.addActionListener(e -> {
            new Login().setVisible(true);
            dispose();
        });
    }

    private void menuUsuarios() {
        cargarUsuarios();
        String[] opciones = {"Ingresar Usuario", "Modificar Usuario", "Borrar Usuario", "Volver a Productos"};
        int sel = JOptionPane.showOptionDialog(this, "Gestión de usuarios:", "Usuarios",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[0]);
        if (sel == 0) ingresarUsuario();
        else if (sel == 1) modificarUsuario();
        else if (sel == 2) borrarUsuario();
        else if (sel == 3) cargarProductos();
    }

    private void buscarConEscaner() {
        filtrarProductos();
        if (modeloTabla.getRowCount() > 0) {
            tabla.setRowSelectionInterval(0, 0);
        }
        txtFiltro.selectAll();
    }

    private void cargarProductos() {
        vistaActual = "PRODUCTOS";
        modeloTabla.setDataVector(new Object[][]{},
        new Object[]{"ID", "Código", "Nombre", "Tipo", "Stock", "Mínimo", "Caducidad", "Ubicación", "Estado"});
        ejecutarConsulta("SELECT articulo_id, codigo_barras, nombre, tipo_articulo, stock_actual, stock_minimo, " +
                "fecha_caducidad, ubicacion, estado_disponibilidad FROM articulos_laboratorio", 9);
    }

    private void filtrarProductos() {
        vistaActual = "PRODUCTOS";
        modeloTabla.setDataVector(new Object[][]{},
                new Object[]{"ID", "Código", "Nombre", "Tipo", "Stock", "Mínimo", "Caducidad", "Ubicación", "Estado"});
        String texto = "%" + txtFiltro.getText().trim() + "%";
        String estado = cmbFiltro.getSelectedItem().toString();
        String sql = estado.equals("Todos")
                ? "SELECT articulo_id, codigo_barras, nombre, tipo_articulo, stock_actual, stock_minimo, fecha_caducidad, ubicacion, estado_disponibilidad FROM articulos_laboratorio WHERE nombre LIKE ? OR codigo_barras LIKE ?"
                : "SELECT articulo_id, codigo_barras, nombre, tipo_articulo, stock_actual, stock_minimo, fecha_caducidad, ubicacion, estado_disponibilidad FROM articulos_laboratorio WHERE (nombre LIKE ? OR codigo_barras LIKE ?) AND estado_disponibilidad = ?";

        try (Connection conn = Conexion.getConexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, texto);
            ps.setString(2, texto);
            if (!estado.equals("Todos")) {
                ps.setString(3, estado);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    modeloTabla.addRow(new Object[]{
                        rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getDate(7), rs.getString(8), rs.getString(9)
                    });
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void ingresarProducto() {
        String codigo = JOptionPane.showInputDialog(this, "Código de barras (escanea o escribe):");
        if (codigo == null || codigo.trim().isEmpty()) return;
        String nombre = JOptionPane.showInputDialog(this, "Nombre del artículo:");
        if (nombre == null || nombre.trim().isEmpty()) return;
        String tipo = (String) JOptionPane.showInputDialog(this, "Tipo:", "Tipo de Artículo",
                JOptionPane.PLAIN_MESSAGE, null, new String[]{"REACTIVO", "MATERIAL", "EQUIPO"}, "REACTIVO");
        if (tipo == null) return;
        String unidad = JOptionPane.showInputDialog(this, "Unidad de medida (mL, pieza, g):", "pieza");
        if (unidad == null) return;
        String stockIni = JOptionPane.showInputDialog(this, "Stock inicial:", "0");
        if (stockIni == null || stockIni.trim().isEmpty()) return;
        String stockMin = JOptionPane.showInputDialog(this, "Stock mínimo:", "5");
        if (stockMin == null || stockMin.trim().isEmpty()) return;
        String fechaCad = JOptionPane.showInputDialog(this, "Fecha caducidad (YYYY-MM-DD o vacío):", "");
        if (fechaCad == null) return;
        String ubicacion = JOptionPane.showInputDialog(this, "Ubicación:", "Almacén General");
        if (ubicacion == null) return;
        String sql = "INSERT INTO articulos_laboratorio " +
                "(codigo_barras, nombre, tipo_articulo, categoria_id, unidad_medida, stock_actual, stock_minimo, fecha_caducidad, ubicacion) " +
                "VALUES (?, ?, ?, 1, ?, ?, ?, ?, ?)";

        try {
            BigDecimal stockVal = new BigDecimal(stockIni.trim().replace(',', '.'));
            BigDecimal minVal = new BigDecimal(stockMin.trim().replace(',', '.'));

            Date fechaVal = null;
            if (!fechaCad.trim().isEmpty()) {
                try {
                    fechaVal = Date.valueOf(fechaCad.trim());
                } catch (IllegalArgumentException exFecha) {
                    JOptionPane.showMessageDialog(this,
                            "La fecha debe tener formato AAAA-MM-DD (ej. 2027-05-15) o dejarse vacía.",
                            "Fecha inválida", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            try (Connection conn = Conexion.getConexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
                setUsuarioAuditoria(conn);
                ps.setString(1, codigo.trim());
                ps.setString(2, nombre.trim());
                ps.setString(3, tipo);
                ps.setString(4, unidad.trim().isEmpty() ? "pieza" : unidad.trim());
                ps.setBigDecimal(5, stockVal);
                ps.setBigDecimal(6, minVal);
                if (fechaVal == null) {
                    ps.setNull(7, Types.DATE);
                } else {
                    ps.setDate(7, fechaVal);
                }
                ps.setString(8, ubicacion.trim().isEmpty() ? "Almacén General" : ubicacion.trim());
                ps.executeUpdate();
                cargarProductos();
                JOptionPane.showMessageDialog(this, "Producto registrado correctamente.");
            }
        } catch (NumberFormatException exNum) {
            JOptionPane.showMessageDialog(this,
               "Stock inicial y stock mínimo deben ser números válidos (ej. 10 o 12.5).",
                   "Número inválido", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            String detalle = ex.getMessage() != null ? ex.getMessage() : ex.toString();
            JOptionPane.showMessageDialog(this, detalle, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modificarProducto() {
        if (!vistaActual.equals("PRODUCTOS") || tabla.getSelectedRow() == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona o escanea un producto primero.");
            return;
        }
        int fila = tabla.getSelectedRow();
        int id = Integer.parseInt(modeloTabla.getValueAt(fila, 0).toString());
        String nombreActual = modeloTabla.getValueAt(fila, 2).toString();
        String ubicActual = modeloTabla.getValueAt(fila, 7).toString();
        String nuevoNombre = JOptionPane.showInputDialog(this, "Nuevo nombre:", nombreActual);
        if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) return;
        String nuevaUbic = JOptionPane.showInputDialog(this, "Nueva ubicación:", ubicActual);
        if (nuevaUbic == null || nuevaUbic.trim().isEmpty()) return;

        String sql = "UPDATE articulos_laboratorio SET nombre = ?, ubicacion = ? WHERE articulo_id = ?";
        try (Connection conn = Conexion.getConexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            setUsuarioAuditoria(conn);
            ps.setString(1, nuevoNombre.trim());
            ps.setString(2, nuevaUbic.trim());
            ps.setInt(3, id);
            ps.executeUpdate();
            cargarProductos();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void borrarProducto() {
        if (!vistaActual.equals("PRODUCTOS") || tabla.getSelectedRow() == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto para borrar.");
            return;
        }
        int fila = tabla.getSelectedRow();
        int id = Integer.parseInt(modeloTabla.getValueAt(fila, 0).toString());
        String nombre = modeloTabla.getValueAt(fila, 2).toString();

        String[] opciones = {
            "1. Dejar stock en 0 con AGOTADO",
            "2. Borrar todo del producto",
            "Cancelar"
        };
        int seleccion = JOptionPane.showOptionDialog(
                this,
                "¿Qué deseas hacer con '" + nombre + "' (ID: " + id + ")?",
                "Borrar o Agotar Producto",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]
        );
        if (seleccion == 2 || seleccion == JOptionPane.CLOSED_OPTION) {
            return;
        }
        if (seleccion == 0) {
            String sqlUpdate = "UPDATE articulos_laboratorio SET stock_actual = 0.00, estado_disponibilidad = 'AGOTADO' WHERE articulo_id = ?";
            try (Connection conn = Conexion.getConexion()) {
                setUsuarioAuditoria(conn);
                try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                ps.setInt(1, id);
                ps.executeUpdate();
                 cargarProductos();
             JOptionPane.showMessageDialog(this, "Producto actualizado: stock en 0.00 y estado AGOTADO.");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        int confirmar = JOptionPane.showConfirmDialog(
                this,
                "Esto eliminará el producto '" + nombre + "' y sus detalles de movimiento asociados. ¿Continuar?",
                "Confirmar borrado total",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirmar != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = Conexion.getConexion()) {
            conn.setAutoCommit(false);
            try {
                setUsuarioAuditoria(conn);
                try (PreparedStatement psDet = conn.prepareStatement("DELETE FROM detalle_movimientos WHERE articulo_id = ?")) {
                    psDet.setInt(1, id);
                    psDet.executeUpdate();
                }
                try (PreparedStatement psArt = conn.prepareStatement("DELETE FROM articulos_laboratorio WHERE articulo_id = ?")) {
                    psArt.setInt(1, id);
                    psArt.executeUpdate();
                }
                conn.commit();
                cargarProductos();
                JOptionPane.showMessageDialog(this, "Producto y sus registros asociados eliminados.");
            } catch (SQLException ex) {
                conn.rollback();
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void registrarMovimiento() {
        String codigoDefecto = (vistaActual.equals("PRODUCTOS") && tabla.getSelectedRow() != -1)
                ? modeloTabla.getValueAt(tabla.getSelectedRow(), 1).toString() : "";
        String codigo = JOptionPane.showInputDialog(this, "Código de barras (escanea o escribe):", codigoDefecto);
        if (codigo == null || codigo.trim().isEmpty()) return;
        String tipo = (String) JOptionPane.showInputDialog(this, "Tipo de movimiento:", "Movimiento",
         JOptionPane.PLAIN_MESSAGE, null, new String[]{"ENTRADA", "SALIDA", "PRESTAMO", "DEVOLUCION"}, "SALIDA");
        if (tipo == null) return;
        String cantStr = JOptionPane.showInputDialog(this, "Cantidad:", "1");
        if (cantStr == null || cantStr.trim().isEmpty()) return;
        String area = JOptionPane.showInputDialog(this, "Área o proyecto:", "Laboratorio");
        if (area == null) return;

        String callSql = "{CALL sp_registrar_movimiento_barcode(?, ?, ?, ?, ?, ?, ?, ?, ?)}";
        try (Connection conn = Conexion.getConexion()) {
            setUsuarioAuditoria(conn);
            try (CallableStatement cs = conn.prepareCall(callSql)) {
                cs.setString(1, codigo.trim());
                cs.setString(2, tipo);
                cs.setBigDecimal(3, new BigDecimal(cantStr.trim().replace(',', '.')));
                cs.setInt(4, sesion.getUsuarioId());
                cs.setString(5, area.trim().isEmpty() ? "Laboratorio" : area.trim());
                cs.setString(6, "Registrado desde MenuAdmin");
                cs.registerOutParameter(7, Types.INTEGER);
                cs.registerOutParameter(8, Types.DECIMAL);
                cs.registerOutParameter(9, Types.VARCHAR);
                cs.execute();
                JOptionPane.showMessageDialog(this, cs.getString(9) + "\nNuevo stock: " + cs.getBigDecimal(8));
                cargarProductos();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarHistorialMovimientos() {
        vistaActual = "HISTORIAL";
        modeloTabla.setDataVector(new Object[][]{},
                new Object[]{"Fecha", "Evento", "Código / Detalle", "Artículo / Info", "Cantidad", "Usuario / Área"});
        String sql =
                "SELECT m.fecha_movimiento AS fecha, " +
                "       m.tipo_movimiento AS evento, " +
                "       a.codigo_barras AS detalle, " +
                "       CONCAT(a.nombre, ' (', COALESCE(m.observaciones, ''), ')') AS info, " +
                "       dm.cantidad AS cantidad, " +
                "       CONCAT(u.nombre_usuario, ' - ', COALESCE(m.area_o_proyecto, 'Lab')) AS origen " +
                "FROM movimientos_inventario m " +
                "INNER JOIN detalle_movimientos dm ON m.movimiento_id = dm.movimiento_id " +
                "INNER JOIN articulos_laboratorio a ON dm.articulo_id = a.articulo_id " +
                "INNER JOIN usuarios u ON m.usuario_id = u.usuario_id " +
                "UNION ALL " +
                "SELECT ac.fecha_hora AS fecha, " +
                "       CONCAT(ac.tipo_operacion, '_ARTICULO') AS evento, " +
                "       COALESCE(JSON_UNQUOTE(JSON_EXTRACT(ac.informacion_nueva, '$.codigo_barras')), " +
                "                JSON_UNQUOTE(JSON_EXTRACT(ac.informacion_anterior, '$.codigo_barras')), '-') AS detalle, " +
                "       COALESCE(JSON_UNQUOTE(JSON_EXTRACT(ac.informacion_nueva, '$.nombre')), " +
                "                JSON_UNQUOTE(JSON_EXTRACT(ac.informacion_anterior, '$.nombre')), ac.tabla_afectada) AS info, " +
                "       COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(ac.informacion_nueva, '$.stock_actual')) AS DECIMAL(12,2)), " +
                "                CAST(JSON_UNQUOTE(JSON_EXTRACT(ac.informacion_anterior, '$.stock_actual')) AS DECIMAL(12,2)), 0.00) AS cantidad, " +
                "       ac.usuario_responsable AS origen " +
                "FROM auditoria_cambios ac " +
                "WHERE ac.tabla_afectada = 'articulos_laboratorio' " +
                "  AND ac.tipo_operacion IN ('INSERT', 'DELETE') " +
                "ORDER BY fecha DESC";
        ejecutarConsulta(sql, 6);  }
    
    
    private void cargarCaducidades() {
        vistaActual = "CADUCIDADES";
        modeloTabla.setDataVector(new Object[][]{},
       new Object[]{"Código", "Nombre", "Stock", "Caducidad", "Días Restantes", "Ubicación"});
        ejecutarConsulta("SELECT codigo_barras, nombre, stock_actual, fecha_caducidad, " +
                "fn_dias_para_caducar(articulo_id) AS dias_restantes, ubicacion " +
                "FROM articulos_laboratorio " +
                "WHERE fecha_caducidad IS NOT NULL AND fecha_caducidad <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) " +
                "ORDER BY dias_restantes ASC", 6);
    }

    private void cargarAuditoria() {
        vistaActual = "AUDITORIA";
        modeloTabla.setDataVector(new Object[][]{},
                new Object[]{"ID", "Fecha / Hora", "Tabla", "Operación", "ID Registro", "Usuario", "Anterior", "Nuevo"});
        String sql = "SELECT auditoria_id, fecha_hora, tabla_afectada, tipo_operacion, " +
                     "COALESCE(JSON_UNQUOTE(JSON_EXTRACT(informacion_nueva, '$.articulo_id')), " +
                     "         JSON_UNQUOTE(JSON_EXTRACT(informacion_nueva, '$.movimiento_id')), " +
                     "         JSON_UNQUOTE(JSON_EXTRACT(informacion_anterior, '$.articulo_id')), '-') AS id_registro, " +
                     "usuario_responsable, informacion_anterior, informacion_nueva " +
                     "FROM auditoria_cambios ORDER BY auditoria_id DESC";
        ejecutarConsulta(sql, 8);
    }

    private void cargarUsuarios() {
        vistaActual = "USUARIOS";
        modeloTabla.setDataVector(new Object[][]{},
         new Object[]{"ID", "Nombre Completo", "Usuario", "Rol", "Estado"});
        ejecutarConsulta("SELECT usuario_id, nombre_completo, nombre_usuario, rol, estado_cuenta FROM usuarios", 5);
    }
    
    
    private void ingresarUsuario() {
        String nombre = JOptionPane.showInputDialog(this, "Nombre completo:");
        if (nombre == null || nombre.trim().isEmpty()) return;
        String user = JOptionPane.showInputDialog(this, "Nombre de usuario:");
        if (user == null || user.trim().isEmpty()) return;
        String pass = JOptionPane.showInputDialog(this, "Contraseña:");
        if (pass == null || pass.trim().isEmpty()) return;
        String rol = (String) JOptionPane.showInputDialog(this, "Rol:", "Rol de Usuario",
                JOptionPane.PLAIN_MESSAGE, null, new String[]{"administrador", "operador", "consulta"}, "operador");
        if (rol == null) return;
        
        
        
        String sql = "INSERT INTO usuarios (nombre_completo, nombre_usuario, password_hash, algoritmo_hash, rol, estado_cuenta) " +
                "VALUES (?, ?, SHA2(?, 256), 'SHA2', ?, 'ACTIVA')";
        try (Connection conn = Conexion.getConexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            ps.setString(2, user.trim());
            ps.setString(3, pass);
            ps.setString(4, rol);
            ps.executeUpdate();
            cargarUsuarios();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modificarUsuario() {
        if (!vistaActual.equals("USUARIOS") || tabla.getSelectedRow() == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario en la tabla.");
            return;
        }
        int fila = tabla.getSelectedRow();
        int id = Integer.parseInt(modeloTabla.getValueAt(fila, 0).toString());
        String rolActual = modeloTabla.getValueAt(fila, 3).toString();
        String estadoActual = modeloTabla.getValueAt(fila, 4).toString();
        String nuevoRol = (String) JOptionPane.showInputDialog(this, "Nuevo rol:", "Rol",
                JOptionPane.PLAIN_MESSAGE, null, new String[]{"administrador", "operador", "consulta"}, rolActual);
        if (nuevoRol == null) return;
        String nuevoEstado = (String) JOptionPane.showInputDialog(this, "Estado cuenta:", "Estado",
                JOptionPane.PLAIN_MESSAGE, null, new String[]{"ACTIVA", "BLOQUEADA", "INACTIVA"}, estadoActual);
        if (nuevoEstado == null) return;

        
        
        String sql = "UPDATE usuarios SET rol = ?, estado_cuenta = ? WHERE usuario_id = ?";
        try (Connection conn = Conexion.getConexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoRol);
            ps.setString(2, nuevoEstado);
            ps.setInt(3, id);
            ps.executeUpdate();
            cargarUsuarios();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void borrarUsuario() {
        if (!vistaActual.equals("USUARIOS") || tabla.getSelectedRow() == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario en la tabla.");
            return;
        }
        int fila = tabla.getSelectedRow();
        int id = Integer.parseInt(modeloTabla.getValueAt(fila, 0).toString());
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar usuario seleccionado?", "Confirmar",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM usuarios WHERE usuario_id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            cargarUsuarios();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setUsuarioAuditoria(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SET @app_usuario = ?")) {
            ps.setString(1, sesion.getNombreUsuario());
            ps.execute();
        }
    }

    private void ejecutarConsulta(String sql, int columnas) {
        try (Connection conn = Conexion.getConexion(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Object[] fila = new Object[columnas];
                for (int i = 0; i < columnas; i++) {
                    fila[i] = rs.getObject(i + 1);
                }
                modeloTabla.addRow(fila);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }
}
