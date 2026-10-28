package pe.edu.utp.jarana.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.utp.jarana.dto.CambiarPasswordRequest;
import pe.edu.utp.jarana.repository.UsuarioRepository;
import pe.edu.utp.jarana.service.AuditoriaService;
import pe.edu.utp.jarana.service.UsuarioService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UsuarioRepository usuarios;
    private final UsuarioService usuarioService;
    private final AuditoriaService auditoria;

    public AuthController(UsuarioRepository usuarios, UsuarioService usuarioService, AuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.usuarioService = usuarioService;
        this.auditoria = auditoria;
    }

    @GetMapping("/me")
    public Map<String,Object> me(Authentication auth) {
        var u = usuarios.findByNombreUsuarioIgnoreCase(auth.getName()).orElse(null);
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("usuario", auth.getName());
        r.put("nombre", u == null || u.getNombre() == null ? auth.getName() : u.getNombre());
        r.put("rol", u == null ? "" : u.getRol().name());
        r.put("roles", auth.getAuthorities().stream().map(a -> a.getAuthority()).toList());
        r.put("debeCambiarPassword", u != null && u.isDebeCambiarPassword());
        return r;
    }

    @PostMapping("/cambiar-password")
    public Map<String,String> cambiarPassword(@Valid @RequestBody CambiarPasswordRequest req, Principal principal) {
        usuarioService.cambiarPasswordPropio(principal.getName(), req);
        auditoria.registrar(principal.getName(), "CAMBIAR_PASSWORD", "USUARIO", principal.getName(),
                "El usuario actualizó su contraseña");
        return Map.of("mensaje", "Contraseña actualizada correctamente");
    }
}
