package pe.edu.utp.jarana.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.edu.utp.jarana.exception.ReglaNegocioException;
import pe.edu.utp.jarana.model.Cliente;
import pe.edu.utp.jarana.model.EstadoPedido;
import pe.edu.utp.jarana.model.Pedido;
import pe.edu.utp.jarana.model.PedidoEliminado;
import pe.edu.utp.jarana.repository.*;

class PedidoRestauracionTest {
    @Test
    void restauraElPedidoSeleccionadoPorId() {
        PedidoRepository pedidos = mock(PedidoRepository.class);
        PedidoEliminadoRepository eliminados = mock(PedidoEliminadoRepository.class);
        PagoRepository pagos = mock(PagoRepository.class);
        EstructurasDatosService estructuras = mock(EstructurasDatosService.class);
        PedidoService service = new PedidoService(pedidos, mock(ClienteRepository.class), mock(PlatoRepository.class), eliminados, mock(VentaRepository.class), pagos, estructuras);

        Cliente cliente = new Cliente(); cliente.setNombre("Cliente");
        Pedido pedido = new Pedido(); pedido.setNumero(7); pedido.setCliente(cliente); pedido.setEstado(EstadoPedido.CANCELADO); pedido.setFechaHoraRegistro(LocalDateTime.now().minusMinutes(10));
        PedidoEliminado eliminado = new PedidoEliminado(); eliminado.setIdEliminacion(22L); eliminado.setPedido(pedido); eliminado.setMotivo("Error de registro");

        when(eliminados.findById(22L)).thenReturn(Optional.of(eliminado));
        when(pagos.existsByPedidoNumero(7)).thenReturn(false);
        when(pedidos.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        var respuesta = service.restaurarEliminado(22L);
        assertEquals("Pendiente", respuesta.estado());
        verify(eliminados).delete(eliminado);
        verify(estructuras).registrarPedido(7);
    }

    @Test
    void noRestauraSiElPedidoTienePago() {
        PedidoRepository pedidos = mock(PedidoRepository.class);
        PedidoEliminadoRepository eliminados = mock(PedidoEliminadoRepository.class);
        PagoRepository pagos = mock(PagoRepository.class);
        PedidoService service = new PedidoService(pedidos, mock(ClienteRepository.class), mock(PlatoRepository.class), eliminados, mock(VentaRepository.class), pagos, mock(EstructurasDatosService.class));

        Pedido pedido = new Pedido(); pedido.setNumero(9); pedido.setEstado(EstadoPedido.CANCELADO); pedido.setCliente(new Cliente());
        PedidoEliminado eliminado = new PedidoEliminado(); eliminado.setIdEliminacion(31L); eliminado.setPedido(pedido);
        when(eliminados.findById(31L)).thenReturn(Optional.of(eliminado));
        when(pagos.existsByPedidoNumero(9)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.restaurarEliminado(31L));
        verify(eliminados, never()).delete(any());
    }
}
