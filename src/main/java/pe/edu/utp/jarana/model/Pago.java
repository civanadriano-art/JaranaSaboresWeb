package pe.edu.utp.jarana.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
public class Pago {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long idPago;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "numero_pedido", nullable = false, unique = true)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodo;

    @Column(name = "importe_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal importeTotal;

    @Column(name = "importe_recibido", precision = 10, scale = 2)
    private BigDecimal importeRecibido;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal vuelto = BigDecimal.ZERO;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_operador", nullable = false)
    private Usuario operador;

    @Column(length = 100)
    private String referencia;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago = LocalDateTime.now();

    public Long getIdPago() { return idPago; }
    public void setIdPago(Long idPago) { this.idPago = idPago; }
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
    public MetodoPago getMetodo() { return metodo; }
    public void setMetodo(MetodoPago metodo) { this.metodo = metodo; }
    public BigDecimal getImporteTotal() { return importeTotal; }
    public void setImporteTotal(BigDecimal importeTotal) { this.importeTotal = importeTotal; }
    public BigDecimal getImporteRecibido() { return importeRecibido; }
    public void setImporteRecibido(BigDecimal importeRecibido) { this.importeRecibido = importeRecibido; }
    public BigDecimal getVuelto() { return vuelto; }
    public void setVuelto(BigDecimal vuelto) { this.vuelto = vuelto; }
    public Usuario getOperador() { return operador; }
    public void setOperador(Usuario operador) { this.operador = operador; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }
}
