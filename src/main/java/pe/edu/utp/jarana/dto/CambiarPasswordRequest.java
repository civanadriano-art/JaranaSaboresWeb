package pe.edu.utp.jarana.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CambiarPasswordRequest {
    @NotBlank
    private String passwordActual;

    @NotBlank
    @Size(min = 8, max = 72, message = "La nueva contraseña debe tener al menos 8 caracteres")
    private String passwordNueva;

    public String getPasswordActual() { return passwordActual; }
    public void setPasswordActual(String passwordActual) { this.passwordActual = passwordActual; }
    public String getPasswordNueva() { return passwordNueva; }
    public void setPasswordNueva(String passwordNueva) { this.passwordNueva = passwordNueva; }
}
