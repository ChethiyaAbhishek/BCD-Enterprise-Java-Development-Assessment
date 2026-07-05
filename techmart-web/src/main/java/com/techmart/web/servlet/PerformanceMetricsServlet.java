package com.techmart.web.servlet;

import com.techmart.common.dto.PerformanceMetricDTO;
import com.techmart.ejb.session.singleton.MetricsCollectorBean;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/performance-metrics")
@Log
public class PerformanceMetricsServlet extends HttpServlet {

    @EJB
    private MetricsCollectorBean metricsCollector;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            List<PerformanceMetricDTO> metrics = metricsCollector.getAllMetrics();
            long uptime = metricsCollector.getUptimeSeconds();
            int monitoredCount = metricsCollector.getMonitoredMethodCount();

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"uptimeSeconds\": ").append(uptime).append(",\n");
            json.append("  \"monitoredMethods\": ").append(monitoredCount).append(",\n");
            json.append("  \"metrics\": [\n");

            for (int i = 0; i < metrics.size(); i++) {
                PerformanceMetricDTO metric = metrics.get(i);
                json.append("    {\n");
                json.append("      \"methodName\": \"").append(metric.methodName()).append("\",\n");
                json.append("      \"beanName\": \"").append(metric.beanName() != null ? metric.beanName() : "").append("\",\n");
                json.append("      \"invocationCount\": ").append(metric.invocationCount()).append(",\n");
                json.append("      \"avgResponseTimeMs\": ").append(String.format("%.2f", metric.avgResponseTimeMs())).append(",\n");
                json.append("      \"minResponseTimeMs\": ").append(metric.minResponseTimeMs()).append(",\n");
                json.append("      \"maxResponseTimeMs\": ").append(metric.maxResponseTimeMs()).append(",\n");
                json.append("      \"p95ResponseTimeMs\": ").append(String.format("%.2f", metric.p95ResponseTimeMs())).append(",\n");
                json.append("      \"p99ResponseTimeMs\": ").append(String.format("%.2f", metric.p99ResponseTimeMs())).append(",\n");
                json.append("      \"throughputPerSecond\": ").append(String.format("%.2f", metric.throughputPerSecond())).append(",\n");
                json.append("      \"successCount\": ").append(metric.successCount()).append(",\n");
                json.append("      \"errorCount\": ").append(metric.errorCount()).append(",\n");
                json.append("      \"errorRatePercent\": ").append(String.format("%.2f", metric.errorRatePercent())).append("\n");
                json.append("    }");

                if (i < metrics.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }

            json.append("  ]\n");
            json.append("}");

            resp.getWriter().write(json.toString());

        } catch (Exception e) {
            log.severe("Failed to get metrics: " + e.getMessage());
            resp.sendError(500, "Failed to retrieve metrics: " + e.getMessage());
        }
    }
}