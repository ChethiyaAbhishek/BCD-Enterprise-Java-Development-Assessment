<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>TechMart - Checkout</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<nav class="navbar navbar-dark bg-primary">
    <div class="container">
        <a class="navbar-brand" href="products.jsp">🛒 TechMart</a>
    </div>
</nav>

<div class="container mt-4">
    <div class="row justify-content-center">
        <div class="col-md-6">
            <div class="card">
                <div class="card-header bg-primary text-white">
                    <h4 class="mb-0">Checkout</h4>
                </div>
                <div class="card-body">
                    <form id="checkout-form">
                        <div class="mb-3">
                            <label class="form-label">Customer Name</label>
                            <input type="text" class="form-control" value="John Doe" readonly>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Email</label>
                            <input type="email" class="form-control" value="john@techmart.com" readonly>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Shipping Address</label>
                            <textarea class="form-control" name="address" rows="3" required>15 Main Street, Colombo</textarea>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Payment Method</label>
                            <select class="form-select">
                                <option>Credit Card (Visa/Mastercard)</option>
                                <option>Bank Transfer</option>
                                <option>Cash on Delivery</option>
                            </select>
                        </div>
                        <button type="submit" class="btn btn-primary w-100">
                            Place Order
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    document.getElementById('checkout-form').addEventListener('submit', async (e) => {
        e.preventDefault();

        const button = e.target.querySelector('button');
        button.disabled = true;
        button.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Processing...';

        try {
            const formData = new URLSearchParams();
            formData.append('address', e.target.address.value);

            const response = await fetch('/techmart-web/api/checkout', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: formData
            });

            const result = await response.json();

            if (result.status === 'SUCCESS') {
                window.location.href = 'order-confirmation.jsp?orderId=' + result.orderId + '&orderNumber=' + result.orderNumber;
            } else {
                alert('Checkout failed: ' + result.message);
                button.disabled = false;
                button.textContent = 'Place Order';
            }
        } catch (err) {
            alert('Checkout failed: ' + err.message);
            button.disabled = false;
            button.textContent = 'Place Order';
        }
    });
</script>
</body>
</html>
