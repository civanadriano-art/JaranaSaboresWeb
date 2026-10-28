package pe.edu.utp.jarana.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.jarana.model.EstadoPedido;
import pe.edu.utp.jarana.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    List<Pedido> findByEstadoOrderByNumeroAsc(EstadoPedido estado);
    long countByEstadoIn(List<EstadoPedido> estados);
}
