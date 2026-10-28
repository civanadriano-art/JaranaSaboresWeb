package pe.edu.utp.jarana.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.utp.jarana.model.RolUsuario;

public class CrearUsuarioRequest {
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$", message = "El usuario debe tener entre 3 y 50 caracteres válidos")
    private String nombreUsuario;

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotNull
    private RolUsuario rol;

    @NotBlank
    @Size(min = 8, max = 72, message = "La contraseña debe tener al menos 8 caracteres")
    private String passwordTemporal;

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public RolUsuario getRol() { return rol; }
    public void setRol(RolUsuario rol) { this.rol = rol; }
    public String getPasswordTemporal() { return passwordTemporal; }
    public void setPasswordTemporal(String passwordTemporal) { this.passwordTemporal = passwordTemporal; }
}
