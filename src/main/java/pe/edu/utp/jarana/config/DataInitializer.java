package pe.edu.utp.jarana.config;

import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.utp.jarana.model.Plato;
import pe.edu.utp.jarana.model.RolUsuario;
import pe.edu.utp.jarana.model.Usuario;
import pe.edu.utp.jarana.repository.PlatoRepository;
import pe.edu.utp.jarana.repository.UsuarioRepository;
import pe.edu.utp.jarana.service.EstructurasDatosService;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner datosIniciales(UsuarioRepository usuarios, PlatoRepository platos, PasswordEncoder encoder, EstructurasDatosService estructuras) {
        return args -> {
            crearUsuarioSiNoExiste(usuarios, encoder, "admin", "admin123", "Administrador General", RolUsuario.ADMINISTRADOR);
            crearUsuarioSiNoExiste(usuarios, encoder, "cajero", "cajero123", "Personal de Caja", RolUsuario.CAJERO);
            crearUsuarioSiNoExiste(usuarios, encoder, "cocina", "cocina123", "Personal de Cocina", RolUsuario.COCINERO);

            if (platos.count() == 0) {
                crearPlato(platos, estructuras, "Pollo Broaster - Ala", "Broaster", "10.00");
                crearPlato(platos, estructuras, "Pollo Broaster - Pecho", "Broaster", "12.00");
                crearPlato(platos, estructuras, "Lomo Saltado", "Platos de fondo", "25.00");
                crearPlato(platos, estructuras, "Ají de Gallina", "Platos de fondo", "22.00");
                crearPlato(platos, estructuras, "Arroz con Pollo", "Platos de fondo", "20.00");
                crearPlato(platos, estructuras, "Chaufa de Pollo", "Platos de fondo", "18.00");
                crearPlato(platos, estructuras, "Seco de Res", "Platos de fondo", "24.00");
                crearPlato(platos, estructuras, "Causa Limeña", "Entradas", "15.00");
                crearPlato(platos, estructuras, "Anticuchos", "Parrillas", "12.00");
                crearPlato(platos, estructuras, "Chicha Morada", "Bebidas", "8.00");
            }
        };
    }

    private void crearUsuarioSiNoExiste(UsuarioRepository repo, PasswordEncoder encoder, String usuario, String clave, String nombre, RolUsuario rol) {
        var existente = repo.findByNombreUsuarioIgnoreCase(usuario);
        if (existente.isPresent()) {
            Usuario u = existente.get();
            if (u.getNombre() == null || u.getNombre().isBlank()) { u.setNombre(nombre); repo.save(u); }
            return;
        }
        Usuario u = new Usuario();
        u.setNombreUsuario(usuario);
        u.setNombre(nombre);
        u.setPasswordHash(encoder.encode(clave));
        u.setPasswordSal("BCrypt");
        u.setRol(rol);
        u.setActivo(true);
        repo.save(u);
    }

    private void crearPlato(PlatoRepository repo, EstructurasDatosService estructuras, String nombre, String categoria, String precio) {
        Plato p = new Plato();
        p.setNombre(nombre); p.setCategoria(categoria); p.setPrecio(new BigDecimal(precio)); p.setDisponible(true); p.setActivo(true);
        p = repo.save(p);
        estructuras.registrarPlato(p);
    }
}
