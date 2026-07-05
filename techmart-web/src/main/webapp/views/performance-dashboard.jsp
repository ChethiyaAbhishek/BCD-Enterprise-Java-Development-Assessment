<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>TechMart - Performance Dashboard</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
    <style>
        .metric-card { transition: all 0.3s; }
        .metric-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
        .status-healthy { color: #198754; }
        .status-warning { color: #ffc107; }
        .status-critical { color: #dc3545; }
        .chart-container { position: relative; height: 300px; }
        .pulse { animation: pulse 2s infinite; }
        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.5; }
        }
    </style>
</head>
<body class="bg-light">
<nav class="navbar navbar-dark bg-dark">
    <div class="container-fluid">
        <a class="navbar-brand" href="performance-dashboard.jsp">Performance Dashboard</a>
        <div class="navbar-nav ms-auto flex-row align-items-center">
                <span class="nav-link text-light">
                    Uptime: <strong id="uptime">--</strong> |
                    Methods Monitored: <strong id="method-count">--</strong>
                </span>
            <button id="poll-toggle" class="btn btn-sm btn-outline-light ms-2">Pause</button>
        </div>
    </div>
</nav>

<div class="container-fluid mt-4">
    <div class="row g-3 mb-4">
        <div class="col-md-3">
            <div class="card metric-card border-success">
                <div class="card-body text-center">
                    <h6 class="text-muted">Total Invocations</h6>
                    <h2 id="total-invocations" class="text-success">0</h2>
                    <small class="text-muted">Across all methods</small>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card metric-card border-primary">
                <div class="card-body text-center">
                    <h6 class="text-muted">Avg Response Time</h6>
                    <h2 id="avg-response-time" class="text-primary">0 ms</h2>
                    <small class="text-muted">Weighted average</small>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card metric-card border-info">
                <div class="card-body text-center">
                    <h6 class="text-muted">Throughput</h6>
                    <h2 id="throughput" class="text-info">0 req/s</h2>
                    <small class="text-muted">Requests per second</small>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card metric-card border-danger">
                <div class="card-body text-center">
                    <h6 class="text-muted">Error Rate</h6>
                    <h2 id="error-rate" class="text-danger">0%</h2>
                    <small class="text-muted">Failed requests</small>
                </div>
            </div>
        </div>
    </div>

    <div class="row g-4 mb-4">
        <div class="col-md-6">
            <div class="card">
                <div class="card-header">
                    <h5 class="mb-0">Response Time Trend (Last 20 polls)</h5>
                </div>
                <div class="card-body">
                    <div class="chart-container">
                        <canvas id="responseTimeChart"></canvas>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-md-6">
            <div class="card">
                <div class="card-header">
                    <h5 class="mb-0">Invocation Count by Method</h5>
                </div>
                <div class="card-body">
                    <div class="chart-container">
                        <canvas id="invocationChart"></canvas>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
            <h5 class="mb-0">Detailed Method Metrics</h5>
            <span class="badge bg-secondary" id="last-update">Last updated: --</span>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover mb-0">
                    <thead class="table-dark">
                    <tr>
                        <th>Method Name</th>
                        <th class="text-end">Invocations</th>
                        <th class="text-end">Avg (ms)</th>
                        <th class="text-end">Min (ms)</th>
                        <th class="text-end">Max (ms)</th>
                        <th class="text-end">P95 (ms)</th>
                        <th class="text-end">P99 (ms)</th>
                        <th class="text-end">Throughput</th>
                        <th class="text-end">Errors</th>
                        <th class="text-end">Error Rate</th>
                        <th>Health</th>
                    </tr>
                    </thead>
                    <tbody id="metrics-table-body">
                    <tr><td colspan="11" class="text-center py-4">Loading metrics...</td></tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/charts.js"></script>
<script src="${pageContext.request.contextPath}/js/metrics-poller.js"></script>
</body>
</html>