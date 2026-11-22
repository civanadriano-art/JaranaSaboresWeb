package pe.edu.utp.jarana.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Agrega el usuario autenticado al contexto de Logback (MDC) durante cada petición,
 * de modo que cada línea del log indique quién realizó la acción: [cajero], [admin], etc.
 * Se ejecuta después de Spring Security, por eso ya conoce al usuario.
 */
@Component
public class LogContextoFilter extends OncePerRequestFilter {
    public static final String CLAVE_USUARIO = "usuario";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean identificado = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
        MDC.put(CLAVE_USUARIO, identificado ? auth.getName() : "anonimo");
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CLAVE_USUARIO);
        }
    }
}
