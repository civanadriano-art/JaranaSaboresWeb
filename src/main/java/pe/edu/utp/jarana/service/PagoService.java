package pe.edu.utp.jarana.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.jarana.dto.PagoRequest;
import pe.edu.utp.jarana.exception.ReglaNegocioException;
import pe.edu.utp.jarana.model.*;
import pe.edu.utp.jarana.repository.*;

@Service
public class PagoService {
    private final PagoRepository pagos;
    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;
    public PagoService(PagoRepository pagos, PedidoRepository pedidos, UsuarioRepository usuarios, AuditoriaService auditoria) {
        this.pagos = pagos; this.pedidos = pedidos; this.usuarios = usuarios; this.auditoria = auditoria;
    }

    public List<Pago> listar() { return pagos.findAllByOrderByFechaPagoDesc(); }

    @Transactional
    public Pago cobrar(Integer numero, PagoRequest req, String username) {
        if (pagos.existsByPedidoNumero(numero)) throw new ReglaNegocioException("El pedido ya fue pagado");
        Pedido pedido = pedidos.findById(numero).orElseThrow(() -> new ReglaNegocioException("Pedido no encontrado"));
        if (pedido.getEstado() == EstadoPedido.CANCELADO) throw new ReglaNegocioException("No se puede cobrar un pedido cancelado");
        BigDecimal total = pedido.getDetalles().stream()
                .map(d -> d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())).add(d.getCostoExtra()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal recibido = req.getImporteRecibido();
        BigDecimal vuelto = BigDecimal.ZERO;
        if (req.getMetodo() == MetodoPago.EFECTIVO) {
            if (recibido == null || recibido.compareTo(total) < 0) throw new ReglaNegocioException("El importe recibido debe cubrir el total");
            vuelto = recibido.subtract(total);
        } else {
            recibido = total;
        }
        Usuario operador = usuarios.findByNombreUsuarioIgnoreCase(username).orElseThrow(() -> new ReglaNegocioException("Usuario operador no encontrado"));
        Pago p = new Pago();
        p.setPedido(pedido); p.setMetodo(req.getMetodo()); p.setImporteTotal(total); p.setImporteRecibido(recibido);
        p.setVuelto(vuelto); p.setOperador(operador);
        p = pagos.save(p);
        auditoria.registrar(username, "REGISTRAR_PAGO", "PEDIDO", String.valueOf(numero), "Pago " + req.getMetodo() + " por S/ " + total);
        return p;
    }

    public boolean estaPagado(Integer numero) { return pagos.existsByPedidoNumero(numero); }
}
