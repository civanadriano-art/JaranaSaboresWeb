package pe.edu.utp.jarana.exception;

import org.springframework.web.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatusCode;
import org.springframework.beans.TypeMismatchException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<?> negocio(ReglaNegocioException ex) {
        log.warn("Regla de negocio rechazada: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errores.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("error", "Datos inválidos", "campos", errores));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> cuerpoInvalido(HttpMessageNotReadableException ex) {
        log.warn("Solicitud con formato inválido: {}", ex.getMostSpecificCause().getClass().getSimpleName());
        return ResponseEntity.badRequest().body(Map.of("error", "La solicitud tiene un formato inválido"));
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<?> parametroInvalido(TypeMismatchException ex) {
        log.warn("Parámetro con tipo inválido: {}", ex.getPropertyName());
        return ResponseEntity.badRequest().body(Map.of("error", "Uno de los parámetros no es válido"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> general(Exception ex) {
        // Errores HTTP estándar de Spring (404, 405, 415...): se responden con su código, no como error interno
        if (ex instanceof ErrorResponse respuesta) {
            HttpStatusCode estado = respuesta.getStatusCode();
            log.warn("Solicitud rechazada ({}): {}", estado.value(), ex.getClass().getSimpleName());
            String mensaje = switch (estado.value()) {
                case 404 -> "Recurso no encontrado";
                case 405 -> "Método no permitido";
                case 415 -> "Tipo de contenido no soportado";
                default -> "Solicitud no válida";
            };
            return ResponseEntity.status(estado).body(Map.of("error", mensaje));
        }
        // El detalle técnico queda solo en el log del servidor; al usuario se le da una referencia para ubicarlo.
        String referencia = UUID.randomUUID().toString().substring(0, 8);
        log.error("Error interno no controlado (ref. {})", referencia, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ocurrió un error interno (ref. " + referencia + ")"));
    }
}
