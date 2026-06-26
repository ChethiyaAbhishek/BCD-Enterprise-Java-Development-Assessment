<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>TechMart - Shopping Cart</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<nav class="navbar navbar-dark bg-primary">
    <div class="container">
        <a class="navbar-brand" href="products.jsp">🛒 TechMart</a>
    </div>
</nav>

<div class="container mt-4">
    <h2 class="mb-4">Shopping Cart</h2>

    <div id="cart-content">
        <div class="text-center py-5">
            <div class="spinner-border" role="status"></div>
            <p class="mt-2">Loading cart...</p>
        </div>
    </div>
</div>

<script>
    async function loadCart() {
        try {
            const response = await fetch('/techmart-web/api/cart');
            const data = await response.json();

            console.log('Cart data received:', data);

            const container = document.getElementById('cart-content');

            if (data.items === 0) {
                container.innerHTML = `
                        <div class="alert alert-info text-center">
                            Your cart is empty. <a href="products.jsp">Continue shopping</a>
                        </div>
                    `;
                return;
            }

            container.innerHTML = `
                    <div class="card">
                        <div class="card-body">
                            <p class="fs-4">Items in cart: <strong>\${data.items}</strong></p>
                            <p class="fs-4">Subtotal: <strong class="text-primary">LKR \${data.subtotal.toLocaleString()}</strong></p>
                            <hr>
                            <div class="d-flex gap-2">
                                <a href="products.jsp" class="btn btn-outline-secondary">Continue Shopping</a>
                                <a href="checkout.jsp" class="btn btn-primary">Proceed to Checkout</a>
                            </div>
                        </div>
                    </div>
                `;
        } catch (err) {
            console.error('Failed to load cart:', err);
            document.getElementById('cart-content').innerHTML =
                '<div class="alert alert-danger">Failed to load cart: ' + err.message + '</div>';
        }
    }

    loadCart();
</script>
</body>
</html>