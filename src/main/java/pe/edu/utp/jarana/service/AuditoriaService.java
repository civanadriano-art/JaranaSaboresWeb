package pe.edu.utp.jarana.service;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import pe.edu.utp.jarana.model.EventoAuditoria;
import pe.edu.utp.jarana.model.Usuario;
import pe.edu.utp.jarana.repository.EventoAuditoriaRepository;
import pe.edu.utp.jarana.repository.UsuarioRepository;

@Service
public class AuditoriaService {
    private final EventoAuditoriaRepository repo;
    private final UsuarioRepository usuarios;
    public AuditoriaService(EventoAuditoriaRepository repo, UsuarioRepository usuarios) { this.repo = repo; this.usuarios = usuarios; }

    public void registrar(String usuario, String accion, String entidad, String entidadId, String descripcion) {
        EventoAuditoria e = new EventoAuditoria();
        Usuario u = usuario == null ? null : usuarios.findByNombreUsuarioIgnoreCase(usuario).orElse(null);
        e.setUsuario(u); e.setAccion(accion); e.setEntidad(entidad); e.setEntidadId(entidadId); e.setDescripcion(descripcion);
        repo.save(e);
    }
    public List<EventoAuditoria> listar() { return repo.findTop100ByOrderByFechaEventoDesc(); }

    public Map<String, Long> resumen() {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = inicio.plusDays(1);
        return Map.of(
                "totalEventos", repo.count(),
                "eventosHoy", repo.countByFechaEventoGreaterThanEqualAndFechaEventoLessThan(inicio, fin),
                "usuariosConActividad", repo.contarUsuariosConActividad());
    }
}
