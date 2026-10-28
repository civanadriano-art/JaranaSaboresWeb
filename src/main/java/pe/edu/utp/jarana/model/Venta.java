package pe.edu.utp.jarana.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventas")
public class Venta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Long idVenta;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "numero_pedido", nullable = false, unique = true)
    private Pedido pedido;

    @Column(name = "fecha_hora_pedido", nullable = false)
    private LocalDateTime fechaHoraPedido;

    @Column(name = "fecha_hora_despacho", nullable = false)
    private LocalDateTime fechaHoraDespacho;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "minutos_cocina")
    private Long minutosCocina;

    public Long getIdVenta() { return idVenta; }
    public void setIdVenta(Long idVenta) { this.idVenta = idVenta; }
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
    public LocalDateTime getFechaHoraPedido() { return fechaHoraPedido; }
    public void setFechaHoraPedido(LocalDateTime fechaHoraPedido) { this.fechaHoraPedido = fechaHoraPedido; }
    public LocalDateTime getFechaHoraDespacho() { return fechaHoraDespacho; }
    public void setFechaHoraDespacho(LocalDateTime fechaHoraDespacho) { this.fechaHoraDespacho = fechaHoraDespacho; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public Long getMinutosCocina() { return minutosCocina; }
    public void setMinutosCocina(Long minutosCocina) { this.minutosCocina = minutosCocina; }
}
