package pe.edu.utp.jarana.dto;

import java.time.LocalDateTime;
import pe.edu.utp.jarana.model.RolUsuario;
import pe.edu.utp.jarana.model.Usuario;

public record UsuarioResponse(
        Integer idUsuario,
        String nombreUsuario,
        String nombre,
        RolUsuario rol,
        boolean activo,
        int intentosFallidos,
        LocalDateTime bloqueadoHasta,
        LocalDateTime ultimoAcceso,
        LocalDateTime fechaCreacion,
        boolean debeCambiarPassword) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(
                u.getIdUsuario(), u.getNombreUsuario(), u.getNombre(), u.getRol(), u.isActivo(),
                u.getIntentosFallidos(), u.getBloqueadoHasta(), u.getUltimoAcceso(), u.getFechaCreacion(),
                u.isDebeCambiarPassword());
    }
}
