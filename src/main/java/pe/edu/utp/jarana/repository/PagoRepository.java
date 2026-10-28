package pe.edu.utp.jarana.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.jarana.model.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByPedidoNumero(Integer numero);
    boolean existsByPedidoNumero(Integer numero);
    List<Pago> findAllByOrderByFechaPagoDesc();
}
