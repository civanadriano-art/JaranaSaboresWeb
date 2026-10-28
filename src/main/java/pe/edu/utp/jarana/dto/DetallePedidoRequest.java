package pe.edu.utp.jarana.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public class DetallePedidoRequest {
    @NotNull
    private Integer codigoPlato;

    @Min(1)
    private int cantidad;

    private String observacion;

    @DecimalMin(value = "0.0", inclusive = true)
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
