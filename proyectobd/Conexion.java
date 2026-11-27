package proyectobd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

//clase solo para conectarse con la base de datos 
public class Conexion {

    private static final String URL = "jdbc:mysql://localhost:3306/laboratorio_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Mexico_City";
    private static final String USER = "root";
    private static final String PASSWORD = "POLLOX20";

    public static Connection getConexion() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
