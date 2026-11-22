package pe.edu.utp.jarana.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Análisis de afluencia por día de la semana (sin dependencias de Spring, fácil de probar).
 *
 * Idea: con el historial de pedidos se calcula cuántos pedidos entran, en promedio, cada lunes, martes, etc.
 * y se compara con el promedio general de todos los días con actividad. Si un día supera el promedio general
 * por el umbral definido, se recomienda reforzar el personal ese día.
 *
 * Solo se recomienda cuando hay datos suficientes; con pocos pedidos los porcentajes no significan nada.
 */
public final class AfluenciaAnalisis {
    /** Un pedido despachado: cuándo se registró y cuánto demoró en cocina. */
    public record Registro(LocalDateTime fechaPedido, Long minutosCocina) {}

    /** Mínimo de días con pedidos en el periodo para poder recomendar. */
    public static final int MIN_DIAS_CON_DATOS = 14;
    /** Mínimo de veces que debe haberse observado un mismo día de la semana (p. ej. 2 sábados). */
    public static final int MIN_OBSERVACIONES_POR_DIA = 2;
    /** 1.25 = el día recibe 25 % más pedidos que el promedio general. */
    public static final double UMBRAL_ALTA = 1.25;
    /** 0.75 = el día recibe 25 % menos pedidos que el promedio general. */
    public static final double UMBRAL_BAJA = 0.75;

    private static final String[] NOMBRES = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    private AfluenciaAnalisis() {}

    public static String nombreDia(DayOfWeek dia) {
        return NOMBRES[dia.getValue() - 1];
    }

    /**
     * @param registros pedidos despachados
     * @param hoy       fecha actual; el día de hoy se excluye porque todavía está incompleto
     * @param semanas   cuántas semanas hacia atrás se analizan
     */
    public static Map<String, Object> analizar(List<Registro> registros, LocalDate hoy, int semanas) {
        LocalDate desde = hoy.minusWeeks(semanas);
        LocalDate hasta = hoy.minusDays(1);

        Map<DayOfWeek, Map<LocalDate, Integer>> pedidosPorFecha = new EnumMap<>(DayOfWeek.class);
        Map<DayOfWeek, int[]> pedidosPorHora = new EnumMap<>(DayOfWeek.class);
        Map<DayOfWeek, long[]> cocina = new EnumMap<>(DayOfWeek.class); // [suma, cantidad]
        for (DayOfWeek d : DayOfWeek.values()) {
            pedidosPorFecha.put(d, new TreeMap<>());
            pedidosPorHora.put(d, new int[24]);
            cocina.put(d, new long[2]);
        }

        int totalPedidos = 0;
        Map<LocalDate, Boolean> diasConActividad = new TreeMap<>();
        for (Registro r : registros) {
            if (r.fechaPedido() == null) continue;
            LocalDate fecha = r.fechaPedido().toLocalDate();
            if (fecha.isBefore(desde) || fecha.isAfter(hasta)) continue;
            DayOfWeek dia = fecha.getDayOfWeek();
            pedidosPorFecha.get(dia).merge(fecha, 1, Integer::sum);
            pedidosPorHora.get(dia)[r.fechaPedido().getHour()]++;
            if (r.minutosCocina() != null && r.minutosCocina() > 0) {
                cocina.get(dia)[0] += r.minutosCocina();
                cocina.get(dia)[1]++;
            }
            diasConActividad.put(fecha, Boolean.TRUE);
            totalPedidos++;
        }

        int diasConDatos = diasConActividad.size();
        double promedioGeneral = diasConDatos == 0 ? 0 : (double) totalPedidos / diasConDatos;
        boolean suficiente = diasConDatos >= MIN_DIAS_CON_DATOS;

        List<Map<String, Object>> dias = new ArrayList<>();
        List<String> refuerzo = new ArrayList<>();
        for (DayOfWeek dia : DayOfWeek.values()) {
            Map<LocalDate, Integer> porFecha = pedidosPorFecha.get(dia);
            int observados = porFecha.size();
            int pedidos = porFecha.values().stream().mapToInt(Integer::intValue).sum();
            double promedio = observados == 0 ? 0 : (double) pedidos / observados;
            double variacion = promedioGeneral == 0 || observados == 0 ? 0 : (promedio / promedioGeneral - 1) * 100;

            String nivel;
            if (!suficiente || observados < MIN_OBSERVACIONES_POR_DIA) nivel = "SIN_DATOS";
            else if (promedio >= promedioGeneral * UMBRAL_ALTA) nivel = "ALTA";
            else if (promedio <= promedioGeneral * UMBRAL_BAJA) nivel = "BAJA";
            else nivel = "NORMAL";

            String nombre = nombreDia(dia);
            if ("ALTA".equals(nivel)) refuerzo.add(nombre);

            long[] c = cocina.get(dia);
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("dia", nombre);
            fila.put("diasObservados", observados);
            fila.put("pedidos", pedidos);
            fila.put("promedioPedidos", redondear(promedio));
            fila.put("variacionPct", redondear(variacion));
            fila.put("horaPico", horaPico(pedidosPorHora.get(dia)));
            fila.put("minutosCocina", c[1] == 0 ? null : redondear((double) c[0] / c[1]));
            fila.put("nivel", nivel);
            fila.put("recomendacion", recomendacion(nivel, observados));
            dias.add(fila);
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("semanas", semanas);
        r.put("desde", desde.toString());
        r.put("hasta", hasta.toString());
        r.put("diasConDatos", diasConDatos);
        r.put("minimoDiasRequerido", MIN_DIAS_CON_DATOS);
        r.put("totalPedidos", totalPedidos);
        r.put("promedioGeneral", redondear(promedioGeneral));
        r.put("suficiente", suficiente);
        r.put("diasRefuerzo", refuerzo);
        r.put("mensaje", mensaje(suficiente, diasConDatos, refuerzo));
        r.put("dias", dias);
        return r;
    }

    private static String mensaje(boolean suficiente, int diasConDatos, List<String> refuerzo) {
        if (!suficiente) {
            return "Aún no hay datos suficientes: se necesitan al menos " + MIN_DIAS_CON_DATOS
                    + " días con pedidos despachados en el periodo y hay " + diasConDatos + ".";
        }
        if (refuerzo.isEmpty()) {
            return "La afluencia es parecida entre los días analizados; no se detecta un día que requiera personal adicional.";
        }
        return "Se recomienda reforzar el personal los días: " + String.join(", ", refuerzo) + ".";
    }

    private static String recomendacion(String nivel, int observados) {
        return switch (nivel) {
            case "ALTA" -> "Reforzar personal";
            case "BAJA" -> "Personal habitual (baja afluencia)";
            case "NORMAL" -> "Personal habitual";
            default -> observados < MIN_OBSERVACIONES_POR_DIA ? "Sin datos suficientes de este día" : "Sin datos suficientes";
        };
    }

    private static String horaPico(int[] porHora) {
        int mejor = -1;
        int max = 0;
        for (int h = 0; h < 24; h++) {
            if (porHora[h] > max) { max = porHora[h]; mejor = h; }
        }
        return mejor < 0 ? null : String.format("%02d:00 - %02d:00", mejor, (mejor + 1) % 24);
    }

    private static double redondear(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
