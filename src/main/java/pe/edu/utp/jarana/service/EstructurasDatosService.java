package pe.edu.utp.jarana.service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Locale;
import org.springframework.stereotype.Service;
import pe.edu.utp.jarana.estructuras.ArbolAVL;
import pe.edu.utp.jarana.estructuras.ListaEnlazada;
import pe.edu.utp.jarana.model.EstadoPedido;
import pe.edu.utp.jarana.model.PedidoEliminado;
import pe.edu.utp.jarana.model.Plato;
import pe.edu.utp.jarana.repository.PedidoEliminadoRepository;
import pe.edu.utp.jarana.repository.PedidoRepository;
import pe.edu.utp.jarana.repository.PlatoRepository;
import pe.edu.utp.jarana.repository.VentaRepository;

@Service
public class EstructurasDatosService {
    private final PedidoRepository pedidoRepository;
    private final PedidoEliminadoRepository eliminadoRepository;
    private final PlatoRepository platoRepository;
    private final VentaRepository ventaRepository;

    private final ArbolAVL<Integer, Integer> pedidosPendientes = new ArbolAVL<>();
    private final ArbolAVL<Long, Long> pedidosEliminados = new ArbolAVL<>();
    private final ArbolAVL<String, Integer> catalogoPorNombre = new ArbolAVL<>();
    private final HashMap<Integer, Integer> platosPorCodigo = new HashMap<>();
    private final ListaEnlazada<Long> historialVentas = new ListaEnlazada<>();

    public EstructurasDatosService(PedidoRepository pedidoRepository,
                                   PedidoEliminadoRepository eliminadoRepository,
                                   PlatoRepository platoRepository,
                                   VentaRepository ventaRepository) {
        this.pedidoRepository = pedidoRepository;
        this.eliminadoRepository = eliminadoRepository;
        this.platoRepository = platoRepository;
        this.ventaRepository = ventaRepository;
    }

    @PostConstruct
    public synchronized void cargarDesdeBaseDatos() {
        pedidosPendientes.limpiar();
        pedidosEliminados.limpiar();
        catalogoPorNombre.limpiar();
        platosPorCodigo.clear();
        historialVentas.limpiar();
        for (EstadoPedido estado : new EstadoPedido[]{EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO}) {
            pedidoRepository.findByEstadoOrderByNumeroAsc(estado)
                    .forEach(p -> pedidosPendientes.insertar(p.getNumero(), p.getNumero()));
        }
        eliminadoRepository.findAll().forEach(e -> pedidosEliminados.insertar(e.getIdEliminacion(), e.getIdEliminacion()));
        platoRepository.findByActivoTrueOrderByNombreAsc().forEach(this::registrarPlato);
        ventaRepository.findAll().forEach(v -> historialVentas.agregar(v.getIdVenta()));
    }

    public synchronized void registrarPedido(Integer numero) { pedidosPendientes.insertar(numero, numero); }
    public synchronized void retirarPedido(Integer numero) { pedidosPendientes.eliminar(numero); }
    public synchronized Integer siguientePedido() { return pedidosPendientes.obtenerMenor(); }
    public synchronized int cantidadPendientesEnEstructura() { return pedidosPendientes.getCantidad(); }

    public synchronized void registrarEliminado(PedidoEliminado eliminado) {
        pedidosEliminados.insertar(eliminado.getIdEliminacion(), eliminado.getIdEliminacion());
    }
    public synchronized void retirarEliminado(Long id) { pedidosEliminados.eliminar(id); }
    public synchronized Long ultimoEliminado() { return pedidosEliminados.obtenerMayor(); }

    public synchronized void registrarPlato(Plato plato) {
        if (plato == null || plato.getCodigo() == null || !plato.isActivo()) return;
        catalogoPorNombre.insertar(normalizar(plato.getNombre()), plato.getCodigo());
        platosPorCodigo.put(plato.getCodigo(), plato.getCodigo());
    }
    public synchronized void retirarPlato(Plato plato) {
        if (plato == null || plato.getCodigo() == null) return;
        catalogoPorNombre.eliminar(normalizar(plato.getNombre()));
        platosPorCodigo.remove(plato.getCodigo());
    }
    public synchronized Integer buscarPlatoPorCodigo(Integer codigo) { return platosPorCodigo.get(codigo); }
    public synchronized Integer buscarPlatoPorNombre(String nombre) { return catalogoPorNombre.buscar(normalizar(nombre)); }

    public synchronized void registrarVenta(Long idVenta) { historialVentas.agregar(idVenta); }
    public synchronized int cantidadVentasEnLista() { return historialVentas.getCantidad(); }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }
}
