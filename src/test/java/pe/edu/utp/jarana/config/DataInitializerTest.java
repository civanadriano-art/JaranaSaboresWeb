package pe.edu.utp.jarana.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pe.edu.utp.jarana.model.RolUsuario;
import pe.edu.utp.jarana.model.Usuario;
import pe.edu.utp.jarana.repository.PlatoRepository;
import pe.edu.utp.jarana.repository.UsuarioRepository;

class DataInitializerTest {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private UsuarioRepository usuarios;
    private PlatoRepository platos;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        platos = mock(PlatoRepository.class);
        when(platos.count()).thenReturn(1L); // ya hay platos: no se cargan los de ejemplo
    }

    private void ejecutar(String usuario, String clave) throws Exception {
        new DataInitializer().datosIniciales(usuarios, platos, encoder, null, usuario, "Esperanza", clave).run();
    }

    private Usuario cuenta(String nombreUsuario, String clave, RolUsuario rol) {
        Usuario u = new Usuario();
        u.setNombreUsuario(nombreUsuario);
        u.setNombre(nombreUsuario);
        u.setPasswordHash(encoder.encode(clave));
        u.setRol(rol);
        u.setActivo(true);
        return u;
    }

    @Test
    void creaElAdministradorInicialConLaClaveCifrada() throws Exception {
        ejecutar("Esperanza", "70619457");

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios, times(1)).save(guardado.capture()); // solo ella: ya no se crean cuentas demo
        Usuario esperanza = guardado.getValue();

        assertEquals("Esperanza", esperanza.getNombreUsuario());
        assertEquals(RolUsuario.ADMINISTRADOR, esperanza.getRol());
        assertTrue(esperanza.isActivo());
        assertTrue(encoder.matches("70619457", esperanza.getPasswordHash()));
        assertNotEquals("70619457", esperanza.getPasswordHash());
    }

    @Test
    void desactivaLasCuentasDemoAntiguasQueConservanSuClaveOriginal() throws Exception {
        Usuario admin = cuenta("admin", "admin123", RolUsuario.ADMINISTRADOR);
        Usuario cajero = cuenta("cajero", "cajero123", RolUsuario.CAJERO);
        Usuario cocina = cuenta("cocina", "cocina123", RolUsuario.COCINERO);
        when(usuarios.findByNombreUsuarioIgnoreCase("admin")).thenReturn(Optional.of(admin));
        when(usuarios.findByNombreUsuarioIgnoreCase("cajero")).thenReturn(Optional.of(cajero));
        when(usuarios.findByNombreUsuarioIgnoreCase("cocina")).thenReturn(Optional.of(cocina));
        when(usuarios.countByRolAndActivoTrue(RolUsuario.ADMINISTRADOR)).thenReturn(2L); // admin demo + Esperanza

        ejecutar("Esperanza", "70619457");

        assertFalse(admin.isActivo());
        assertFalse(cajero.isActivo());
        assertFalse(cocina.isActivo());
    }

    @Test
    void noTocaUnaCuentaQueYaCambioSuContrasena() throws Exception {
        Usuario cajeroReal = cuenta("cajero", "ClaveNueva#2026", RolUsuario.CAJERO);
        when(usuarios.findByNombreUsuarioIgnoreCase("cajero")).thenReturn(Optional.of(cajeroReal));

        ejecutar("Esperanza", "70619457");

        assertTrue(cajeroReal.isActivo());
    }

    @Test
    void noDejaElSistemaSinAdministradorSiFaltaLaClaveInicial() throws Exception {
        Usuario adminDemo = cuenta("admin", "admin123", RolUsuario.ADMINISTRADOR);
        when(usuarios.findByNombreUsuarioIgnoreCase("admin")).thenReturn(Optional.of(adminDemo));
        when(usuarios.countByRolAndActivoTrue(RolUsuario.ADMINISTRADOR)).thenReturn(1L);

        ejecutar("Esperanza", "");

        assertTrue(adminDemo.isActivo());
        verify(usuarios, never()).save(any(Usuario.class));
    }
}
