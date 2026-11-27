package proyectobd;

import proyectobd.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// esta toma datos del usuario, modifica los datos hashiados para poder mostrarlos 
public class UsuarioDAO {

    public UsuarioSesion autenticar(String nombreUsuario, String passwordPlano) throws SQLException {
        String sql = "SELECT usuario_id, nombre_completo, nombre_usuario, rol " +
                     "FROM usuarios " +
                     "WHERE nombre_usuario = ? " +
                     "  AND password_hash = SHA2(?, 256) " +
                     "  AND estado_cuenta = 'ACTIVA'";

        try (Connection conn = Conexion.getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombreUsuario.trim());
            ps.setString(2, passwordPlano);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UsuarioSesion(
                        rs.getInt("usuario_id"),
                        rs.getString("nombre_completo"),
                        rs.getString("nombre_usuario"),
                        rs.getString("rol")
                    );
                }
            }
        }
        return null;
    }
}
