package pe.edu.utp.jarana.service;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.jarana.dto.CambiarPasswordRequest;
import pe.edu.utp.jarana.dto.CrearUsuarioRequest;
import pe.edu.utp.jarana.dto.UsuarioResponse;
import pe.edu.utp.jarana.exception.ReglaNegocioException;
import pe.edu.utp.jarana.model.RolUsuario;
import pe.edu.utp.jarana.model.Usuario;
import pe.edu.utp.jarana.repository.UsuarioRepository;

@Service
public class UsuarioService {
    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    public List<UsuarioResponse> listar() {
        return usuarios.findAll().stream()
                .sorted((a,b) -> a.getNombreUsuario().compareToIgnoreCase(b.getNombreUsuario()))
                .map(UsuarioResponse::de)
                .toList();
    }

    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest req) {
        String login = req.getNombreUsuario().trim();
        if (usuarios.findByNombreUsuarioIgnoreCase(login).isPresent()) {
            throw new ReglaNegocioException("Ya existe un usuario con ese nombre de acceso");
        }
        Usuario u = new Usuario();
        u.setNombreUsuario(login);
        u.setNombre(req.getNombre().trim());
        u.setRol(req.getRol());
        u.setPasswordHash(encoder.encode(req.getPasswordTemporal()));
        u.setPasswordSal("BCrypt");
        u.setActivo(true);
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        u.setDebeCambiarPassword(true);
        Usuario guardado = usuarios.save(u);
        log.info("Usuario '{}' creado con rol {}", limpiar(login), req.getRol());
        return UsuarioResponse.de(guardado);
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Integer id, boolean activo, String usuarioActual) {
        Usuario u = buscar(id);
        if (!activo && u.getNombreUsuario().equalsIgnoreCase(usuarioActual)) {
            throw new ReglaNegocioException("No puedes desactivar tu propia cuenta");
        }
        if (!activo && u.getRol() == RolUsuario.ADMINISTRADOR && u.isActivo()
                && usuarios.countByRolAndActivoTrue(RolUsuario.ADMINISTRADOR) <= 1) {
            throw new ReglaNegocioException("Debe permanecer al menos un administrador activo");
        }
        u.setActivo(activo);
        if (activo) {
            u.setIntentosFallidos(0);
            u.setBloqueadoHasta(null);
        }
        Usuario guardado = usuarios.save(u);
        log.info("Cuenta '{}' {} por '{}'", limpiar(u.getNombreUsuario()), activo ? "activada" : "desactivada", limpiar(usuarioActual));
        return UsuarioResponse.de(guardado);
    }

    @Transactional
    public UsuarioResponse desbloquear(Integer id) {
        Usuario u = buscar(id);
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        Usuario guardado = usuarios.save(u);
        log.info("Cuenta '{}' desbloqueada", limpiar(u.getNombreUsuario()));
        return UsuarioResponse.de(guardado);
    }

    @Transactional
    public UsuarioResponse restablecerPassword(Integer id, String temporal) {
        Usuario u = buscar(id);
        u.setPasswordHash(encoder.encode(temporal));
        u.setPasswordSal("BCrypt");
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        u.setDebeCambiarPassword(true);
        Usuario guardado = usuarios.save(u);
        log.info("Contraseña restablecida para '{}' (deberá cambiarla al ingresar)", limpiar(u.getNombreUsuario()));
        return UsuarioResponse.de(guardado);
    }

    @Transactional
    public void cambiarPasswordPropio(String username, CambiarPasswordRequest req) {
        Usuario u = usuarios.findByNombreUsuarioIgnoreCase(username)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado"));
        if (!encoder.matches(req.getPasswordActual(), u.getPasswordHash())) {
            log.warn("Cambio de contraseña rechazado para '{}': la contraseña actual no coincide", limpiar(username));
            throw new ReglaNegocioException("La contraseña actual no es correcta");
        }
        if (encoder.matches(req.getPasswordNueva(), u.getPasswordHash())) {
            throw new ReglaNegocioException("La nueva contraseña debe ser diferente de la actual");
        }
        u.setPasswordHash(encoder.encode(req.getPasswordNueva()));
        u.setPasswordSal("BCrypt");
        u.setDebeCambiarPassword(false);
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        usuarios.save(u);
        log.info("'{}' cambió su contraseña", limpiar(username));
    }

    @Transactional
    public void registrarFalloLogin(String username) {
        if (StringUtils.isBlank(username)) return;
        usuarios.findByNombreUsuarioIgnoreCase(username).ifPresent(u -> {
            if (!u.isActivo()) return;
            if (u.getBloqueadoHasta() != null && u.getBloqueadoHasta().isAfter(LocalDateTime.now())) return;
            int intentos = u.getIntentosFallidos() + 1;
            u.setIntentosFallidos(intentos);
            if (intentos >= 5) {
                u.setBloqueadoHasta(LocalDateTime.now().plusMinutes(15));
                log.warn("Cuenta '{}' bloqueada por 15 minutos tras {} intentos fallidos", limpiar(username), intentos);
            } else {
                log.warn("Intento de inicio de sesión fallido para '{}' ({} de 5)", limpiar(username), intentos);
            }
            usuarios.save(u);
        });
    }

    @Transactional
    public boolean registrarLoginExitoso(String username) {
        Usuario u = usuarios.findByNombreUsuarioIgnoreCase(username)
                .orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado"));
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        u.setUltimoAcceso(LocalDateTime.now());
        usuarios.save(u);
        log.info("Inicio de sesión correcto: '{}'", limpiar(username));
        return u.isDebeCambiarPassword();
    }

    /** Evita que un nombre de usuario con saltos de línea falsifique entradas del log. */
    private static String limpiar(String texto) {
        return StringUtils.defaultString(StringUtils.replaceChars(texto, "\r\n", "__"));
    }

    private Usuario buscar(Integer id) {
        return usuarios.findById(id).orElseThrow(() -> new ReglaNegocioException("Usuario no encontrado"));
    }
}
