package pe.edu.utp.jarana.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_auditoria")
public class EventoAuditoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Long idEvento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String accion;

    @Column(length = 50)
    private String entidad;

    @Column(name = "entidad_id", length = 50)
    private String entidadId;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaEvento = LocalDateTime.now();

    public Long getIdEvento() { return idEvento; }
    public void setIdEvento(Long idEvento) { this.idEvento = idEvento; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }
    public String getEntidadId() { return entidadId; }
    public void setEntidadId(String entidadId) { this.entidadId = entidadId; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDateTime getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(LocalDateTime fechaEvento) { this.fechaEvento = fechaEvento; }
}
