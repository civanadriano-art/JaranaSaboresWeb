package pe.edu.utp.jarana.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.jarana.model.Venta;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    List<Venta> findAllByOrderByFechaHoraDespachoDesc();
    List<Venta> findByFechaHoraDespachoBetweenOrderByFechaHoraDespachoDesc(LocalDateTime desde, LocalDateTime hasta);
}
