package pe.edu.utp.jarana.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "productos", uniqueConstraints = @UniqueConstraint(name = "uk_productos_nombre", columnNames = "nombre"))
public class Plato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String categoria = "Platos de fondo";

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private boolean disponible = true;

    @Column(nullable = false)
    private boolean activo = true;

    public Integer getCodigo() { return codigo; }
    public void setCodigo(Integer codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
