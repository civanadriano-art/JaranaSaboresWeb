package pe.edu.utp.jarana.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import pe.edu.utp.jarana.estructuras.ListaEnlazada;
import pe.edu.utp.jarana.model.EstadoPedido;
import pe.edu.utp.jarana.model.Venta;
import pe.edu.utp.jarana.repository.PedidoRepository;
import pe.edu.utp.jarana.repository.VentaRepository;

@Service
public class ReporteService {
    private final VentaRepository ventaRepository;
    private final PedidoRepository pedidoRepository;

    public ReporteService(VentaRepository ventaRepository, PedidoRepository pedidoRepository) {
        this.ventaRepository = ventaRepository;
        this.pedidoRepository = pedidoRepository;
    }

    public Map<String, Object> resumen() {
        ListaEnlazada<Venta> listaVentas = new ListaEnlazada<>();
        ventaRepository.findAll().forEach(listaVentas::agregar);

        BigDecimal[] total = {BigDecimal.ZERO};
        long[] sumaMin = {0};
        int[] conTiempo = {0};
        Map<String, Integer> platos = new LinkedHashMap<>();

        listaVentas.paraCada(v -> {
            total[0] = total[0].add(v.getTotal());
            if (v.getMinutosCocina() != null) { sumaMin[0] += v.getMinutosCocina(); conTiempo[0]++; }
            v.getPedido().getDetalles().forEach(d -> platos.merge(d.getPlato().getNombre(), d.getCantidad(), Integer::sum));
        });

        String platoMasVendido = platos.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("Sin ventas");
        double promedio = conTiempo[0] == 0 ? 0 : (double) sumaMin[0] / conTiempo[0];
        long pendientes = pedidoRepository.countByEstadoIn(List.of(EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO));

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("pedidosPendientes", pendientes);
        r.put("pedidosAtendidos", listaVentas.getCantidad());
        r.put("totalVendido", total[0]);
        r.put("platoMasVendido", platoMasVendido);
        r.put("tiempoPromedioCocina", Math.round(promedio * 100.0) / 100.0);
        return r;
    }

    public List<Venta> ventas(LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) return ventaRepository.findAllByOrderByFechaHoraDespachoDesc();
        LocalDate d = desde == null ? LocalDate.of(2000,1,1) : desde;
        LocalDate h = hasta == null ? LocalDate.now() : hasta;
        LocalDateTime inicio = d.atStartOfDay();
        LocalDateTime fin = h.atTime(LocalTime.MAX);
        return ventaRepository.findByFechaHoraDespachoBetweenOrderByFechaHoraDespachoDesc(inicio, fin);
    }
}
