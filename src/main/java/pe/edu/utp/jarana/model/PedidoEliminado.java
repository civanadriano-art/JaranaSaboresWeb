package pe.edu.utp.jarana.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos_eliminados")
public class PedidoEliminado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_eliminacion")
    private Long idEliminacion;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "numero_pedido", nullable = false)
    private Pedido pedido;

    @Column(nullable = false, length = 255)
    private String motivo;

    @Column(name = "fecha_hora_eliminacion", nullable = false)
    private LocalDateTime fechaHoraEliminacion = LocalDateTime.now();

    public Long getIdEliminacion() { return idEliminacion; }
    public void setIdEliminacion(Long idEliminacion) { this.idEliminacion = idEliminacion; }
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public LocalDateTime getFechaHoraEliminacion() { return fechaHoraEliminacion; }
    public void setFechaHoraEliminacion(LocalDateTime fechaHoraEliminacion) { this.fechaHoraEliminacion = fechaHoraEliminacion; }
}
