package pe.edu.utp.jarana.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.jarana.model.MetodoPago;
import pe.edu.utp.jarana.service.AuditoriaService;
import pe.edu.utp.jarana.service.ReporteExcelService;
import pe.edu.utp.jarana.service.ReporteService;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {
    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReporteService service;
    private final ReporteExcelService excel;
    private final AuditoriaService auditoria;

    public ReporteController(ReporteService service, ReporteExcelService excel, AuditoriaService auditoria) {
        this.service = service;
        this.excel = excel;
        this.auditoria = auditoria;
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

    @GetMapping("/excel")
    public ResponseEntity<byte[]> excel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) MetodoPago metodo,
            Principal principal) {
        byte[] archivo = excel.generar(desde, hasta, metodo);
        auditoria.registrar(principal.getName(), "EXPORTAR_REPORTE", "REPORTE", "EXCEL", "Reporte de ventas exportado a Excel");
        String nombre = "reporte_jarana_" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .contentType(XLSX)
                .contentLength(archivo.length)
                .body(archivo);
    }
}
