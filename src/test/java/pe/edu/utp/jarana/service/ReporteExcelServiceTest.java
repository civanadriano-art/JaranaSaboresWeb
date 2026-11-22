package pe.edu.utp.jarana.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ReporteExcelServiceTest {

    @Test
    void generaLibroConHojasResumenYVentasYTotalCalculado() throws IOException {
        ReporteService reportes = mock(ReporteService.class);

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("totalVendido", new BigDecimal("45.50"));
        resumen.put("pedidosDespachados", 2);
        resumen.put("ticketPromedio", new BigDecimal("22.75"));
        resumen.put("platoMasVendido", "Lomo saltado");
        resumen.put("cantidadPlatoMasVendido", 3);
        resumen.put("tiempoPromedioCocina", 12.5);
        resumen.put("platosVendidos", List.of(Map.of("plato", "Lomo saltado", "cantidad", 3, "importe", new BigDecimal("45.50"))));
        resumen.put("ventasPorDia", List.of(Map.of("fecha", LocalDate.of(2026, 11, 2), "total", new BigDecimal("45.50"))));
        resumen.put("ventasPorMetodo", List.of(Map.of("metodo", "EFECTIVO", "total", new BigDecimal("45.50"))));

        Map<String, Object> venta1 = new LinkedHashMap<>();
        venta1.put("numero", 1);
        venta1.put("cliente", "Ana Pérez");
        venta1.put("fechaHoraDespacho", LocalDateTime.of(2026, 11, 2, 13, 30));
        venta1.put("minutosCocina", 10L);
        venta1.put("metodoPago", "EFECTIVO");
        venta1.put("total", new BigDecimal("25.50"));

        Map<String, Object> venta2 = new LinkedHashMap<>();
        venta2.put("numero", 2);
        venta2.put("cliente", "Luis Soto");
        venta2.put("fechaHoraDespacho", LocalDateTime.of(2026, 11, 2, 14, 5));
        venta2.put("minutosCocina", 15L);
        venta2.put("metodoPago", "EFECTIVO");
        venta2.put("total", new BigDecimal("20.00"));

        when(reportes.resumen(null, null, null)).thenReturn(resumen);
        when(reportes.ventas(null, null, null)).thenReturn(List.of(venta1, venta2));

        byte[] archivo = new ReporteExcelService(reportes).generar(null, null, null);

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            assertEquals(3, libro.getNumberOfSheets());

            Sheet hojaResumen = libro.getSheet("Resumen");
            assertNotNull(hojaResumen);
            assertEquals("Total vendido", hojaResumen.getRow(6).getCell(0).getStringCellValue());
            assertEquals(45.50, hojaResumen.getRow(6).getCell(1).getNumericCellValue(), 0.001);

            Sheet hojaVentas = libro.getSheet("Ventas");
            assertNotNull(hojaVentas);
            assertEquals("Pedido", hojaVentas.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Ana Pérez", hojaVentas.getRow(1).getCell(1).getStringCellValue());
            assertEquals(25.50, hojaVentas.getRow(1).getCell(5).getNumericCellValue(), 0.001);
            assertEquals("Efectivo", hojaVentas.getRow(1).getCell(4).getStringCellValue());

            // Fila de total: fórmula SUM con su resultado ya calculado
            assertEquals(CellType.FORMULA, hojaVentas.getRow(3).getCell(5).getCellType());
            assertEquals(45.50, hojaVentas.getRow(3).getCell(5).getNumericCellValue(), 0.001);

            Sheet hojaPlatos = libro.getSheet("Platos");
            assertNotNull(hojaPlatos);
            assertEquals("Lomo saltado", hojaPlatos.getRow(1).getCell(1).getStringCellValue());
            assertEquals(3, hojaPlatos.getRow(1).getCell(2).getNumericCellValue(), 0.001);
        }
    }

    @Test
    void sinVentasGeneraSoloEncabezadoSinFilaDeTotal() throws IOException {
        ReporteService reportes = mock(ReporteService.class);

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("totalVendido", BigDecimal.ZERO);
        resumen.put("pedidosDespachados", 0);
        resumen.put("ticketPromedio", BigDecimal.ZERO);
        resumen.put("platoMasVendido", "Sin ventas");
        resumen.put("cantidadPlatoMasVendido", 0);
        resumen.put("tiempoPromedioCocina", 0.0);
        resumen.put("platosVendidos", List.of());
        resumen.put("ventasPorDia", List.of());
        resumen.put("ventasPorMetodo", List.of());

        when(reportes.resumen(null, null, null)).thenReturn(resumen);
        when(reportes.ventas(null, null, null)).thenReturn(List.of());

        byte[] archivo = new ReporteExcelService(reportes).generar(null, null, null);

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            Sheet hojaVentas = libro.getSheet("Ventas");
            assertEquals(0, hojaVentas.getLastRowNum());
        }
    }
}
