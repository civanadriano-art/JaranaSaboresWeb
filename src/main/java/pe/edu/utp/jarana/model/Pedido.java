package pe.edu.utp.jarana.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer numero;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 30)
    private EstadoPedido estado = EstadoPedido.PENDIENTE;

    @Column(name = "fecha_hora_registro", nullable = false)
    private LocalDateTime fechaHoraRegistro = LocalDateTime.now();

    @Column(name = "fecha_hora_inicio_preparacion")
    private LocalDateTime fechaHoraInicioPreparacion;

    @Column(name = "fecha_hora_listo")
    private LocalDateTime fechaHoraListo;

    @Column(name = "fecha_hora_despacho")
    private LocalDateTime fechaHoraDespacho;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<DetallePedido> detalles = new ArrayList<>();

    public void agregarDetalle(DetallePedido detalle) {
        detalle.setPedido(this);
        detalles.add(detalle);
    }

    public void limpiarDetalles() {
        detalles.clear();
    }

    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public EstadoPedido getEstado() { return estado; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }
    public LocalDateTime getFechaHoraRegistro() { return fechaHoraRegistro; }
    public void setFechaHoraRegistro(LocalDateTime fechaHoraRegistro) { this.fechaHoraRegistro = fechaHoraRegistro; }
    public LocalDateTime getFechaHoraInicioPreparacion() { return fechaHoraInicioPreparacion; }
    public void setFechaHoraInicioPreparacion(LocalDateTime fechaHoraInicioPreparacion) { this.fechaHoraInicioPreparacion = fechaHoraInicioPreparacion; }
    public LocalDateTime getFechaHoraListo() { return fechaHoraListo; }
    public void setFechaHoraListo(LocalDateTime fechaHoraListo) { this.fechaHoraListo = fechaHoraListo; }
    public LocalDateTime getFechaHoraDespacho() { return fechaHoraDespacho; }
    public void setFechaHoraDespacho(LocalDateTime fechaHoraDespacho) { this.fechaHoraDespacho = fechaHoraDespacho; }
    public List<DetallePedido> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePedido> detalles) { this.detalles = detalles; }
}
