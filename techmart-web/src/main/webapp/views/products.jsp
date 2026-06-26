<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>TechMart - Products</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        .product-card { transition: transform 0.2s; cursor: pointer; }
        .product-card:hover { transform: translateY(-5px); box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
        .stock-badge { position: absolute; top: 10px; right: 10px; }
    </style>
</head>
<body class="bg-light">
<nav class="navbar navbar-expand-lg navbar-dark bg-primary">
    <div class="navbar-nav ms-auto">
        <a class="nav-link" href="cart.jsp">Cart <span id="cart-count" class="badge bg-danger">0</span></a>
        <a class="nav-link" href="order-history.jsp">📦 My Orders</a>
        <a class="nav-link" href="admin-dashboard.jsp">Admin</a>
        <a class="nav-link" href="performance-dashboard.jsp">📊 Performance</a>
    </div>
</nav>

<div class="container mt-4">
    <h2 class="mb-4">Product Catalog</h2>

    <div class="mb-3">
        <select id="category-filter" class="form-select w-auto d-inline-block">
            <option value="">All Categories</option>
            <option value="Electronics">Electronics</option>
            <option value="Clothing">Clothing</option>
            <option value="Footwear">Footwear</option>
            <option value="Home">Home</option>
        </select>
    </div>

    <div id="products-container" class="row row-cols-1 row-cols-md-3 row-cols-lg-4 g-4">
    </div>
</div>

<script>
    // Load products via AJAX
    async function loadProducts(category = '') {
        // ✅ FIX 1: Escape the $ in the URL template literal
        const url = category
            ? `/techmart-web/api/products?category=\${encodeURIComponent(category)}`
            : '/techmart-web/api/products';

        try {
            const response = await fetch(url);
            const products = await response.json();

            const container = document.getElementById('products-container');
            container.innerHTML = products.map(p => `
                    <div class="col">
                        <div class="card product-card h-100 position-relative">
                            <span class="badge \${p.totalStock > 0 ? 'bg-success' : 'bg-danger'} stock-badge">
                                \${p.totalStock > 0 ? 'In Stock' : 'Out of Stock'}
                            </span>
                            <img src="\${p.imageUrl || 'https://via.placeholder.com/300x200?text=Product'}"
                                 class="card-img-top" alt="\${p.name}" style="height: 200px; object-fit: cover;">
                            <div class="card-body">
                                <h6 class="card-title">\${p.name}</h6>
                                <p class="card-text text-muted small">\${p.categoryName}</p>
                                <p class="card-text fw-bold text-primary">LKR \${p.sellingPrice.toLocaleString()}</p>
                                <button class="btn btn-primary btn-sm w-100"
                                        onclick="addToCart(\${p.id}, '\${p.name}', \${p.sellingPrice})" \${p.totalStock <= 0 ? 'disabled' : ''}>
                                    Add to Cart
                                </button>
                            </div>
                        </div>
                    </div>
                `).join('');
        } catch (err) {
            console.error('Failed to load products:', err);
        }
    }

    async function addToCart(productId, name, price) {
        const formData = new URLSearchParams();
        formData.append('productId', productId);
        formData.append('quantity', 1);
        formData.append('price', price);

        try {
            const response = await fetch('/techmart-web/api/cart', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: formData
            });
            const result = await response.json();
            document.getElementById('cart-count').textContent = result.totalItems;
            alert(`\${name} added to cart!`);
        } catch (err) {
            alert('Failed to add to cart');
        }
    }

    document.getElementById('category-filter').addEventListener('change', (e) => {
        loadProducts(e.target.value);
    });

    loadProducts();
</script>
</body>
</html>