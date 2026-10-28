package pe.edu.utp.jarana.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import pe.edu.utp.jarana.model.MetodoPago;

public class PagoRequest {
    @NotNull private MetodoPago metodo;
    @DecimalMin(value = "0.0", inclusive = true) private BigDecimal importeRecibido;
    private String referencia;
    public MetodoPago getMetodo() { return metodo; }
    public void setMetodo(MetodoPago metodo) { this.metodo = metodo; }
    public BigDecimal getImporteRecibido() { return importeRecibido; }
    public void setImporteRecibido(BigDecimal importeRecibido) { this.importeRecibido = importeRecibido; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
}
