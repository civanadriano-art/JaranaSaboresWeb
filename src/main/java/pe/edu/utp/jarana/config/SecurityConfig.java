package pe.edu.utp.jarana.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login.html", "/css/**", "/js/**", "/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/pedidos/*/preparar", "/api/pedidos/*/listo").hasAnyRole("COCINERO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/pedidos", "/api/pedidos/*/despachar", "/api/pedidos/*/cancelar", "/api/pedidos/restaurar-ultimo").hasAnyRole("CAJERO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/api/pedidos/**").hasAnyRole("CAJERO", "ADMINISTRADOR")
                .requestMatchers("/api/pagos/**").hasAnyRole("CAJERO", "ADMINISTRADOR")
                .requestMatchers("/api/auditoria/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/platos/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/api/platos/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/api/platos/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/reportes/**").hasRole("ADMINISTRADOR")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard.html", true)
                .failureUrl("/login.html?error")
                .permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login.html?logout").permitAll());
        return http.build();
    }
}
