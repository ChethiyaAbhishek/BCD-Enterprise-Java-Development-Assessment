package com.techmart.web.servlet;

import com.techmart.common.dto.PerformanceMetricDTO;
import com.techmart.ejb.session.singleton.MetricsCollectorBean;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformanceMetricsServletTest {

    @InjectMocks
    private PerformanceMetricsServlet servlet;

    @Mock
    private MetricsCollectorBean metricsCollector;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private StringWriter responseBody;
    private PrintWriter writer;

    @BeforeEach
    void setUp() throws Exception {
        responseBody = new StringWriter();
        writer = new PrintWriter(responseBody);

        lenient().when(response.getWriter()).thenReturn(writer);
    }

    @Test
    void testGetMetricsReturnsJson() throws Exception {
        List<PerformanceMetricDTO> metrics = List.of(
                new PerformanceMetricDTO(
                        "OrderProcessingBean.processCheckout",
                        "OrderProcessingBean",
                        100,
                        45.5,
                        12.0,
                        120.0,
                        95.0,
                        110.0,
                        50.0,
                        98,
                        2,
                        2.0,
                        LocalDateTime.now()
                )
        );

        when(metricsCollector.getAllMetrics()).thenReturn(metrics);
        when(metricsCollector.getUptimeSeconds()).thenReturn(3600L);
        when(metricsCollector.getMonitoredMethodCount()).thenReturn(5);

        servlet.doGet(request, response);

        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");

        writer.flush();
        String json = responseBody.toString();

        assertNotNull(json);
        assertFalse(json.isEmpty());
        assertTrue(json.contains("uptimeSeconds"));
        assertTrue(json.contains("monitoredMethods"));
        assertTrue(json.contains("metrics"));
        assertTrue(json.contains("OrderProcessingBean.processCheckout"));
    }

    @Test
    void testGetMetricsHandlesEmptyMetrics() throws Exception {
        when(metricsCollector.getAllMetrics()).thenReturn(List.of());
        when(metricsCollector.getUptimeSeconds()).thenReturn(0L);
        when(metricsCollector.getMonitoredMethodCount()).thenReturn(0);

        servlet.doGet(request, response);

        verify(response).setContentType("application/json");

        writer.flush();
        String json = responseBody.toString();

        assertNotNull(json);
        assertTrue(json.contains("\"metrics\": ["));
        assertTrue(json.contains("\"uptimeSeconds\": 0"));
    }

    @Test
    void testGetMetricsHandlesException() throws Exception {
        when(metricsCollector.getAllMetrics()).thenThrow(new RuntimeException("Test error"));

        servlet.doGet(request, response);

        verify(response).sendError(eq(500), anyString());
    }
}