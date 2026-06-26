const MetricsPoller = (() => {
    const API_URL = '/techmart-web/api/performance-metrics';
    const POLL_INTERVAL_MS = 2000;

    let pollTimer = null;
    let isPolling = false;
    let pollCount = 0;

    function start() {
        if (isPolling) return;

        isPolling = true;
        ChartsManager.init();
        fetchAndUpdate();

        pollTimer = setInterval(fetchAndUpdate, POLL_INTERVAL_MS);

        console.log('Metrics polling started (interval: ' + POLL_INTERVAL_MS + 'ms)');
    }

    function stop() {
        if (!isPolling) return;

        isPolling = false;
        if (pollTimer) {
            clearInterval(pollTimer);
            pollTimer = null;
        }

        console.log('Metrics polling stopped');
    }

    function toggle() {
        if (isPolling) {
            stop();
        } else {
            start();
        }
        return isPolling;
    }

    async function fetchAndUpdate() {
        try {
            const response = await fetch(API_URL);

            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }

            const data = await response.json();
            updateDashboard(data);

            pollCount++;

        } catch (err) {
            console.error('Failed to fetch metrics:', err);
            showError('Failed to fetch metrics: ' + err.message);
        }
    }

    function updateDashboard(data) {
        document.getElementById('uptime').textContent = formatUptime(data.uptimeSeconds);
        document.getElementById('method-count').textContent = data.monitoredMethods;

        const totals = calculateTotals(data.metrics);

        document.getElementById('total-invocations').textContent =
            totals.totalInvocations.toLocaleString();
        document.getElementById('avg-response-time').textContent =
            totals.avgResponseTime.toFixed(2) + ' ms';
        document.getElementById('throughput').textContent =
            totals.totalThroughput.toFixed(2) + ' req/s';
        document.getElementById('error-rate').textContent =
            totals.errorRate.toFixed(2) + '%';

        ChartsManager.updateResponseTimeTrend(totals.avgResponseTime);
        ChartsManager.updateInvocationChart(data.metrics);

        updateMetricsTable(data.metrics);

        document.getElementById('last-update').textContent =
            'Last updated: ' + new Date().toLocaleTimeString();
    }

    function calculateTotals(metrics) {
        if (!metrics || metrics.length === 0) {
            return {
                totalInvocations: 0,
                avgResponseTime: 0,
                totalThroughput: 0,
                errorRate: 0
            };
        }

        const totalInvocations = metrics.reduce((sum, m) => sum + m.invocationCount, 0);
        const totalErrors = metrics.reduce((sum, m) => sum + m.errorCount, 0);
        const totalThroughput = metrics.reduce((sum, m) => sum + m.throughputPerSecond, 0);

        const weightedSum = metrics.reduce((sum, m) =>
            sum + (m.avgResponseTimeMs * m.invocationCount), 0);
        const avgResponseTime = totalInvocations > 0
            ? weightedSum / totalInvocations
            : 0;

        const errorRate = totalInvocations > 0
            ? (totalErrors / totalInvocations) * 100
            : 0;

        return {
            totalInvocations,
            avgResponseTime,
            totalThroughput,
            errorRate
        };
    }

    function updateMetricsTable(metrics) {
        const tbody = document.getElementById('metrics-table-body');

        if (!metrics || metrics.length === 0) {
            tbody.innerHTML = '<tr><td colspan="11" class="text-center py-4 text-muted">No metrics recorded yet</td></tr>';
            return;
        }

        tbody.innerHTML = metrics.map(m => {
            const health = determineHealth(m);
            const healthClass = health === 'HEALTHY' ? 'success' : health === 'WARNING' ? 'warning' : 'danger';

            return `
                <tr>
                    <td><code>${escapeHtml(m.methodName)}</code></td>
                    <td class="text-end">${m.invocationCount.toLocaleString()}</td>
                    <td class="text-end">${m.avgResponseTimeMs.toFixed(2)}</td>
                    <td class="text-end">${m.minResponseTimeMs}</td>
                    <td class="text-end">${m.maxResponseTimeMs}</td>
                    <td class="text-end">${m.p95ResponseTimeMs.toFixed(2)}</td>
                    <td class="text-end">${m.p99ResponseTimeMs.toFixed(2)}</td>
                    <td class="text-end">${m.throughputPerSecond.toFixed(2)}</td>
                    <td class="text-end">${m.errorCount}</td>
                    <td class="text-end">${m.errorRatePercent.toFixed(2)}%</td>
                    <td><span class="badge bg-${healthClass}">${health}</span></td>
                </tr>
            `;
        }).join('');
    }

    function determineHealth(metric) {
        if (metric.errorRatePercent > 5 || metric.avgResponseTimeMs > 1000) {
            return 'CRITICAL';
        } else if (metric.errorRatePercent > 1 || metric.avgResponseTimeMs > 500) {
            return 'WARNING';
        }
        return 'HEALTHY';
    }

    function formatUptime(seconds) {
        const hours = Math.floor(seconds / 3600);
        const minutes = Math.floor((seconds % 3600) / 60);
        const secs = seconds % 60;

        if (hours > 0) {
            return `${hours}h ${minutes}m`;
        } else if (minutes > 0) {
            return `${minutes}m ${secs}s`;
        }
        return `${secs}s`;
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function showError(message) {
        const lastUpdate = document.getElementById('last-update');
        lastUpdate.textContent = '⚠️ ' + message;
        lastUpdate.className = 'badge bg-danger';
    }

    document.addEventListener('DOMContentLoaded', () => {
        const toggleBtn = document.getElementById('poll-toggle');
        if (toggleBtn) {
            toggleBtn.addEventListener('click', () => {
                const polling = MetricsPoller.toggle();
                toggleBtn.textContent = polling ? '⏸ Pause' : '▶ Resume';
                toggleBtn.className = polling
                    ? 'btn btn-sm btn-outline-light ms-2'
                    : 'btn btn-sm btn-outline-success ms-2';
            });
        }

        MetricsPoller.start();
    });

    return { start, stop, toggle };
})();