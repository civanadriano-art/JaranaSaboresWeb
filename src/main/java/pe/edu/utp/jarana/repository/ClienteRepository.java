package pe.edu.utp.jarana.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.jarana.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    Optional<Cliente> findFirstByTelefono(String telefono);
}
