package pe.edu.utp.jarana.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.edu.utp.jarana.model.Cliente;
import pe.edu.utp.jarana.model.DetallePedido;
import pe.edu.utp.jarana.model.Pedido;
import pe.edu.utp.jarana.model.Plato;
import pe.edu.utp.jarana.model.Venta;
import pe.edu.utp.jarana.repository.PagoRepository;
import pe.edu.utp.jarana.repository.VentaRepository;

class ReporteServiceRankingTest {

    @Test
    @SuppressWarnings("unchecked")
    void rankingOrdenaPorCantidadYEnEmpateGanaElPrimero() {
        Plato lomo = plato(1, "Lomo saltado", "25.00");
        Plato ceviche = plato(2, "Ceviche", "30.00");
        Plato chicha = plato(3, "Chicha", "8.00");

        VentaRepository ventas = mock(VentaRepository.class);
        PagoRepository pagos = mock(PagoRepository.class);
        when(ventas.findAllByOrderByFechaHoraDespachoDesc()).thenReturn(List.of(
                venta(1, "58.00", detalle(lomo, 2, "0"), detalle(chicha, 1, "0")),
                venta(2, "70.00", detalle(ceviche, 2, "2.00"), detalle(chicha, 1, "0")),
                venta(3, "55.00", detalle(ceviche, 1, "0"), detalle(lomo, 1, "0"))));
        when(pagos.findAll()).thenReturn(List.of());

        Map<String, Object> resumen = new ReporteService(ventas, pagos).resumen(null, null, null);
        List<Map<String, Object>> ranking = (List<Map<String, Object>>) resumen.get("platosVendidos");

        // Lomo y Ceviche empatan con 3 unidades: gana el que se vendió primero
        assertEquals("Lomo saltado", resumen.get("platoMasVendido"));
        assertEquals(3, resumen.get("cantidadPlatoMasVendido"));
        assertEquals(3, ranking.size());
        assertEquals("Lomo saltado", ranking.get(0).get("plato"));
        assertEquals("Ceviche", ranking.get(1).get("plato"));
        assertEquals("Chicha", ranking.get(2).get("plato"));
        assertEquals(0, new BigDecimal("75.00").compareTo((BigDecimal) ranking.get(0).get("importe")));
        assertEquals(0, new BigDecimal("92.00").compareTo((BigDecimal) ranking.get(1).get("importe")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sinVentasNoHayPlatoMasVendido() {
        VentaRepository ventas = mock(VentaRepository.class);
        PagoRepository pagos = mock(PagoRepository.class);
        when(ventas.findAllByOrderByFechaHoraDespachoDesc()).thenReturn(List.of());
        when(pagos.findAll()).thenReturn(List.of());

        Map<String, Object> resumen = new ReporteService(ventas, pagos).resumen(null, null, null);

        assertEquals("Sin ventas", resumen.get("platoMasVendido"));
        assertTrue(((List<Object>) resumen.get("platosVendidos")).isEmpty());
    }

    private static Plato plato(int codigo, String nombre, String precio) {
        Plato p = new Plato();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setPrecio(new BigDecimal(precio));
        return p;
    }

    private static DetallePedido detalle(Plato plato, int cantidad, String extra) {
        DetallePedido d = new DetallePedido();
        d.setPlato(plato);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(plato.getPrecio());
        d.setCostoExtra(new BigDecimal(extra));
        return d;
    }

    private static Venta venta(int numero, String total, DetallePedido... detalles) {
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente " + numero);
        Pedido pedido = new Pedido();
        pedido.setNumero(numero);
        pedido.setCliente(cliente);
        pedido.setDetalles(Arrays.asList(detalles));
        Venta v = new Venta();
        v.setPedido(pedido);
        v.setTotal(new BigDecimal(total));
        v.setFechaHoraDespacho(LocalDateTime.of(2026, 11, 2, 12, numero));
        v.setMinutosCocina(10L);
        return v;
    }
}
