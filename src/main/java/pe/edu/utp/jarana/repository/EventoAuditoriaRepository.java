package pe.edu.utp.jarana.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.jarana.model.EventoAuditoria;

public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {
    List<EventoAuditoria> findTop100ByOrderByFechaEventoDesc();
}
