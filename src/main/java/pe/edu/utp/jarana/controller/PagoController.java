package pe.edu.utp.jarana.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.jarana.dto.PagoRequest;
import pe.edu.utp.jarana.model.Pago;
import pe.edu.utp.jarana.service.PagoService;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {
    private final PagoService service;
    public PagoController(PagoService service) { this.service = service; }
    @GetMapping public List<Pago> listar() { return service.listar(); }
    @GetMapping("/pedido/{numero}") public Map<String,Boolean> estado(@PathVariable Integer numero) { return Map.of("pagado", service.estaPagado(numero)); }
    @PostMapping("/pedido/{numero}") @ResponseStatus(HttpStatus.CREATED)
    public Pago cobrar(@PathVariable Integer numero, @Valid @RequestBody PagoRequest req, Principal principal) {
        return service.cobrar(numero, req, principal.getName());
    }
}
