package proyectobd;

import proyectobd.Login;
import proyectobd.Conexion;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class MenuQuimico extends JFrame {
    private final UsuarioSesion sesion;
    private final DefaultTableModel modeloTabla = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modeloTabla);
    private final JTextField txtFiltro = new JTextField(18);
    private final JComboBox<String> cmbFiltro = new JComboBox<>(
     new String[]{"Todos", "DISPONIBLE", "BAJO_STOCK", "AGOTADO", "CADUCADO"});
    private String vistaActual = "PRODUCTOS";
    public MenuQuimico(UsuarioSesion sesion)  {
        this.sesion = sesion;
        setTitle("Menú Químico - " + sesion.getNombreCompleto());
        setSize(1100, 650);
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
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        JButton btnMostrarProd = new JButton("Mostrar Producto");
        JButton btnSalida = new JButton("Registrar Salida");
        JButton btnCaducidades = new JButton("Caducidades");
        JButton btnCerrarSesion = new JButton("Cerrar Sesión");
        panelBotones.add(btnMostrarProd);
        panelBotones.add(btnSalida);
        panelBotones.add(btnCaducidades);
        panelBotones.add(btnCerrarSesion);
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
        btnSalida.addActionListener(e -> registrarSalida());
        btnCaducidades.addActionListener(e -> cargarCaducidades());
        btnCerrarSesion.addActionListener(e -> {
            new Login().setVisible(true);
            dispose();
        });
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
                    });}}
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

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

    private void registrarSalida() {
        String codigoDefecto = (vistaActual.equals("PRODUCTOS") && tabla.getSelectedRow() != -1)
                ? modeloTabla.getValueAt(tabla.getSelectedRow(), 1).toString() : "";
        String codigo = JOptionPane.showInputDialog(this, "Código de barras para SALIDA (escanea o escribe):", codigoDefecto);
        if (codigo == null || codigo.trim().isEmpty()) return;
        String cantStr = JOptionPane.showInputDialog(this, "Cantidad a retirar:", "1");
        if (cantStr == null || cantStr.trim().isEmpty()) return;
        String area = JOptionPane.showInputDialog(this, "Área o proyecto:", "Laboratorio");
        if (area == null) return;
        String callSql = "{CALL sp_registrar_movimiento_barcode(?, ?, ?, ?, ?, ?, ?, ?, ?)}";
        try {
            BigDecimal cantidad = new BigDecimal(cantStr.trim().replace(',', '.'));
            try (Connection conn = Conexion.getConexion()) {
                try (PreparedStatement psUser = conn.prepareStatement("SET @app_usuario = ?")) {
                    psUser.setString(1, sesion.getNombreUsuario());
                    psUser.execute();
                }
                try (CallableStatement cs = conn.prepareCall(callSql)) {
                    cs.setString(1, codigo.trim());
                    cs.setString(2, "SALIDA");
                    cs.setBigDecimal(3, cantidad);
                    cs.setInt(4, sesion.getUsuarioId());
                    cs.setString(5, area.trim().isEmpty() ? "Laboratorio" : area.trim());
                    cs.setString(6, "Salida registrada desde MenuQuimico");
                    cs.registerOutParameter(7, Types.INTEGER);
                    cs.registerOutParameter(8, Types.DECIMAL);
                    cs.registerOutParameter(9, Types.VARCHAR);
                    cs.execute();

                    JOptionPane.showMessageDialog(this, cs.getString(9) + "\nNuevo stock: " + cs.getBigDecimal(8));
                    cargarProductos();
                }
            }
        } catch (NumberFormatException exNum) {
            JOptionPane.showMessageDialog(this, "Ingresa una cantidad numérica válida.", "Cantidad inválida", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            String detalle = ex.getMessage() != null ? ex.getMessage() : ex.toString();
            JOptionPane.showMessageDialog(this, detalle, "Error", JOptionPane.ERROR_MESSAGE);
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