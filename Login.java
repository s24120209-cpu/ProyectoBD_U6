package proyectobd;

import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;

public class Login extends JFrame {

    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JButton btnIngresar;
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public Login() {
        setTitle("Sistema de Laboratorio - Inicio de Sesión");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        initComponents();
    }

    private void initComponents() {
        //inicializacion y configuracion de componentes 
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
       // llamdo de clase gbc para el acomodo de los elementos 
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel lblTitulo = new JLabel("Inventario de Laboratorio", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(lblTitulo, gbc);
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        panel.add(new JLabel("Usuario:"), gbc);
        txtUsuario = new JTextField(16);
        gbc.gridx = 1;
        panel.add(txtUsuario, gbc);
        gbc.gridy = 2;
        gbc.gridx = 0;
        panel.add(new JLabel("Contraseña:"), gbc);
        txtPassword = new JPasswordField(16);
        gbc.gridx = 1;
        panel.add(txtPassword, gbc);
        btnIngresar = new JButton("Iniciar Sesión");
        btnIngresar.setFont(new Font(" ", Font.BOLD, 13));
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(14, 6, 6, 6);
        panel.add(btnIngresar, gbc);
        add(panel);
        btnIngresar.addActionListener(e -> iniciarSesion());
        txtPassword.addActionListener(e -> iniciarSesion());     }
    
    private void iniciarSesion() { // llave de aperturra  de metodo iniciar secion 
        String usuario = txtUsuario.getText();
        String password = new String(txtPassword.getPassword());
        if (usuario.trim().isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresa usuario y contraseña", "Campos vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            UsuarioSesion sesion = usuarioDAO.autenticar(usuario, password);
            if (sesion == null) {
                JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos", "Acceso denegado", JOptionPane.ERROR_MESSAGE);
                return;
            }
            abrirMenuPorRol(sesion);
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error de base de datos: " + ex.getMessage(), "Error SQL", JOptionPane.ERROR_MESSAGE);
        }
    } // termino del metodo inicias cesion 

    private void abrirMenuPorRol(UsuarioSesion sesion) {
        switch (sesion.getRol()) {
            case "administrador":
            new MenuAdmin(sesion).setVisible(true);
            break;
            case "operador":
           new MenuQuimico(sesion).setVisible(true);
                break;
            case "consulta":
                new MenuConsulta(sesion).setVisible(true);
                break;
            default:
            JOptionPane.showMessageDialog(this, "Rol no reconocido: " + sesion.getRol());
                break;
        }
    }

    // clase principal 
    
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceEmeraldDuskLookAndFeel");
        SwingUtilities.invokeLater(() -> new Login().setVisible(true));
    }
}