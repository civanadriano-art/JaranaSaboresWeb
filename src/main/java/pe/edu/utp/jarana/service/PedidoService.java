package pe.edu.utp.jarana.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.jarana.dto.CrearPedidoRequest;
import pe.edu.utp.jarana.dto.DetallePedidoRequest;
import pe.edu.utp.jarana.dto.PedidoResponse;
import pe.edu.utp.jarana.estructuras.ListaEnlazada;
import pe.edu.utp.jarana.exception.ReglaNegocioException;
import pe.edu.utp.jarana.model.*;
import pe.edu.utp.jarana.repository.*;

@Service
public class PedidoService {
    private static final int LIMITE_PENDIENTES = 50;
    private static final int LIMITE_ELIMINADOS = 50;

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final PlatoRepository platoRepository;
    private final PedidoEliminadoRepository eliminadoRepository;
    private final VentaRepository ventaRepository;
    private final PagoRepository pagoRepository;
    private final EstructurasDatosService estructuras;

    public PedidoService(PedidoRepository pedidoRepository,
                         ClienteRepository clienteRepository,
                         PlatoRepository platoRepository,
                         PedidoEliminadoRepository eliminadoRepository,
                         VentaRepository ventaRepository,
                         PagoRepository pagoRepository,
                         EstructurasDatosService estructuras) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.platoRepository = platoRepository;
        this.eliminadoRepository = eliminadoRepository;
        this.ventaRepository = ventaRepository;
        this.pagoRepository = pagoRepository;
        this.estructuras = estructuras;
    }

    public List<PedidoResponse> listarActivos() {
        List<PedidoResponse> salida = new ArrayList<>();
        for (EstadoPedido estado : List.of(EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO)) {
            pedidoRepository.findByEstadoOrderByNumeroAsc(estado).forEach(p -> salida.add(aResponse(p)));
        }
        salida.sort((a,b) -> a.numero().compareTo(b.numero()));
        return salida;
    }

    public List<PedidoResponse> listarPorEstado(EstadoPedido estado) {
        return pedidoRepository.findByEstadoOrderByNumeroAsc(estado).stream().map(this::aResponse).toList();
    }

    public PedidoResponse obtener(Integer numero) { return aResponse(buscarPedido(numero)); }

    @Transactional
    public PedidoResponse registrar(CrearPedidoRequest req) {
        long activos = pedidoRepository.countByEstadoIn(List.of(EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO));
        if (activos >= LIMITE_PENDIENTES) throw new ReglaNegocioException("Se alcanzó el límite de 50 pedidos pendientes");

        Cliente cliente = clienteRepository.findFirstByTelefono(req.getTelefono()).orElseGet(Cliente::new);
        cliente.setNombre(req.getNombreCliente().trim());
        cliente.setTelefono(req.getTelefono());
        cliente = clienteRepository.save(cliente);

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setFechaHoraRegistro(LocalDateTime.now());
        cargarDetalles(pedido, req.getDetalles());
        pedido = pedidoRepository.save(pedido);
        estructuras.registrarPedido(pedido.getNumero());
        return aResponse(pedido);
    }

    @Transactional
    public PedidoResponse editar(Integer numero, CrearPedidoRequest req) {
        Pedido pedido = buscarPedido(numero);
        exigirEstado(pedido, EstadoPedido.PENDIENTE, "Solo se puede editar un pedido Pendiente");
        if (pagoRepository.existsByPedidoNumero(numero)) throw new ReglaNegocioException("Un pedido pagado ya no puede modificarse");
        pedido.getCliente().setNombre(req.getNombreCliente().trim());
        pedido.getCliente().setTelefono(req.getTelefono());
        clienteRepository.save(pedido.getCliente());
        pedido.limpiarDetalles();
        cargarDetalles(pedido, req.getDetalles());
        return aResponse(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponse iniciarPreparacion(Integer numero) {
        Pedido pedido = buscarPedido(numero);
        exigirEstado(pedido, EstadoPedido.PENDIENTE, "El pedido debe estar Pendiente");
        pedido.setEstado(EstadoPedido.EN_PREPARACION);
        pedido.setFechaHoraInicioPreparacion(LocalDateTime.now());
        return aResponse(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponse marcarListo(Integer numero) {
        Pedido pedido = buscarPedido(numero);
        exigirEstado(pedido, EstadoPedido.EN_PREPARACION, "El pedido debe estar En Preparación");
        pedido.setEstado(EstadoPedido.LISTO);
        pedido.setFechaHoraListo(LocalDateTime.now());
        return aResponse(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponse despachar(Integer numero) {
        Pedido pedido = buscarPedido(numero);
        exigirEstado(pedido, EstadoPedido.LISTO, "Solo se pueden despachar pedidos Listos");
        pedido.setEstado(EstadoPedido.DESPACHADO);
        pedido.setFechaHoraDespacho(LocalDateTime.now());
        pedido = pedidoRepository.save(pedido);
        estructuras.retirarPedido(numero);

        Venta venta = new Venta();
        venta.setPedido(pedido);
        venta.setFechaHoraPedido(pedido.getFechaHoraRegistro());
        venta.setFechaHoraDespacho(pedido.getFechaHoraDespacho());
        venta.setTotal(calcularTotal(pedido));
        if (pedido.getFechaHoraInicioPreparacion() != null && pedido.getFechaHoraListo() != null) {
            venta.setMinutosCocina(Duration.between(pedido.getFechaHoraInicioPreparacion(), pedido.getFechaHoraListo()).toMinutes());
        }
        venta = ventaRepository.save(venta);
        estructuras.registrarVenta(venta.getIdVenta());
        return aResponse(pedido);
    }

    @Transactional
    public void cancelar(Integer numero, String motivo) {
        Pedido pedido = buscarPedido(numero);
        exigirEstado(pedido, EstadoPedido.PENDIENTE, "Solo se pueden cancelar pedidos Pendientes");
        if (pagoRepository.existsByPedidoNumero(numero)) throw new ReglaNegocioException("Un pedido pagado ya no puede cancelarse");
        if (eliminadoRepository.count() >= LIMITE_ELIMINADOS) throw new ReglaNegocioException("El historial de eliminados alcanzó el límite de 50 registros");
        pedido.setEstado(EstadoPedido.CANCELADO);
        pedidoRepository.save(pedido);
        estructuras.retirarPedido(numero);

        PedidoEliminado eliminado = new PedidoEliminado();
        eliminado.setPedido(pedido);
        eliminado.setMotivo(motivo == null || motivo.isBlank() ? "Sin motivo especificado" : motivo.trim());
        eliminado.setFechaHoraEliminacion(LocalDateTime.now());
        eliminado = eliminadoRepository.save(eliminado);
        estructuras.registrarEliminado(eliminado);
    }

    @Transactional
    public PedidoResponse restaurarUltimo() {
        if (pedidoRepository.countByEstadoIn(List.of(EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO)) >= LIMITE_PENDIENTES)
            throw new ReglaNegocioException("No hay espacio para restaurar: existen 50 pedidos activos");

        Long idEstructura = estructuras.ultimoEliminado();
        PedidoEliminado eliminado = idEstructura == null
                ? eliminadoRepository.findTopByOrderByIdEliminacionDesc().orElseThrow(() -> new ReglaNegocioException("No hay pedidos eliminados para restaurar"))
                : eliminadoRepository.findById(idEstructura).orElseThrow(() -> new ReglaNegocioException("No se encontró el pedido eliminado"));

        Pedido pedido = eliminado.getPedido();
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setFechaHoraRegistro(LocalDateTime.now());
        pedido.setFechaHoraInicioPreparacion(null);
        pedido.setFechaHoraListo(null);
        pedido.setFechaHoraDespacho(null);
        pedidoRepository.save(pedido);
        eliminadoRepository.delete(eliminado);
        estructuras.retirarEliminado(eliminado.getIdEliminacion());
        estructuras.registrarPedido(pedido.getNumero());
        return aResponse(pedido);
    }

    public List<PedidoEliminado> listarEliminados() { return eliminadoRepository.findAllByOrderByIdEliminacionDesc(); }

    private Pedido buscarPedido(Integer numero) {
        return pedidoRepository.findById(numero).orElseThrow(() -> new ReglaNegocioException("Pedido #" + numero + " no encontrado"));
    }

    private void exigirEstado(Pedido pedido, EstadoPedido esperado, String mensaje) {
        if (pedido.getEstado() != esperado) throw new ReglaNegocioException(mensaje);
    }

    private void cargarDetalles(Pedido pedido, List<DetallePedidoRequest> detallesJson) {
        ListaEnlazada<DetallePedidoRequest> lista = new ListaEnlazada<>();
        detallesJson.forEach(lista::agregar);
        if (lista.estaVacia()) throw new ReglaNegocioException("El pedido debe tener al menos un plato");

        lista.paraCada(item -> {
            Plato plato = platoRepository.findById(item.getCodigoPlato())
                    .filter(Plato::isActivo)
                    .orElseThrow(() -> new ReglaNegocioException("Plato no disponible: " + item.getCodigoPlato()));
            if (item.getCantidad() <= 0) throw new ReglaNegocioException("La cantidad debe ser mayor que 0");
            DetallePedido detalle = new DetallePedido();
            detalle.setPlato(plato);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(plato.getPrecio());
            detalle.setObservacion(item.getObservacion());
            pedido.agregarDetalle(detalle);
        });
    }

    private BigDecimal calcularTotal(Pedido pedido) {
        return pedido.getDetalles().stream()
                .map(d -> d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PedidoResponse aResponse(Pedido p) {
        var detalles = p.getDetalles().stream().map(d -> new PedidoResponse.DetalleResponse(
                d.getPlato().getCodigo(), d.getPlato().getNombre(), d.getCantidad(), d.getPrecioUnitario(), d.getObservacion()
        )).toList();
        return new PedidoResponse(p.getNumero(), p.getCliente().getNombre(), p.getCliente().getTelefono(),
                p.getEstado().getEtiqueta(), p.getFechaHoraRegistro(), calcularTotal(p), detalles);
    }
}
