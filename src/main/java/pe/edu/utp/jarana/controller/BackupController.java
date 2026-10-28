package pe.edu.utp.jarana.controller;

import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.utp.jarana.service.AuditoriaService;
import pe.edu.utp.jarana.service.BackupService;

@RestController
@RequestMapping("/api/backups")
public class BackupController {
    private final BackupService backups;
    private final AuditoriaService auditoria;

    public BackupController(BackupService backups, AuditoriaService auditoria) {
        this.backups = backups;
        this.auditoria = auditoria;
    }

    @GetMapping("/estado")
    public Map<String, Object> estado() {
        return backups.estado();
    }

    @PostMapping
    public Map<String, Object> crear(Principal principal) {
        Map<String, Object> r = backups.crearBackup();
        auditoria.registrar(principal.getName(), "CREAR_BACKUP", "BASE_DATOS",
                String.valueOf(r.get("archivo")), "Copia de seguridad creada desde el panel web");
        return r;
    }

    @PostMapping("/restaurar-ultimo")
    public Map<String, Object> restaurarUltimo(Principal principal) {
        Map<String, Object> r = backups.restaurarUltimoBackup();
        auditoria.registrar(principal.getName(), "RESTAURAR_BACKUP", "BASE_DATOS",
                String.valueOf(r.get("archivoRestaurado")), "Restauración ejecutada desde el panel web");
        return r;
    }
}
