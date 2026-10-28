package pe.edu.utp.jarana.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.jarana.dto.CrearUsuarioRequest;
import pe.edu.utp.jarana.dto.RestablecerPasswordRequest;
import pe.edu.utp.jarana.dto.UsuarioResponse;
import pe.edu.utp.jarana.service.AuditoriaService;
import pe.edu.utp.jarana.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService service;
    private final AuditoriaService auditoria;

    public UsuarioController(UsuarioService service, AuditoriaService auditoria) {
        this.service = service;
        this.auditoria = auditoria;
    }

    @GetMapping
    public List<UsuarioResponse> listar() { return service.listar(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody CrearUsuarioRequest req, Principal principal) {
        UsuarioResponse r = service.crear(req);
        auditoria.registrar(principal.getName(), "CREAR_USUARIO", "USUARIO", String.valueOf(r.idUsuario()),
                "Usuario " + r.nombreUsuario() + " creado con rol " + r.rol());
        return r;
    }

    @PostMapping("/{id}/estado")
    public UsuarioResponse estado(@PathVariable Integer id, @RequestBody Map<String, Boolean> body, Principal principal) {
        boolean activo = Boolean.TRUE.equals(body.get("activo"));
        UsuarioResponse r = service.cambiarEstado(id, activo, principal.getName());
        auditoria.registrar(principal.getName(), activo ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO", "USUARIO",
                String.valueOf(id), "Estado actualizado para " + r.nombreUsuario());
        return r;
    }

    @PostMapping("/{id}/desbloquear")
    public UsuarioResponse desbloquear(@PathVariable Integer id, Principal principal) {
        UsuarioResponse r = service.desbloquear(id);
        auditoria.registrar(principal.getName(), "DESBLOQUEAR_USUARIO", "USUARIO", String.valueOf(id),
                "Cuenta desbloqueada: " + r.nombreUsuario());
        return r;
    }

    @PostMapping("/{id}/restablecer-password")
    public UsuarioResponse restablecer(@PathVariable Integer id, @Valid @RequestBody RestablecerPasswordRequest req, Principal principal) {
        UsuarioResponse r = service.restablecerPassword(id, req.getPasswordTemporal());
        auditoria.registrar(principal.getName(), "RESTABLECER_PASSWORD", "USUARIO", String.valueOf(id),
                "Contraseña temporal asignada a " + r.nombreUsuario());
        return r;
    }
}
