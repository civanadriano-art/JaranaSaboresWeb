package pe.edu.utp.jarana.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.jarana.model.PedidoEliminado;

public interface PedidoEliminadoRepository extends JpaRepository<PedidoEliminado, Long> {
    Optional<PedidoEliminado> findTopByOrderByIdEliminacionDesc();
    List<PedidoEliminado> findAllByOrderByIdEliminacionDesc();
}
