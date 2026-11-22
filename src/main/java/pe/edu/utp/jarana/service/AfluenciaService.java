package pe.edu.utp.jarana.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import pe.edu.utp.jarana.repository.VentaRepository;

/** Recomendación de personal según la afluencia histórica por día de la semana. */
@Service
public class AfluenciaService {
    public static final int SEMANAS_MIN = 2;
    public static final int SEMANAS_MAX = 26;
    public static final int SEMANAS_DEFECTO = 8;

    private final VentaRepository ventaRepository;

    public AfluenciaService(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    public Map<String, Object> analizar(Integer semanas) {
        int s = semanas == null ? SEMANAS_DEFECTO : Math.max(SEMANAS_MIN, Math.min(SEMANAS_MAX, semanas));
        List<AfluenciaAnalisis.Registro> registros = ventaRepository.findAllByOrderByFechaHoraDespachoDesc().stream()
                .map(v -> new AfluenciaAnalisis.Registro(v.getFechaHoraPedido(), v.getMinutosCocina()))
                .toList();
        return AfluenciaAnalisis.analizar(registros, LocalDate.now(), s);
    }
}
