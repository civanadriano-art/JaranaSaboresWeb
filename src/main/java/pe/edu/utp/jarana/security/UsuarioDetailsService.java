package pe.edu.utp.jarana.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pe.edu.utp.jarana.model.Usuario;
import pe.edu.utp.jarana.repository.UsuarioRepository;

@Service
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository repo;

    public UsuarioDetailsService(UsuarioRepository repo) { this.repo = repo; }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario u = repo.findByNombreUsuarioIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return User.withUsername(u.getNombreUsuario())
                .password(u.getPasswordHash())
                .roles(u.getRol().name())
                .disabled(!u.isActivo())
                .build();
    }
}
