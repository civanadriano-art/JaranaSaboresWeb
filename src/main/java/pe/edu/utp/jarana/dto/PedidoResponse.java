package pe.edu.utp.jarana.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Integer numero,
        String cliente,
        String telefono,
        String estado,
        LocalDateTime fechaRegistro,
        BigDecimal total,
        List<DetalleResponse> detalles) {

    public record DetalleResponse(Integer codigo, String plato, int cantidad, BigDecimal precioUnitario, String observacion, BigDecimal costoExtra) {}
}
