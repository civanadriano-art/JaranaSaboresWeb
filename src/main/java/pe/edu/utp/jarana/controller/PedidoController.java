package pe.edu.utp.jarana.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.jarana.dto.CrearPedidoRequest;
import pe.edu.utp.jarana.dto.PedidoResponse;
import pe.edu.utp.jarana.model.EstadoPedido;
import pe.edu.utp.jarana.model.PedidoEliminado;
import pe.edu.utp.jarana.service.AuditoriaService;
import pe.edu.utp.jarana.service.PedidoService;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService service;
    private final AuditoriaService auditoria;
    public PedidoController(PedidoService service, AuditoriaService auditoria) { this.service = service; this.auditoria = auditoria; }

    @GetMapping
    public List<PedidoResponse> listar(@RequestParam(required = false) String estado) {
        return estado == null || estado.isBlank() ? service.listarActivos() : service.listarPorEstado(EstadoPedido.desdeEtiqueta(estado));
    }

    @GetMapping("/{numero}") public PedidoResponse obtener(@PathVariable Integer numero) { return service.obtener(numero); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse registrar(@Valid @RequestBody CrearPedidoRequest req, Principal principal) {
        PedidoResponse p = service.registrar(req);
        auditoria.registrar(principal.getName(), "REGISTRAR_PEDIDO", "PEDIDO", String.valueOf(p.numero()), "Pedido registrado para " + p.cliente());
        return p;
    }

    @PutMapping("/{numero}")
    public PedidoResponse editar(@PathVariable Integer numero, @Valid @RequestBody CrearPedidoRequest req, Principal principal) {
        PedidoResponse p = service.editar(numero, req);
        auditoria.registrar(principal.getName(), "MODIFICAR_PEDIDO", "PEDIDO", String.valueOf(numero), "Se modificó el detalle del pedido");
        return p;
    }

    @PostMapping("/{numero}/preparar")
    public PedidoResponse preparar(@PathVariable Integer numero, Principal principal) {
        PedidoResponse p = service.iniciarPreparacion(numero);
        auditoria.registrar(principal.getName(), "INICIAR_PREPARACION", "PEDIDO", String.valueOf(numero), "Pedido enviado a preparación");
        return p;
    }

    @PostMapping("/{numero}/listo")
    public PedidoResponse listo(@PathVariable Integer numero, Principal principal) {
        PedidoResponse p = service.marcarListo(numero);
        auditoria.registrar(principal.getName(), "MARCAR_LISTO", "PEDIDO", String.valueOf(numero), "Pedido marcado como listo");
        return p;
    }

    @PostMapping("/{numero}/despachar")
    public PedidoResponse despachar(@PathVariable Integer numero, Principal principal) {
        PedidoResponse p = service.despachar(numero);
        auditoria.registrar(principal.getName(), "DESPACHAR_PEDIDO", "PEDIDO", String.valueOf(numero), "Pedido despachado y venta generada");
        return p;
    }

    @PostMapping("/{numero}/cancelar")
    public Map<String,String> cancelar(@PathVariable Integer numero, @RequestBody(required = false) Map<String,String> body, Principal principal) {
        String motivo = body == null ? null : body.get("motivo");
        service.cancelar(numero, motivo);
        auditoria.registrar(principal.getName(), "CANCELAR_PEDIDO", "PEDIDO", String.valueOf(numero), motivo == null ? "Sin motivo" : motivo);
        return Map.of("mensaje", "Pedido cancelado");
    }

    @PostMapping("/restaurar-ultimo")
    public PedidoResponse restaurarUltimo(Principal principal) {
        PedidoResponse p = service.restaurarUltimo();
        auditoria.registrar(principal.getName(), "RESTAURAR_PEDIDO", "PEDIDO", String.valueOf(p.numero()), "Se restauró el último pedido eliminado");
        return p;
    }

    @GetMapping("/eliminados/historial") public List<PedidoEliminado> eliminados() { return service.listarEliminados(); }
}
