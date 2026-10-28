package pe.edu.utp.jarana.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.jarana.model.Plato;

public interface PlatoRepository extends JpaRepository<Plato, Integer> {
    Optional<Plato> findByNombreIgnoreCase(String nombre);
    List<Plato> findByActivoTrueOrderByNombreAsc();
}
