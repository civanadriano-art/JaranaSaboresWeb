package pe.edu.utp.jarana.service;

import com.google.common.base.Stopwatch;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.edu.utp.jarana.model.MetodoPago;

/**
 * Genera el reporte de ventas como un libro de Excel real (.xlsx) usando Apache POI.
 * Reutiliza los mismos datos y filtros que ya muestra la pantalla de reportes
 * (ReporteService), de modo que el archivo coincide con lo que ve el administrador.
 *
 * Hojas: Resumen (indicadores), Ventas (detalle con total) y Platos (ranking de ventas por plato).
 */
@Service
public class ReporteExcelService {
    private static final Logger log = LoggerFactory.getLogger(ReporteExcelService.class);

    private static final String FORMATO_MONEDA = "\"S/\" #,##0.00";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    // Rojo de la marca (mismo --jarana de app.css: #b91c1c)
    private static final byte[] COLOR_MARCA = {(byte) 0xB9, (byte) 0x1C, (byte) 0x1C};

    private final ReporteService reportes;

    public ReporteExcelService(ReporteService reportes) {
        this.reportes = reportes;
    }

    public byte[] generar(LocalDate desde, LocalDate hasta, MetodoPago metodo) {
        Stopwatch cronometro = Stopwatch.createStarted();
        Map<String, Object> resumen = reportes.resumen(desde, hasta, metodo);
        List<Map<String, Object>> ventas = reportes.ventas(desde, hasta, metodo);

        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            libro.getProperties().getCoreProperties().setTitle("Reporte de ventas - La Jarana de Sabores");
            libro.getProperties().getCoreProperties().setCreator("La Jarana de Sabores");

            Estilos estilos = new Estilos(libro);
            hojaResumen(libro, estilos, resumen, desde, hasta, metodo);
            hojaVentas(libro, estilos, ventas);
            hojaPlatos(libro, estilos, lista(resumen.get("platosVendidos")));

            // Calcula y guarda el resultado de las fórmulas para que también se vea en visores del celular
            XSSFFormulaEvaluator.evaluateAllFormulaCells(libro);
            libro.write(salida);
            log.info("Reporte Excel generado en {} ms: {} ventas, periodo '{}'",
                    cronometro.elapsed(TimeUnit.MILLISECONDS), ventas.size(), textoPeriodo(desde, hasta));
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel", e);
        }
    }

    // ------------------------------------------------------------------ Hoja 1: Resumen

    private void hojaResumen(XSSFWorkbook libro, Estilos e, Map<String, Object> r,
                             LocalDate desde, LocalDate hasta, MetodoPago metodo) {
        Sheet hoja = libro.createSheet("Resumen");
        hoja.setColumnWidth(0, 36 * 256);
        hoja.setColumnWidth(1, 30 * 256);
        hoja.setColumnWidth(2, 14 * 256);
        configurarImpresion(hoja, false);

        int fila = 0;
        Row titulo = hoja.createRow(fila++);
        titulo.setHeightInPoints(28);
        celda(titulo, 0, e.titulo).setCellValue("La Jarana de Sabores - Reporte de ventas");
        celda(titulo, 1, e.titulo);
        celda(titulo, 2, e.titulo);
        hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        filaTexto(hoja, fila++, "Periodo", textoPeriodo(desde, hasta), e);
        filaTexto(hoja, fila++, "Método de pago", metodo == null ? "Todos" : nombreMetodo(metodo.name()), e);
        filaTexto(hoja, fila++, "Generado el", LocalDateTime.now().format(FECHA_HORA), e);
        fila++;

        encabezado(hoja, fila++, e, "Indicador", "Valor");
        filaNumero(hoja, fila++, "Total vendido", numero(r.get("totalVendido")), e.moneda, e);
        filaNumero(hoja, fila++, "Pedidos despachados", numero(r.get("pedidosDespachados")), e.entero, e);
        filaNumero(hoja, fila++, "Ticket promedio", numero(r.get("ticketPromedio")), e.moneda, e);
        int cantidad = (int) numero(r.get("cantidadPlatoMasVendido"));
        String plato = String.valueOf(r.get("platoMasVendido"));
        filaTexto(hoja, fila++, "Plato más vendido", cantidad > 0 ? plato + " (" + cantidad + " unidades)" : plato, e);
        filaNumero(hoja, fila++, "Tiempo promedio de preparación (min)", numero(r.get("tiempoPromedioCocina")), e.decimal, e);
        fila++;

        // Ventas por método de pago, con su porcentaje (igual que en la pantalla)
        List<Map<String, Object>> porMetodo = lista(r.get("ventasPorMetodo"));
        double totalMetodos = porMetodo.stream().mapToDouble(m -> numero(m.get("total"))).sum();
        encabezado(hoja, fila++, e, "Ventas por método de pago", "Total", "% del total");
        for (Map<String, Object> item : porMetodo) {
            Row row = hoja.createRow(fila++);
            double total = numero(item.get("total"));
            celda(row, 0, e.etiqueta).setCellValue(nombreMetodo(String.valueOf(item.get("metodo"))));
            celda(row, 1, e.moneda).setCellValue(total);
            celda(row, 2, e.porcentaje).setCellValue(totalMetodos == 0 ? 0 : total / totalMetodos);
        }
        fila++;

        encabezado(hoja, fila++, e, "Ventas por día", "Total");
        for (Map<String, Object> item : lista(r.get("ventasPorDia"))) {
            Row row = hoja.createRow(fila++);
            Cell fecha = celda(row, 0, e.fecha);
            if (item.get("fecha") instanceof LocalDate d) fecha.setCellValue(aFecha(d.atStartOfDay()));
            celda(row, 1, e.moneda).setCellValue(numero(item.get("total")));
        }
    }

    // ------------------------------------------------------------------ Hoja 2: Ventas

    private void hojaVentas(XSSFWorkbook libro, Estilos e, List<Map<String, Object>> ventas) {
        Sheet hoja = libro.createSheet("Ventas");
        int[] anchos = {12, 32, 20, 24, 18, 16};
        for (int i = 0; i < anchos.length; i++) hoja.setColumnWidth(i, anchos[i] * 256);
        configurarImpresion(hoja, true);

        encabezado(hoja, 0, e, "Pedido", "Cliente", "Despacho", "Minutos de preparación", "Método de pago", "Total");

        int fila = 1;
        for (Map<String, Object> v : ventas) {
            Row row = hoja.createRow(fila++);
            celda(row, 0, e.entero).setCellValue(numero(v.get("numero")));
            celda(row, 1, e.texto).setCellValue(String.valueOf(v.get("cliente")));

            Cell despacho = celda(row, 2, e.fechaHora);
            if (v.get("fechaHoraDespacho") instanceof LocalDateTime f) despacho.setCellValue(aFecha(f));

            Cell minutos = celda(row, 3, e.entero);
            if (v.get("minutosCocina") instanceof Number m) minutos.setCellValue(m.doubleValue());

            celda(row, 4, e.texto).setCellValue(nombreMetodo(String.valueOf(v.get("metodoPago"))));
            celda(row, 5, e.moneda).setCellValue(numero(v.get("total")));
        }

        if (!ventas.isEmpty()) {
            Row total = hoja.createRow(fila);
            celda(total, 4, e.totalEtiqueta).setCellValue("TOTAL");
            Cell suma = celda(total, 5, e.monedaTotal);
            suma.setCellFormula("SUM(F2:F" + fila + ")");
        }

        hoja.createFreezePane(0, 1);
        hoja.setAutoFilter(new CellRangeAddress(0, Math.max(ventas.size(), 1), 0, 5));
    }

    // ------------------------------------------------------------------ Hoja 3: Platos

    private void hojaPlatos(XSSFWorkbook libro, Estilos e, List<Map<String, Object>> platos) {
        Sheet hoja = libro.createSheet("Platos");
        int[] anchos = {8, 36, 14, 18};
        for (int i = 0; i < anchos.length; i++) hoja.setColumnWidth(i, anchos[i] * 256);
        configurarImpresion(hoja, false);

        encabezado(hoja, 0, e, "N.º", "Plato", "Cantidad", "Importe vendido");
        int fila = 1;
        for (Map<String, Object> p : platos) {
            Row row = hoja.createRow(fila);
            celda(row, 0, e.entero).setCellValue(fila);
            celda(row, 1, e.texto).setCellValue(String.valueOf(p.get("plato")));
            celda(row, 2, e.entero).setCellValue(numero(p.get("cantidad")));
            celda(row, 3, e.moneda).setCellValue(numero(p.get("importe")));
            fila++;
        }
        hoja.createFreezePane(0, 1);
    }

    // ------------------------------------------------------------------ Utilidades

    /** Deja la hoja lista para imprimir: ajustada al ancho de la página y con numeración. */
    private static void configurarImpresion(Sheet hoja, boolean horizontal) {
        hoja.setFitToPage(true);
        hoja.getPrintSetup().setFitWidth((short) 1);
        hoja.getPrintSetup().setFitHeight((short) 0);
        hoja.getPrintSetup().setLandscape(horizontal);
        hoja.getFooter().setCenter("Página &P de &N");
    }

    private static Cell celda(Row row, int columna, XSSFCellStyle estilo) {
        Cell c = row.createCell(columna);
        c.setCellStyle(estilo);
        return c;
    }

    private static void encabezado(Sheet hoja, int fila, Estilos e, String... titulos) {
        Row row = hoja.createRow(fila);
        row.setHeightInPoints(22);
        for (int i = 0; i < titulos.length; i++) celda(row, i, e.encabezado).setCellValue(titulos[i]);
    }

    private static void filaTexto(Sheet hoja, int fila, String etiqueta, String valor, Estilos e) {
        Row row = hoja.createRow(fila);
        celda(row, 0, e.etiqueta).setCellValue(etiqueta);
        celda(row, 1, e.texto).setCellValue(valor);
    }

    private static void filaNumero(Sheet hoja, int fila, String etiqueta, double valor, XSSFCellStyle estilo, Estilos e) {
        Row row = hoja.createRow(fila);
        celda(row, 0, e.etiqueta).setCellValue(etiqueta);
        celda(row, 1, estilo).setCellValue(valor);
    }

    private static double numero(Object valor) {
        return valor instanceof Number n ? n.doubleValue() : 0d;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> lista(Object valor) {
        return valor instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
    }

    private static Date aFecha(LocalDateTime fecha) {
        return Date.from(fecha.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static String nombreMetodo(String metodo) {
        return switch (metodo) {
            case "EFECTIVO" -> "Efectivo";
            case "YAPE" -> "Yape";
            case "PLIN" -> "Plin";
            case "SIN_REGISTRO" -> "Sin registro";
            default -> metodo;
        };
    }

    private static String textoPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) return "Todas las ventas";
        if (desde != null && hasta != null) return desde.format(FECHA) + " al " + hasta.format(FECHA);
        if (desde != null) return "Desde el " + desde.format(FECHA);
        return "Hasta el " + hasta.format(FECHA);
    }

    /** Estilos reutilizados en todo el libro (cada estilo se crea una sola vez). */
    private static final class Estilos {
        final XSSFCellStyle titulo;
        final XSSFCellStyle encabezado;
        final XSSFCellStyle etiqueta;
        final XSSFCellStyle texto;
        final XSSFCellStyle entero;
        final XSSFCellStyle decimal;
        final XSSFCellStyle moneda;
        final XSSFCellStyle porcentaje;
        final XSSFCellStyle monedaTotal;
        final XSSFCellStyle totalEtiqueta;
        final XSSFCellStyle fecha;
        final XSSFCellStyle fechaHora;

        Estilos(XSSFWorkbook libro) {
            XSSFColor marca = new XSSFColor(COLOR_MARCA, null);

            XSSFFont fuenteTitulo = libro.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 16);
            fuenteTitulo.setColor(marca);

            XSSFFont fuenteEncabezado = libro.createFont();
            fuenteEncabezado.setBold(true);
            fuenteEncabezado.setColor(IndexedColors.WHITE.getIndex());

            XSSFFont negrita = libro.createFont();
            negrita.setBold(true);

            titulo = libro.createCellStyle();
            titulo.setFont(fuenteTitulo);

            encabezado = libro.createCellStyle();
            encabezado.setFont(fuenteEncabezado);
            encabezado.setFillForegroundColor(marca);
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            encabezado.setAlignment(HorizontalAlignment.CENTER);
            bordes(encabezado);

            etiqueta = libro.createCellStyle();
            etiqueta.setFont(negrita);
            bordes(etiqueta);

            texto = libro.createCellStyle();
            bordes(texto);

            entero = formato(libro, "#,##0");
            decimal = formato(libro, "0.0");
            moneda = formato(libro, FORMATO_MONEDA);
            porcentaje = formato(libro, "0%");
            fecha = formato(libro, "dd/mm/yyyy");
            fechaHora = formato(libro, "dd/mm/yyyy hh:mm");

            monedaTotal = formato(libro, FORMATO_MONEDA);
            monedaTotal.setFont(negrita);

            totalEtiqueta = libro.createCellStyle();
            totalEtiqueta.setFont(negrita);
            totalEtiqueta.setAlignment(HorizontalAlignment.RIGHT);
            bordes(totalEtiqueta);
        }

        private static XSSFCellStyle formato(XSSFWorkbook libro, String patron) {
            XSSFCellStyle estilo = libro.createCellStyle();
            estilo.setDataFormat(libro.createDataFormat().getFormat(patron));
            bordes(estilo);
            return estilo;
        }

        private static void bordes(XSSFCellStyle estilo) {
            estilo.setBorderTop(BorderStyle.THIN);
            estilo.setBorderBottom(BorderStyle.THIN);
            estilo.setBorderLeft(BorderStyle.THIN);
            estilo.setBorderRight(BorderStyle.THIN);
        }
    }
}
