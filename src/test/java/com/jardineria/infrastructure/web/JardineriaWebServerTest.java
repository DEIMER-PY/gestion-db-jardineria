package com.jardineria.infrastructure.web;

import com.jardineria.infrastructure.config.ApplicationContext;
import com.jardineria.infrastructure.adapter.in.web.JardineriaWebServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class JardineriaWebServerTest {

    @Test
    @DisplayName("Servidor Web Embebido: Responde a endpoints REST y sirve index.html")
    void testWebServerEndpoints() throws Exception {
        ApplicationContext registry = ApplicationContext.getInstance();
        JardineriaWebServer server = com.jardineria.infrastructure.adapter.in.web.JardineriaWebServer.shared(com.jardineria.infrastructure.config.ApplicationContext.getInstance());
        assertNotNull(server);
        assertTrue(server.isRunning());

        HttpClient client = HttpClient.newHttpClient();
        String baseUrl = "http://localhost:" + server.getPort();

        // 1. GET /dashboard
        HttpRequest reqDashboard = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/dashboard")).GET().build();
        HttpResponse<String> resDashboard = client.send(reqDashboard, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resDashboard.statusCode());
        assertTrue(resDashboard.body().contains("Jardinería Enterprise Suite"));

        // 2. GET /api/metrics
        HttpRequest reqMetrics = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/metrics")).GET().build();
        HttpResponse<String> resMetrics = client.send(reqMetrics, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resMetrics.statusCode());
        assertTrue(resMetrics.body().contains("\"totalClientes\""));
        assertTrue(resMetrics.body().contains("\"totalPedidos\""));

        // 3. GET /api/patterns
        HttpRequest reqPatterns = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/patterns")).GET().build();
        HttpResponse<String> resPatterns = client.send(reqPatterns, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resPatterns.statusCode());
        assertTrue(resPatterns.body().contains("\"Facade\""));
        assertTrue(resPatterns.body().contains("\"Decorator\""));
        assertTrue(resPatterns.body().contains("\"Proxy\""));
    }
}
