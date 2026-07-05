<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>TechMart - Order History</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        .order-card {
            transition: all 0.3s;
            cursor: pointer;
        }

        .order-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        }

        .status-badge {
            font-size: 0.9rem;
        }

        .track-btn {
            font-size: 0.85rem;
        }
    </style>
</head>
<body class="bg-light">
<nav class="navbar navbar-dark bg-primary">
    <div class="container">
        <a class="navbar-brand" href="products.jsp">TechMart</a>
        <div class="navbar-nav ms-auto">
            <a class="nav-link" href="cart.jsp">Cart</a>
            <a class="nav-link active" href="order-history.jsp">My Orders</a>
            <a class="nav-link" href="performance-dashboard.jsp">Performance</a>
        </div>
    </div>
</nav>

<div class="container mt-4">
    <h2 class="mb-4">My Orders</h2>

    <div id="orders-container">
        <div class="text-center py-5">
            <div class="spinner-border" role="status"></div>
            <p class="mt-2">Loading orders...</p>
        </div>
    </div>

    <div id="pagination" class="mt-4 text-center"></div>
</div>

<div class="modal fade" id="trackingModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title">Track Order</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div id="tracking-content">
                    <div class="text-center">
                        <div class="spinner-border text-primary" role="status"></div>
                        <p class="mt-2">Fetching tracking information...</p>
                        <small class="text-muted">This uses Future&lt;T&gt; with 3-second timeout</small>
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script>
    let currentPage = 0;

    async function loadOrders(page = 0) {
        currentPage = page;

        try {
            const response = await fetch('/techmart-web/api/orders?page=' + page);
            const data = await response.json();

            const container = document.getElementById('orders-container');

            if (data.orders.length === 0) {
                container.innerHTML = '<div class="alert alert-info">No orders found. <a href="products.jsp">Start shopping</a></div>';
                return;
            }

            container.innerHTML = data.orders.map(order => {
                const statusColor = getStatusColor(order.status);
                const canTrack = ['CONFIRMED', 'SHIPPED', 'DELIVERED'].includes(order.status);
                const orderNum = escapeHtml(order.orderNumber);
                const createdDate = formatDate(order.createdAt);
                const itemCount = order.itemCount;
                const totalAmount = order.totalAmount.toLocaleString();

                const trackBtn = canTrack
                    ? '<button class="btn btn-sm btn-primary track-btn" onclick="trackOrder(\'' + orderNum + '\')">Track</button>'
                    : '<span class="text-muted">Not shippable</span>';

                return '<div class="card order-card mb-3">' +
                    '<div class="card-body">' +
                    '<div class="row align-items-center">' +
                    '<div class="col-md-4">' +
                    '<h6 class="mb-1">Order #' + orderNum + '</h6>' +
                    '<small class="text-muted">' + createdDate + '</small>' +
                    '</div>' +
                    '<div class="col-md-2">' +
                    '<span class="badge bg-' + statusColor + ' status-badge">' + order.status + '</span>' +
                    '</div>' +
                    '<div class="col-md-2">' +
                    '<small>Items: ' + itemCount + '</small>' +
                    '</div>' +
                    '<div class="col-md-2">' +
                    '<strong>LKR ' + totalAmount + '</strong>' +
                    '</div>' +
                    '<div class="col-md-2 text-end">' +
                    trackBtn +
                    '</div>' +
                    '</div>' +
                    '</div>' +
                    '</div>';
            }).join('');

            const pagination = document.getElementById('pagination');
            const prevDisabled = (page === 0) ? 'disabled' : '';
            const nextDisabled = (!data.hasMore) ? 'disabled' : '';
            const prevPage = page - 1;
            const nextPage = page + 1;
            const currentPageNum = page + 1;

            pagination.innerHTML =
                '<button class="btn btn-outline-primary" onclick="loadOrders(' + prevPage + ')" ' + prevDisabled + '>' +
                '← Previous' +
                '</button>' +
                '<span class="mx-3">Page ' + currentPageNum + '</span>' +
                '<button class="btn btn-outline-primary" onclick="loadOrders(' + nextPage + ')" ' + nextDisabled + '>' +
                'Next →' +
                '</button>';

        } catch (err) {
            console.error('Failed to load orders:', err);
            document.getElementById('orders-container').innerHTML =
                '<div class="alert alert-danger">Failed to load orders</div>';
        }
    }

    async function trackOrder(orderNumber) {
        const modal = new bootstrap.Modal(document.getElementById('trackingModal'));
        modal.show();

        const content = document.getElementById('tracking-content');
        content.innerHTML = `
                <div class="text-center">
                    <div class="spinner-border text-primary" role="status"></div>
                    <p class="mt-2">Fetching tracking information...</p>
                    <small class="text-muted">Calling external courier API with Future&lt;T&gt; (3s timeout)</small>
                </div>
            `;

        try {
            const response = await fetch('/techmart-web/api/order-tracking?orderNumber=' + encodeURIComponent(orderNumber));

            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }

            const data = await response.json();

            const statusColor = data.status === 'IN_TRANSIT' ? 'info' :
                data.status === 'DELAYED' ? 'warning' :
                    data.status === 'DELIVERED' ? 'success' : 'secondary';

            content.innerHTML =
                '<div class="text-center">' +
                '<h5 class="mb-3">Order: ' + escapeHtml(data.orderNumber) + '</h5>' +
                '<div class="alert alert-' + statusColor + '">' +
                '<h6>Status: <strong>' + data.status + '</strong></h6>' +
                '<p class="mb-2"> Location: ' + escapeHtml(data.location) + '</p>' +
                '<p class="mb-0">Estimated Delivery: ' + escapeHtml(data.estimatedDelivery) + '</p>' +
                '</div>' +
                '<small class="text-muted">' +
                'Retrieved using Future&lt;T&gt; with timeout<br>' +
                'Graceful degradation if API fails' +
                '</small>' +
                '</div>';

        } catch (err) {
            console.error('Tracking failed:', err);
            content.innerHTML = `
                    <div class="alert alert-warning">
                        <h6>Tracking Unavailable</h6>
                        <p class="mb-2">Unable to retrieve tracking information at this time.</p>
                        <small>This demonstrates the timeout/fallback behavior of Future&lt;T&gt;</small>
                    </div>
                `;
        }
    }

    function getStatusColor(status) {
        const colors = {
            'PENDING': 'warning',
            'PROCESSING': 'info',
            'CONFIRMED': 'primary',
            'SHIPPED': 'info',
            'DELIVERED': 'success',
            'CANCELLED': 'danger'
        };
        return colors[status] || 'secondary';
    }

    function formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
    }

    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    loadOrders();
</script>
</body>
</html>