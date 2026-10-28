package pe.edu.utp.jarana.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.jarana.model.EventoAuditoria;
import pe.edu.utp.jarana.service.AuditoriaService;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {
    private final AuditoriaService service;
    public AuditoriaController(AuditoriaService service) { this.service = service; }
    @GetMapping public List<EventoAuditoria> listar() { return service.listar(); }
}
