package pe.edu.utp.jarana.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.edu.utp.jarana.service.AfluenciaAnalisis.Registro;

class AfluenciaAnalisisTest {
    // Lunes 2026-11-23: se analizan las 4 semanas anteriores (26-oct al 22-nov).
    private static final LocalDate HOY = LocalDate.of(2026, 11, 23);

    @Test
    @SuppressWarnings("unchecked")
    void recomiendaReforzarLosDiasConMuchaMasAfluencia() {
        List<Registro> datos = new ArrayList<>();
        for (LocalDate d = HOY.minusWeeks(4); d.isBefore(HOY); d = d.plusDays(1)) {
            int pedidos = (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) ? 30 : 10;
            agregar(datos, d, pedidos, 20);
        }

        Map<String, Object> r = AfluenciaAnalisis.analizar(datos, HOY, 4);

        assertEquals(true, r.get("suficiente"));
        assertEquals(List.of("Sábado", "Domingo"), r.get("diasRefuerzo"));
        List<Map<String, Object>> dias = (List<Map<String, Object>>) r.get("dias");
        assertEquals("ALTA", dias.get(5).get("nivel"));
        assertEquals("Reforzar personal", dias.get(5).get("recomendacion"));
        assertEquals("BAJA", dias.get(0).get("nivel"));
        assertEquals("20:00 - 21:00", dias.get(5).get("horaPico"));
        assertEquals(30.0, dias.get(5).get("promedioPedidos"));
    }

    @Test
    void noRecomiendaNadaSiLaAfluenciaEsParecida() {
        List<Registro> datos = new ArrayList<>();
        for (LocalDate d = HOY.minusWeeks(4); d.isBefore(HOY); d = d.plusDays(1)) agregar(datos, d, 12, 15);

        Map<String, Object> r = AfluenciaAnalisis.analizar(datos, HOY, 4);

        assertEquals(List.of(), r.get("diasRefuerzo"));
        assertTrue(((String) r.get("mensaje")).contains("no se detecta"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void conPocosDatosNoRecomienda() {
        List<Registro> datos = new ArrayList<>();
        agregar(datos, LocalDate.of(2026, 11, 21), 40, 20); // un sábado
        agregar(datos, LocalDate.of(2026, 11, 20), 5, 20);  // un viernes

        Map<String, Object> r = AfluenciaAnalisis.analizar(datos, HOY, 4);

        assertEquals(false, r.get("suficiente"));
        assertEquals(List.of(), r.get("diasRefuerzo"));
        assertTrue(((String) r.get("mensaje")).startsWith("Aún no hay datos suficientes"));
        for (Map<String, Object> dia : (List<Map<String, Object>>) r.get("dias")) {
            assertEquals("SIN_DATOS", dia.get("nivel"));
        }
    }

    @Test
    void ignoraHoyYLosPedidosFueraDelPeriodo() {
        List<Registro> datos = new ArrayList<>();
        agregar(datos, HOY, 50, 10);                 // hoy: incompleto, se excluye
        agregar(datos, HOY.minusWeeks(10), 50, 10);  // demasiado antiguo

        Map<String, Object> r = AfluenciaAnalisis.analizar(datos, HOY, 4);

        assertEquals(0, r.get("totalPedidos"));
        assertEquals(0, r.get("diasConDatos"));
    }

    @Test
    void sinVentasNoFalla() {
        Map<String, Object> r = AfluenciaAnalisis.analizar(List.of(), HOY, 8);
        assertEquals(0, r.get("totalPedidos"));
        assertEquals(false, r.get("suficiente"));
    }

    private static void agregar(List<Registro> lista, LocalDate dia, int cantidad, long minutos) {
        for (int i = 0; i < cantidad; i++) {
            lista.add(new Registro(LocalDateTime.of(dia, LocalTime.of(20, i % 60)), minutos));
        }
    }
}
