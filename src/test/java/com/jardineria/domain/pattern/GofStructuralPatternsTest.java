package com.jardineria.domain.pattern;

import com.jardineria.domain.model.Pago;
import com.jardineria.domain.model.Producto;
import com.jardineria.domain.pattern.adapter.PayPalAdapter;
import com.jardineria.domain.pattern.adapter.StripeAdapter;
import com.jardineria.domain.pattern.bridge.*;
import com.jardineria.domain.pattern.composite.ProductBundleComposite;
import com.jardineria.domain.pattern.composite.ProductLeaf;
import com.jardineria.domain.pattern.decorator.*;
import com.jardineria.domain.pattern.flyweight.GamaFlyweight;
import com.jardineria.domain.pattern.flyweight.GamaFlyweightFactory;
import com.jardineria.domain.pattern.proxy.CachedProductLookupProxy;
import com.jardineria.domain.repository.ProductoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias completas para los 7 Patrones de Diseño Estructurales GoF.
 */
class GofStructuralPatternsTest {

    @Test
    @DisplayName("1. Decorator: Calcula capas de precio base, volumen, promo e IVA")
    void testDecoratorPriceCalculation() {
        PriceCalculator calculator = new BasePriceCalculator();
        calculator = new BulkDiscountDecorator(calculator);
        calculator = new SeasonalPromoDecorator(calculator);
        calculator = new VatTaxDecorator(calculator);

        // 10 unidades a 20.00 € = 200.00 € base
        // -10% volumen (>= 10 uds) = 180.00 €
        // -5% promo primavera = 171.00 €
        // +21% IVA = 206.91 €
        BigDecimal total = calculator.calcularTotal(new BigDecimal("20.00"), 10);
        assertEquals(new BigDecimal("206.91"), total);

        List<String> desglose = calculator.getDesglose();
        assertEquals(4, desglose.size());
        assertTrue(desglose.get(0).contains("Precio base"));
        assertTrue(desglose.get(1).contains("Descuento por Volumen"));
        assertTrue(desglose.get(2).contains("Promoción Primavera Verde"));
        assertTrue(desglose.get(3).contains("IVA Estándar"));
    }

    @Test
    @DisplayName("2. Composite: Maneja jerarquías arbóreas de productos y kits con precio y stock compuesto")
    void testCompositeProductBundle() {
        Producto p1 = new Producto("AR-001", "Albahaca", "Aromáticas", "10x10", "Vivero A", "Hierba", 25, new BigDecimal("3.50"), new BigDecimal("1.50"));
        Producto p2 = new Producto("HE-002", "Pala de Mano", "Herramientas", "30cm", "Tools Co", "Pala", 10, new BigDecimal("12.00"), new BigDecimal("6.00"));

        ProductLeaf leaf1 = new ProductLeaf(p1);
        ProductLeaf leaf2 = new ProductLeaf(p2);

        ProductBundleComposite bundle = new ProductBundleComposite("KIT-HUERTO", "Kit Huerto Urbano", new BigDecimal("0.10"));
        bundle.agregarComponente(leaf1);
        bundle.agregarComponente(leaf2);

        // Suma: 3.50 + 12.00 = 15.50 €. Con 10% descuento = 13.95 €
        assertEquals(new BigDecimal("13.95"), bundle.getPrecio());
        // Stock disponible: min(25, 10) = 10 kits
        assertEquals(10, bundle.getStockDisponible());

        StringBuilder sb = new StringBuilder();
        bundle.renderEstructura(sb, "");
        String rendered = sb.toString();
        assertTrue(rendered.contains("Kit Huerto Urbano"));
        assertTrue(rendered.contains("Albahaca"));
        assertTrue(rendered.contains("Pala de Mano"));
    }

