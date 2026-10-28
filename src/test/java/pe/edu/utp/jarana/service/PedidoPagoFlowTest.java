package pe.edu.utp.jarana.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.utp.jarana.exception.ReglaNegocioException;
import pe.edu.utp.jarana.model.Cliente;
import pe.edu.utp.jarana.model.EstadoPedido;
import pe.edu.utp.jarana.model.Pedido;
import pe.edu.utp.jarana.model.Venta;
import pe.edu.utp.jarana.repository.*;

class PedidoPagoFlowTest {
    private PedidoRepository pedidos;
    private PagoRepository pagos;
    private VentaRepository ventas;
    private EstructurasDatosService estructuras;
    private PedidoService service;
    private Pedido pedido;

    @BeforeEach
    void setup() {
        pedidos = mock(PedidoRepository.class);
        pagos = mock(PagoRepository.class);
        ventas = mock(VentaRepository.class);
        estructuras = mock(EstructurasDatosService.class);
        service = new PedidoService(
                pedidos,
                mock(ClienteRepository.class),
                mock(PlatoRepository.class),
                mock(PedidoEliminadoRepository.class),
                ventas,
                pagos,
                estructuras);

        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente prueba");
        pedido = new Pedido();
        pedido.setNumero(101);
        pedido.setCliente(cliente);
        pedido.setEstado(EstadoPedido.LISTO);
        pedido.setFechaHoraRegistro(LocalDateTime.now().minusMinutes(20));
        pedido.setFechaHoraInicioPreparacion(LocalDateTime.now().minusMinutes(15));
        pedido.setFechaHoraListo(LocalDateTime.now().minusMinutes(2));
        when(pedidos.findById(101)).thenReturn(Optional.of(pedido));
        when(pedidos.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));
        when(ventas.save(any(Venta.class))).thenAnswer(i -> {
            Venta v = i.getArgument(0);
            v.setIdVenta(1L);
            return v;
        });
    }

    @Test
    void noPermiteDespacharSinPago() {
        when(pagos.existsByPedidoNumero(101)).thenReturn(false);
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> service.despachar(101));
        assertTrue(ex.getMessage().toLowerCase().contains("pagado"));
        assertEquals(EstadoPedido.LISTO, pedido.getEstado());
        verify(ventas, never()).save(any());
    }

    @Test
    void permiteDespacharCuandoExistePago() {
        when(pagos.existsByPedidoNumero(101)).thenReturn(true);
        var respuesta = service.despachar(101);
        assertEquals("Despachado", respuesta.estado());
        assertEquals(EstadoPedido.DESPACHADO, pedido.getEstado());
        verify(ventas).save(any(Venta.class));
        verify(estructuras).retirarPedido(101);
    }
}
