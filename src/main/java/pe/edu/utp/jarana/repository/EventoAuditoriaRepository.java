package pe.edu.utp.jarana.repository;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.utp.jarana.model.EventoAuditoria;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {
    List<EventoAuditoria> findTop100ByOrderByFechaEventoDesc();
    long countByFechaEventoGreaterThanEqualAndFechaEventoLessThan(LocalDateTime inicio, LocalDateTime fin);

    @Query("select count(distinct e.usuario.idUsuario) from EventoAuditoria e where e.usuario is not null")
    long contarUsuariosConActividad();
}
