package pe.edu.utp.jarana.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.jarana.dto.PlatoRequest;
import pe.edu.utp.jarana.exception.ReglaNegocioException;
import pe.edu.utp.jarana.model.Plato;
import pe.edu.utp.jarana.repository.PlatoRepository;

@Service
public class PlatoService {
    private final PlatoRepository platoRepository;
    private final EstructurasDatosService estructuras;

    public PlatoService(PlatoRepository platoRepository, EstructurasDatosService estructuras) {
        this.platoRepository = platoRepository;
        this.estructuras = estructuras;
    }

    public List<Plato> listarActivos() { return platoRepository.findByActivoTrueOrderByNombreAsc(); }

    public Plato buscar(Integer codigo) {
        if (estructuras.buscarPlatoPorCodigo(codigo) == null) throw new ReglaNegocioException("Plato no encontrado");
        return platoRepository.findById(codigo).orElseThrow(() -> new ReglaNegocioException("Plato no encontrado"));
    }

    @Transactional
    public Plato crear(PlatoRequest req) {
        String nombre = req.getNombre().trim();
        if (platoRepository.findByNombreIgnoreCase(nombre).isPresent())
            throw new ReglaNegocioException("Ya existe un plato con ese nombre");
        Plato p = new Plato();
        aplicar(req, p);
        p.setActivo(true);
        p = platoRepository.save(p);
        estructuras.registrarPlato(p);
        return p;
    }

    @Transactional
    public Plato modificar(Integer codigo, PlatoRequest req) {
        Plato p = buscar(codigo);
        platoRepository.findByNombreIgnoreCase(req.getNombre().trim()).ifPresent(otro -> {
            if (!otro.getCodigo().equals(codigo)) throw new ReglaNegocioException("Ya existe otro plato con ese nombre");
        });
        estructuras.retirarPlato(p);
        aplicar(req, p);
        p = platoRepository.save(p);
        estructuras.registrarPlato(p);
        return p;
    }

    @Transactional
    public void desactivar(Integer codigo) {
        Plato p = buscar(codigo);
        estructuras.retirarPlato(p);
        p.setActivo(false);
        platoRepository.save(p);
    }

    private void aplicar(PlatoRequest req, Plato p) {
        p.setNombre(req.getNombre().trim());
        p.setCategoria(req.getCategoria().trim());
        p.setPrecio(req.getPrecio());
        p.setDisponible(req.isDisponible());
    }
}
