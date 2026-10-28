package pe.edu.utp.jarana.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DetallePedidoRequest {
    @NotNull
    private Integer codigoPlato;

    @Min(1)
    private int cantidad;

    private String observacion;

    public Integer getCodigoPlato() { return codigoPlato; }
    public void setCodigoPlato(Integer codigoPlato) { this.codigoPlato = codigoPlato; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
