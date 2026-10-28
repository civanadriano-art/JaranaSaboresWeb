package pe.edu.utp.jarana.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import pe.edu.utp.jarana.exception.ReglaNegocioException;
import pe.edu.utp.jarana.model.MetodoPago;
import pe.edu.utp.jarana.model.Pago;
import pe.edu.utp.jarana.model.Venta;
import pe.edu.utp.jarana.repository.PagoRepository;
import pe.edu.utp.jarana.repository.VentaRepository;

@Service
public class ReporteService {
    private final VentaRepository ventaRepository;
    private final PagoRepository pagoRepository;

    public ReporteService(VentaRepository ventaRepository, PagoRepository pagoRepository) {
        this.ventaRepository = ventaRepository;
        this.pagoRepository = pagoRepository;
    }

    public Map<String, Object> resumen(LocalDate desde, LocalDate hasta, MetodoPago metodo) {
        List<Venta> ventas = filtrarVentas(desde, hasta, metodo);
        Map<Integer, Pago> pagos = pagosPorPedido();

        BigDecimal total = ventas.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ticketPromedio = ventas.isEmpty()
                ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(ventas.size()), 2, RoundingMode.HALF_UP);

        long sumaMinutos = ventas.stream()
                .map(Venta::getMinutosCocina)
                .filter(m -> m != null && m > 0)
                .mapToLong(Long::longValue)
                .sum();
        long ventasConTiempo = ventas.stream()
                .map(Venta::getMinutosCocina)
                .filter(m -> m != null && m > 0)
                .count();
        double promedioCocina = ventasConTiempo == 0 ? 0 : (double) sumaMinutos / ventasConTiempo;

        Map<String, Integer> platos = new LinkedHashMap<>();
        ventas.forEach(v -> v.getPedido().getDetalles().forEach(d ->
                platos.merge(d.getPlato().getNombre(), d.getCantidad(), Integer::sum)));

        Map.Entry<String, Integer> topPlato = platos.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        Map<LocalDate, BigDecimal> porDia = new LinkedHashMap<>();
        ventas.stream()
                .sorted(Comparator.comparing(Venta::getFechaHoraDespacho))
                .forEach(v -> porDia.merge(v.getFechaHoraDespacho().toLocalDate(), v.getTotal(), BigDecimal::add));

        Map<MetodoPago, BigDecimal> porMetodo = new LinkedHashMap<>();
        for (MetodoPago mp : MetodoPago.values()) porMetodo.put(mp, BigDecimal.ZERO);
        ventas.forEach(v -> {
            Pago pago = pagos.get(v.getPedido().getNumero());
            if (pago != null) porMetodo.merge(pago.getMetodo(), v.getTotal(), BigDecimal::add);
        });

        List<Map<String, Object>> ventasPorDia = porDia.entrySet().stream().map(e -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fecha", e.getKey());
            item.put("total", e.getValue());
            return item;
        }).toList();

        List<Map<String, Object>> ventasPorMetodo = porMetodo.entrySet().stream().map(e -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("metodo", e.getKey().name());
            item.put("total", e.getValue());
            return item;
        }).toList();

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalVendido", total);
        r.put("pedidosDespachados", ventas.size());
        r.put("ticketPromedio", ticketPromedio);
        r.put("platoMasVendido", topPlato == null ? "Sin ventas" : topPlato.getKey());
        r.put("cantidadPlatoMasVendido", topPlato == null ? 0 : topPlato.getValue());
        r.put("tiempoPromedioCocina", Math.round(promedioCocina * 10.0) / 10.0);
        r.put("ventasPorDia", ventasPorDia);
        r.put("ventasPorMetodo", ventasPorMetodo);
        return r;
    }

    public List<Map<String, Object>> ventas(LocalDate desde, LocalDate hasta, MetodoPago metodo) {
        Map<Integer, Pago> pagos = pagosPorPedido();
        return filtrarVentas(desde, hasta, metodo).stream().map(v -> {
            Pago pago = pagos.get(v.getPedido().getNumero());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("numero", v.getPedido().getNumero());
            item.put("cliente", v.getPedido().getCliente().getNombre());
            item.put("fechaHoraDespacho", v.getFechaHoraDespacho());
            item.put("minutosCocina", v.getMinutosCocina());
            item.put("metodoPago", pago == null ? "SIN_REGISTRO" : pago.getMetodo().name());
            item.put("total", v.getTotal());
            return item;
        }).toList();
    }

    private List<Venta> filtrarVentas(LocalDate desde, LocalDate hasta, MetodoPago metodo) {
        validarRango(desde, hasta);
        List<Venta> ventas;
        if (desde == null && hasta == null) {
            ventas = ventaRepository.findAllByOrderByFechaHoraDespachoDesc();
        } else {
            LocalDate d = desde == null ? LocalDate.of(2000, 1, 1) : desde;
            LocalDate h = hasta == null ? LocalDate.now() : hasta;
            LocalDateTime inicio = d.atStartOfDay();
            LocalDateTime fin = h.atTime(LocalTime.MAX);
            ventas = ventaRepository.findByFechaHoraDespachoBetweenOrderByFechaHoraDespachoDesc(inicio, fin);
        }

        if (metodo == null) return ventas;
        Map<Integer, Pago> pagos = pagosPorPedido();
        return ventas.stream()
                .filter(v -> {
                    Pago pago = pagos.get(v.getPedido().getNumero());
                    return pago != null && pago.getMetodo() == metodo;
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Map<Integer, Pago> pagosPorPedido() {
        return pagoRepository.findAll().stream().collect(Collectors.toMap(
                p -> p.getPedido().getNumero(),
                p -> p,
                (a, b) -> a,
                LinkedHashMap::new
        ));
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ReglaNegocioException("La fecha Desde no puede ser posterior a la fecha Hasta");
        }
    }
}
