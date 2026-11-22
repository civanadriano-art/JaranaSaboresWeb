package pe.edu.utp.jarana.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public class DetallePedidoRequest {
    @NotNull
    private Integer codigoPlato;

    @Min(value = 1, message = "La cantidad mínima es 1")
    @Max(value = 99, message = "La cantidad máxima por plato es 99")
    private int cantidad;

    @Size(max = 255, message = "La observación no puede superar los 255 caracteres")
    private String observacion;

    @DecimalMin(value = "0.0", inclusive = true, message = "El costo extra no puede ser negativo")
    @DecimalMax(value = "9999.99", message = "El costo extra no puede superar S/ 9,999.99")
    private BigDecimal costoExtra;

    public Integer getCodigoPlato() { return codigoPlato; }
    public void setCodigoPlato(Integer codigoPlato) { this.codigoPlato = codigoPlato; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public BigDecimal getCostoExtra() { return costoExtra; }
    public void setCostoExtra(BigDecimal costoExtra) { this.costoExtra = costoExtra; }
}
