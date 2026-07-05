<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>TechMart - Order Status</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        .status-pulse { animation: pulse 2s infinite; }
        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.5; }
        }
    </style>
</head>
<body class="bg-light">
<nav class="navbar navbar-dark bg-primary">
    <div class="container">
        <a class="navbar-brand" href="products.jsp">🛒 TechMart</a>
    </div>
</nav>

<div class="container mt-5">
    <div class="row justify-content-center">
        <div class="col-md-8">
            <div class="card">
                <div class="card-body text-center">
                    <h2>Order Status</h2>
                    <p class="text-muted">Order Number: <strong id="order-number">Loading...</strong></p>

                    <div id="status-display" class="my-4">
                        <div class="spinner-border text-primary status-pulse" role="status"></div>
                        <p class="mt-2">Checking status...</p>
                    </div>

                    <div id="status-details" class="d-none">
                        <div class="alert alert-info">
                            <h5>Current Status: <span id="current-status" class="fw-bold"></span></h5>
                            <p class="mb-0">Payment: <span id="payment-status"></span></p>
                        </div>
                    </div>

                    <a href="products.jsp" class="btn btn-outline-primary">Continue Shopping</a>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    const urlParams = new URLSearchParams(window.location.search);
    const orderId = urlParams.get('orderId');
    const orderNumber = urlParams.get('orderNumber');

    console.log('Order ID:', orderId, 'Order Number:', orderNumber);

    document.getElementById('order-number').textContent = orderNumber || 'Unknown';

    async function checkStatus() {
        try {
            const response = await fetch('/techmart-web/api/order-status?orderId=' + orderId);

            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }

            const data = await response.json();

            const statusDisplay = document.getElementById('status-display');
            const statusDetails = document.getElementById('status-details');

            document.getElementById('current-status').textContent = data.status;
            document.getElementById('payment-status').textContent = data.paymentStatus;

            const statusColors = {
                'PENDING': 'warning',
                'PROCESSING': 'info',
                'CONFIRMED': 'primary',
                'SHIPPED': 'info',
                'DELIVERED': 'success',
                'CANCELLED': 'danger'
            };

            const color = statusColors[data.status] || 'secondary';
            document.getElementById('current-status').className = 'fw-bold text-' + color;

            if (['DELIVERED', 'CANCELLED'].includes(data.status)) {
                statusDisplay.innerHTML = '<div class="alert alert-' + color + '">' +
                    '<h4>Order ' + data.status + '</h4>' +
                    '</div>';
                statusDetails.classList.remove('d-none');
                return false;
            }

            statusDisplay.innerHTML = '<div class="spinner-border text-' + color + ' status-pulse" role="status"></div>' +
                '<p class="mt-2">Your order is being processed...</p>';
            statusDetails.classList.remove('d-none');
            return true;

        } catch (err) {
            console.error('Status check failed:', err);
            return true;
        }
    }

    const pollInterval = setInterval(async () => {
        const shouldContinue = await checkStatus();
        if (!shouldContinue) {
            clearInterval(pollInterval);
        }
    }, 3000);

    checkStatus();
</script>
</body>
</html>