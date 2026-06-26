const ChartsManager = (() => {
    let responseTimeChart = null;
    let invocationChart = null;

    const MAX_DATA_POINTS = 20;
    const trendLabels = [];
    const trendData = [];

    function init() {
        initResponseTimeChart();
        initInvocationChart();
    }

    function initResponseTimeChart() {
        const ctx = document.getElementById('responseTimeChart').getContext('2d');

        responseTimeChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: trendLabels,
                datasets: [
                    {
                        label: 'Avg Response Time (ms)',
                        data: trendData,
                        borderColor: 'rgb(54, 162, 235)',
                        backgroundColor: 'rgba(54, 162, 235, 0.1)',
                        tension: 0.3,
                        fill: true,
                        pointRadius: 3
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: { duration: 500 },
                scales: {
                    y: {
                        beginAtZero: true,
                        title: { display: true, text: 'Response Time (ms)' }
                    },
                    x: {
                        title: { display: true, text: 'Poll Number' }
                    }
                },
                plugins: {
                    legend: { display: true, position: 'top' }
                }
            }
        });
    }

    function initInvocationChart() {
        const ctx = document.getElementById('invocationChart').getContext('2d');

        invocationChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: [],
                datasets: [
                    {
                        label: 'Invocations',
                        data: [],
                        backgroundColor: 'rgba(75, 192, 192, 0.7)',
                        borderColor: 'rgba(75, 192, 192, 1)',
                        borderWidth: 1
                    },
                    {
                        label: 'Errors',
                        data: [],
                        backgroundColor: 'rgba(255, 99, 132, 0.7)',
                        borderColor: 'rgba(255, 99, 132, 1)',
                        borderWidth: 1
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: { duration: 500 },
                scales: {
                    y: {
                        beginAtZero: true,
                        title: { display: true, text: 'Count' }
                    },
                    x: {
                        ticks: { maxRotation: 45, minRotation: 45 }
                    }
                },
                plugins: {
                    legend: { display: true, position: 'top' }
                }
            }
        });
    }

    function updateResponseTimeTrend(avgResponseTime) {
        const now = new Date();
        const label = now.toLocaleTimeString();

        trendLabels.push(label);
        trendData.push(avgResponseTime);

        if (trendLabels.length > MAX_DATA_POINTS) {
            trendLabels.shift();
            trendData.shift();
        }

        responseTimeChart.update('none');
    }

    function updateInvocationChart(metrics) {
        const sorted = [...metrics].sort((a, b) => b.invocationCount - a.invocationCount);
        const top = sorted.slice(0, 10);

        invocationChart.data.labels = top.map(m => {
            const parts = m.methodName.split('.');
            return parts.length > 1 ? parts[parts.length - 1] : m.methodName;
        });
        invocationChart.data.datasets[0].data = top.map(m => m.invocationCount);
        invocationChart.data.datasets[1].data = top.map(m => m.errorCount);

        invocationChart.update();
    }

    function destroy() {
        if (responseTimeChart) responseTimeChart.destroy();
        if (invocationChart) invocationChart.destroy();
    }

    return { init, updateResponseTimeTrend, updateInvocationChart, destroy };
})();