package pe.edu.utp.jarana.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.jarana.model.MetodoPago;
import pe.edu.utp.jarana.service.ReporteService;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {
    private final ReporteService service;

    public ReporteController(ReporteService service) {
        this.service = service;
    }

    @GetMapping("/resumen")
    public Map<String, Object> resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) MetodoPago metodo) {
        return service.resumen(desde, hasta, metodo);
    }

    @GetMapping("/ventas")
    public List<Map<String, Object>> ventas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) MetodoPago metodo) {
        return service.ventas(desde, hasta, metodo);
    }
}
