package proyectobd;

public class UsuarioSesion {
    private final int usuarioId;
    private final String nombreCompleto;
    private final String nombreUsuario;
    private final String rol;

    // esta clase solo guarda datos de la sesion del usuario 
    public UsuarioSesion(int usuarioId, String nombreCompleto, String nombreUsuario, String rol) {
        this.usuarioId = usuarioId;
        this.nombreCompleto = nombreCompleto;
        this.nombreUsuario = nombreUsuario;
        this.rol = rol;
    }

    public int getUsuarioId() { return usuarioId; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getRol() { return rol; }
}
