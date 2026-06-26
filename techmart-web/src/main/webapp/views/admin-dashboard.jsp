<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>TechMart - Admin Dashboard</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<nav class="navbar navbar-dark bg-dark">
    <div class="container">
        <a class="navbar-brand" href="admin-dashboard.jsp">📊 TechMart Admin</a>
        <div class="navbar-nav ms-auto">
            <a class="nav-link" href="products.jsp">Store</a>
            <a class="nav-link" href="performance-dashboard.jsp">Performance</a>
        </div>
    </div>
</nav>

<div class="container mt-4">
    <h2 class="mb-4">Admin Dashboard</h2>

    <div class="row g-4 mb-4" id="kpi-cards">
        <div class="col-md-3">
            <div class="card text-white bg-primary">
                <div class="card-body">
                    <h6 class="card-title">Revenue (30 days)</h6>
                    <h3 id="revenue">Loading...</h3>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card text-white bg-warning">
                <div class="card-body">
                    <h6 class="card-title">Pending Orders</h6>
                    <h3 id="pending-orders">Loading...</h3>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card text-white bg-success">
                <div class="card-body">
                    <h6 class="card-title">Confirmed Orders</h6>
                    <h3 id="confirmed-orders">Loading...</h3>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card text-white bg-info">
                <div class="card-body">
                    <h6 class="card-title">Total Products</h6>
                    <h3 id="total-products">Loading...</h3>
                </div>
            </div>
        </div>
    </div>

    <div class="card">
        <div class="card-header">
            <h5 class="mb-0">Quick Actions</h5>
        </div>
        <div class="card-body">
            <div class="d-grid gap-2 d-md-flex">
                <a href="performance-dashboard.jsp" class="btn btn-outline-primary">📊 View Performance Metrics</a>
                <a href="products.jsp" class="btn btn-outline-secondary">🛍️ View Products</a>
                <button class="btn btn-outline-success" onclick="loadDashboard()">🔄 Refresh Dashboard</button>
            </div>
        </div>
    </div>
</div>

<script>
    async function loadDashboard() {
        try {
            const response = await fetch('/techmart-web/api/admin/dashboard');
            const data = await response.json();

            document.getElementById('revenue').textContent =
                'LKR ' + (data.revenue || 0).toLocaleString();
            document.getElementById('pending-orders').textContent =
                data.pendingOrders || 0;
            document.getElementById('confirmed-orders').textContent =
                data.confirmedOrders || 0;
            document.getElementById('total-products').textContent =
                data.totalProducts || 0;
        } catch (err) {
            console.error('Failed to load dashboard:', err);
        }
    }

    loadDashboard();
</script>
</body>
</html>