package pe.edu.utp.jarana.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.jarana.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);
}