    @Test
    @DisplayName("3. Adapter: Traduce SDKs de PayPal y Stripe a la entidad Pago unificada")
    void testPaymentAdapters() {
        PayPalAdapter payPalAdapter = new PayPalAdapter(null);
        Pago pagoPaypal = payPalAdapter.procesarCobro(101, "TX-9988", new BigDecimal("45.50"));

        assertEquals("PayPal", pagoPaypal.getFormaPago());
        assertEquals(101, pagoPaypal.getCodigoCliente());
        assertTrue(pagoPaypal.getIdTransaccion().startsWith("PAYPAL-TX-"));
        assertEquals(new BigDecimal("45.50"), pagoPaypal.getTotal());

        StripeAdapter stripeAdapter = new StripeAdapter(null);
        Pago pagoStripe = stripeAdapter.procesarCobro(102, "TX-7766", new BigDecimal("120.00"));

        assertEquals("Tarjeta", pagoStripe.getFormaPago());
        assertEquals(102, pagoStripe.getCodigoCliente());
        assertTrue(pagoStripe.getIdTransaccion().startsWith("ch_stripe_"));
        assertEquals(new BigDecimal("120.00"), pagoStripe.getTotal());
    }

    @Test
    @DisplayName("4. Proxy: Caché transparente en memoria con métricas de hits y misses")
    void testProductLookupProxy() {
        ProductoRepository mockRepo = mock(ProductoRepository.class);
        Producto prod = new Producto("FR-001", "Limonero", "Frutales", "1m", "Vivero C", "Árbol", 5, new BigDecimal("35.00"), new BigDecimal("20.00"));
        when(mockRepo.findById("FR-001")).thenReturn(Optional.of(prod));

        CachedProductLookupProxy proxy = new CachedProductLookupProxy(mockRepo);

        // Primera llamada: Cache Miss (debe consultar mockRepo)
        Optional<Producto> opt1 = proxy.buscarPorCodigo("FR-001");
        assertTrue(opt1.isPresent());
        assertEquals(1, proxy.getCacheMisses());
        assertEquals(0, proxy.getCacheHits());
        verify(mockRepo, times(1)).findById("FR-001");

        // Segunda llamada: Cache Hit (no debe consultar mockRepo de nuevo)
        Optional<Producto> opt2 = proxy.buscarPorCodigo("FR-001");
        assertTrue(opt2.isPresent());
        assertEquals(1, proxy.getCacheMisses());
        assertEquals(1, proxy.getCacheHits());
        assertEquals(50.0, proxy.getHitRatio());
        verify(mockRepo, times(1)).findById("FR-001");
    }

    @Test
    @DisplayName("5. Flyweight: Reutiliza instancias únicas compartidas para ahorrar memoria")
    void testFlyweightFactory() {
        GamaFlyweight g1 = GamaFlyweightFactory.getFlyweight("Aromáticas");
        GamaFlyweight g2 = GamaFlyweightFactory.getFlyweight("aromaticas");
        GamaFlyweight g3 = GamaFlyweightFactory.getFlyweight("Frutales");

        assertSame(g1, g2, "Las instancias para Aromáticas deben ser exactamente el mismo objeto en memoria");
        assertNotSame(g1, g3);
        assertEquals("🌿", g1.emojiVisual());
        assertEquals("🍎", g3.emojiVisual());
    }

    @Test
    @DisplayName("6. Bridge: Genera reportes desacoplando abstracción de implementadores CSV, HTML y JSON")
    void testBridgeReportOutputs() {
        List<String> columnas = List.of("Código", "Producto", "Stock");
        List<List<String>> datos = List.of(
                List.of("P-1", "Rosal", "15"),
                List.of("P-2", "Cactus", "40")
        );

        // CSV
        ReportBridge csvReport = new ExecutiveReport(new CsvOutputImplementor(), "Reporte Stock", "Subtítulo", columnas);
        String csvOut = csvReport.generarReporte(datos);
        assertTrue(csvOut.contains("Código,Producto,Stock"));
        assertTrue(csvOut.contains("P-1,Rosal,15"));

        // HTML
        ReportBridge htmlReport = new ExecutiveReport(new HtmlOutputImplementor(), "Reporte Stock", "Subtítulo", columnas);
        String htmlOut = htmlReport.generarReporte(datos);
        assertTrue(htmlOut.contains("<table>"));
        assertTrue(htmlOut.contains("<th>Producto</th>"));
        assertTrue(htmlOut.contains("<td>Rosal</td>"));

        // JSON
        ReportBridge jsonReport = new ExecutiveReport(new JsonOutputImplementor(), "Reporte Stock", "Subtítulo", columnas);
        String jsonOut = jsonReport.generarReporte(datos);
        assertTrue(jsonOut.contains("\"totalRegistros\": 2"));
        assertTrue(jsonOut.contains("\"Producto\": \"Rosal\""));
    }
}
