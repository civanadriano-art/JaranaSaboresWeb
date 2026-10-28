package pe.edu.utp.jarana.controller;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.utp.jarana.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UsuarioRepository usuarios;
    public AuthController(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @GetMapping("/me")
    public Map<String,Object> me(Authentication auth) {
        var u = usuarios.findByNombreUsuarioIgnoreCase(auth.getName()).orElse(null);
        return Map.of(
                "usuario", auth.getName(),
                "nombre", u == null || u.getNombre() == null ? auth.getName() : u.getNombre(),
                "rol", u == null ? "" : u.getRol().name(),
                "roles", auth.getAuthorities().stream().map(a -> a.getAuthority()).toList());
    }
}
