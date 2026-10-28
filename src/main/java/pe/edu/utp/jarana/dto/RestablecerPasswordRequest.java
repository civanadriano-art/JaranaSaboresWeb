package pe.edu.utp.jarana.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RestablecerPasswordRequest {
    @NotBlank
    @Size(min = 8, max = 72, message = "La contraseña debe tener al menos 8 caracteres")
    private String passwordTemporal;

    public String getPasswordTemporal() { return passwordTemporal; }
    public void setPasswordTemporal(String passwordTemporal) { this.passwordTemporal = passwordTemporal; }
}
