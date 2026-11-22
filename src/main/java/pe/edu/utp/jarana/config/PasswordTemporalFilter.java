package pe.edu.utp.jarana.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Mientras una cuenta tenga contraseña temporal, solo puede usar /api/auth/** (para cambiarla).
 * Antes esto lo hacía únicamente el frontend; así también se bloquea el uso directo de la API.
 */
@Component
public class PasswordTemporalFilter extends OncePerRequestFilter {
    public static final String ATRIBUTO = "DEBE_CAMBIAR_PASSWORD";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpSession sesion = request.getSession(false);
        String ruta = request.getRequestURI();
        if (sesion != null && Boolean.TRUE.equals(sesion.getAttribute(ATRIBUTO))
                && ruta.startsWith("/api/") && !ruta.startsWith("/api/auth/")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Debes cambiar tu contraseña temporal antes de continuar\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
