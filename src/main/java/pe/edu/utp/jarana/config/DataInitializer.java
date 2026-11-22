package pe.edu.utp.jarana.config;

import java.math.BigDecimal;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /**
     * Cuentas de demostración de versiones anteriores del sistema. Ya no se crean; si una base de datos
     * antigua todavía las tiene con su contraseña original, se desactivan al iniciar.
     */
    private static final Map<String, String> CUENTAS_DEMO_ANTIGUAS = Map.of(
            "admin", "admin123",
            "cajero", "cajero123",
            "cocina", "cocina123");

    @Bean
    CommandLineRunner datosIniciales(UsuarioRepository usuarios, PlatoRepository platos, PasswordEncoder encoder,
                                     EstructurasDatosService estructuras,
                                     @Value("${app.admin.usuario}") String adminUsuario,
                                     @Value("${app.admin.nombre}") String adminNombre,
                                     @Value("${app.admin.password}") String adminPassword) {
        return args -> {
            crearAdministradorInicial(usuarios, encoder, adminUsuario, adminNombre, adminPassword);
            desactivarCuentasDemoAntiguas(usuarios, encoder, adminUsuario);

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

    /** Crea el administrador inicial solo si no existe; nunca pisa la contraseña de una cuenta ya creada. */
    private void crearAdministradorInicial(UsuarioRepository repo, PasswordEncoder encoder,
                                           String usuario, String nombre, String clave) {
        if (StringUtils.isBlank(usuario) || StringUtils.isBlank(clave)) {
            log.warn("No se creó el administrador inicial: faltan app.admin.usuario o app.admin.password");
            return;
        }
        String login = usuario.trim();
        var existente = repo.findByNombreUsuarioIgnoreCase(login);
        if (existente.isPresent()) {
            Usuario u = existente.get();
            if (StringUtils.isBlank(u.getNombre())) { u.setNombre(StringUtils.defaultIfBlank(nombre, login)); repo.save(u); }
            return;
        }
        Usuario u = new Usuario();
        u.setNombreUsuario(login);
        u.setNombre(StringUtils.defaultIfBlank(nombre, login));
        u.setPasswordHash(encoder.encode(clave));
        u.setPasswordSal("BCrypt");
        u.setRol(RolUsuario.ADMINISTRADOR);
        u.setActivo(true);
        u.setDebeCambiarPassword(false);
        repo.save(u);
        log.info("Cuenta de administrador inicial '{}' creada", login);
    }

    /**
     * Desactiva (no borra, para conservar el historial) las antiguas cuentas de demostración que aún tengan su
     * contraseña original. Si alguien ya cambió la contraseña, la cuenta se considera real y no se toca.
     */
    private void desactivarCuentasDemoAntiguas(UsuarioRepository repo, PasswordEncoder encoder, String adminUsuario) {
        String admin = StringUtils.trimToEmpty(adminUsuario);
        CUENTAS_DEMO_ANTIGUAS.forEach((usuario, claveOriginal) -> {
            if (usuario.equalsIgnoreCase(admin)) return;
            repo.findByNombreUsuarioIgnoreCase(usuario).ifPresent(u -> {
                if (!u.isActivo() || !encoder.matches(claveOriginal, u.getPasswordHash())) return;
                if (u.getRol() == RolUsuario.ADMINISTRADOR
                        && repo.countByRolAndActivoTrue(RolUsuario.ADMINISTRADOR) <= 1) {
                    log.warn("La cuenta de demostración '{}' sigue activa porque es el único administrador activo", usuario);
                    return;
                }
                u.setActivo(false);
                repo.save(u);
                log.warn("Cuenta de demostración '{}' desactivada (conservaba su contraseña original)", usuario);
            });
        });
    }

    private void crearPlato(PlatoRepository repo, EstructurasDatosService estructuras, String nombre, String categoria, String precio) {
        Plato p = new Plato();
        p.setNombre(nombre); p.setCategoria(categoria); p.setPrecio(new BigDecimal(precio)); p.setDisponible(true); p.setActivo(true);
        p = repo.save(p);
        estructuras.registrarPlato(p);
    }
}
