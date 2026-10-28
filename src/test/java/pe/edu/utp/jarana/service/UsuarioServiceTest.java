package pe.edu.utp.jarana.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pe.edu.utp.jarana.model.RolUsuario;
import pe.edu.utp.jarana.model.Usuario;
import pe.edu.utp.jarana.repository.UsuarioRepository;

class UsuarioServiceTest {
    @Test
    void restablecimientoGuardaBcryptYObligaCambio() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        Usuario u = new Usuario();
        u.setIdUsuario(7);
        u.setNombreUsuario("caja2");
        u.setNombre("Caja Dos");
        u.setRol(RolUsuario.CAJERO);
        u.setActivo(true);
        u.setPasswordHash(new BCryptPasswordEncoder().encode("Anterior123"));
        when(repo.findById(7)).thenReturn(Optional.of(u));
        when(repo.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        var service = new UsuarioService(repo, new BCryptPasswordEncoder());

        service.restablecerPassword(7, "Temporal2026");

        assertTrue(new BCryptPasswordEncoder().matches("Temporal2026", u.getPasswordHash()));
        assertTrue(u.isDebeCambiarPassword());
        assertEquals(0, u.getIntentosFallidos());
        assertNull(u.getBloqueadoHasta());
    }

    @Test
    void desbloquearLimpiaIntentosYBloqueo() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        Usuario u = new Usuario();
        u.setIdUsuario(8);
        u.setNombreUsuario("cocina2");
        u.setNombre("Cocina Dos");
        u.setRol(RolUsuario.COCINERO);
        u.setActivo(true);
        u.setIntentosFallidos(5);
        u.setBloqueadoHasta(java.time.LocalDateTime.now().plusMinutes(10));
        when(repo.findById(8)).thenReturn(Optional.of(u));
        when(repo.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        var service = new UsuarioService(repo, new BCryptPasswordEncoder());

        service.desbloquear(8);

        assertEquals(0, u.getIntentosFallidos());
        assertNull(u.getBloqueadoHasta());
    }
}
