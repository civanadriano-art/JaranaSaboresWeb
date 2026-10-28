package pe.edu.utp.jarana.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public class CrearPedidoRequest {
    @NotBlank
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü ]{2,100}$", message = "El nombre solo debe contener letras y espacios")
    private String nombreCliente;

    @NotBlank
    @Pattern(regexp = "^[0-9]{7,9}$", message = "El teléfono debe tener entre 7 y 9 dígitos")
    private String telefono;

    @NotEmpty
    @Valid
    private List<DetallePedidoRequest> detalles;

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public List<DetallePedidoRequest> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePedidoRequest> detalles) { this.detalles = detalles; }
}
