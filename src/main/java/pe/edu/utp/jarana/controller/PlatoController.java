package pe.edu.utp.jarana.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.jarana.dto.PlatoRequest;
import pe.edu.utp.jarana.model.Plato;
import pe.edu.utp.jarana.service.AuditoriaService;
import pe.edu.utp.jarana.service.PlatoService;

@RestController
@RequestMapping("/api/platos")
public class PlatoController {
    private final PlatoService service;
    private final AuditoriaService auditoria;
    public PlatoController(PlatoService service, AuditoriaService auditoria) { this.service = service; this.auditoria = auditoria; }

    @GetMapping public List<Plato> listar() { return service.listarActivos(); }
    @GetMapping("/{codigo}") public Plato obtener(@PathVariable Integer codigo) { return service.buscar(codigo); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Plato crear(@Valid @RequestBody PlatoRequest req, Principal principal) {
        Plato p = service.crear(req);
        auditoria.registrar(principal.getName(), "REGISTRAR_PLATO", "PLATO", String.valueOf(p.getCodigo()), p.getNombre());
        return p;
    }

    @PutMapping("/{codigo}")
    public Plato modificar(@PathVariable Integer codigo, @Valid @RequestBody PlatoRequest req, Principal principal) {
        Plato p = service.modificar(codigo, req);
        auditoria.registrar(principal.getName(), "MODIFICAR_PLATO", "PLATO", String.valueOf(codigo), p.getNombre());
        return p;
    }

    @PatchMapping("/{codigo}/disponibilidad")
    public Plato cambiarDisponibilidad(@PathVariable Integer codigo, @RequestParam boolean disponible, Principal principal) {
        Plato p = service.cambiarDisponibilidad(codigo, disponible);
        auditoria.registrar(
                principal.getName(),
                disponible ? "MARCAR_PLATO_DISPONIBLE" : "MARCAR_PLATO_AGOTADO",
                "PLATO",
                String.valueOf(codigo),
                p.getNombre());
        return p;
    }

    @DeleteMapping("/{codigo}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer codigo, Principal principal) {
        service.desactivar(codigo);
        auditoria.registrar(principal.getName(), "DESACTIVAR_PLATO", "PLATO", String.valueOf(codigo), "Plato desactivado");
    }
}
