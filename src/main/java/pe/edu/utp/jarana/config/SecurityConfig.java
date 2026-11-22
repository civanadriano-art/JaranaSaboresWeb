package pe.edu.utp.jarana.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import pe.edu.utp.jarana.service.UsuarioService;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioService usuarios) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            // La cookie de sesión no viaja en peticiones iniciadas desde otros sitios (protege al tener CSRF desactivado)
            .headers(headers -> headers
                .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login.html", "/css/**", "/js/**", "/favicon.ico").permitAll()
                .requestMatchers("/cambiar-password.html", "/api/auth/me", "/api/auth/cambiar-password").authenticated()
                .requestMatchers("/pedidos.html", "/pagos.html").hasAnyRole("CAJERO", "ADMINISTRADOR")
                .requestMatchers("/cocina.html").hasAnyRole("COCINERO", "ADMINISTRADOR")
                .requestMatchers("/catalogo.html", "/reportes.html", "/auditoria.html").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/pedidos/*/preparar", "/api/pedidos/*/listo").hasAnyRole("COCINERO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/pedidos", "/api/pedidos/*/despachar", "/api/pedidos/*/cancelar").hasAnyRole("CAJERO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/pedidos/restaurar-ultimo").hasRole("ADMINISTRADOR")
                .requestMatchers("/historial.html", "/api/pedidos/eliminados/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/api/pedidos/**").hasAnyRole("CAJERO", "ADMINISTRADOR")
                .requestMatchers("/api/pagos/**").hasAnyRole("CAJERO", "ADMINISTRADOR")
                .requestMatchers("/api/usuarios/**", "/usuarios.html").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/auditoria/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/backups/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/backup.html").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PATCH, "/api/platos/*/disponibilidad").hasAnyRole("COCINERO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/platos/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/api/platos/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/api/platos/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/reportes/**").hasRole("ADMINISTRADOR")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .successHandler((request, response, authentication) -> {
                    boolean cambiar = usuarios.registrarLoginExitoso(authentication.getName());
                    if (cambiar) request.getSession().setAttribute(PasswordTemporalFilter.ATRIBUTO, Boolean.TRUE);
                    response.sendRedirect(cambiar ? "/cambiar-password.html" : "/dashboard.html");
                })
                .failureHandler((request, response, exception) -> {
                    usuarios.registrarFalloLogin(request.getParameter("username"));
                    // Mensaje distinto según el motivo, para que el personal sepa qué hacer
                    String destino = exception instanceof LockedException ? "/login.html?bloqueado"
                            : exception instanceof DisabledException ? "/login.html?inactivo"
                            : "/login.html?error";
                    response.sendRedirect(destino);
                })
                .permitAll())
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/login.html?logout")
                .permitAll());
        return http.build();
    }
}
