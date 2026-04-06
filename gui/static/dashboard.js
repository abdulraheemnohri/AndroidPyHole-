const ctx = document.getElementById('queriesChart').getContext('2d');
const queriesChart = new Chart(ctx, {
    type: 'line',
    data: {
        labels: [],
        datasets: [{
            label: 'Total Queries',
            borderColor: '#78dc77',
            backgroundColor: 'rgba(120, 220, 119, 0.1)',
            data: [],
            fill: true,
            tension: 0.4
        }, {
            label: 'Blocked Queries',
            borderColor: '#ffb4a9',
            backgroundColor: 'rgba(255, 180, 169, 0.1)',
            data: [],
            fill: true,
            tension: 0.4
        }]
    },
    options: {
        responsive: true,
        plugins: {
            legend: {
                labels: {
                    color: '#e5e2e1'
                }
            }
        },
        scales: {
            x: {
                grid: {
                    color: '#353534'
                },
                ticks: {
                    color: '#becab9'
                }
            },
            y: {
                grid: {
                    color: '#353534'
                },
                ticks: {
                    color: '#becab9'
                }
            }
        }
    }
});

async function updateStats() {
    try {
        const response = await fetch('/api/stats');
        const data = await response.json();

        document.getElementById('total').textContent = data.total_queries;
        document.getElementById('blocked').textContent = data.blocked_queries;
        document.getElementById('block-rate').textContent =
            (data.total_queries > 0 ? (data.blocked_queries / data.total_queries * 100).toFixed(2) : '0.00') + '%';

        const list = document.getElementById('top-domains-list');
        list.innerHTML = '';
        if (data.top_domains && data.top_domains.length > 0) {
            data.top_domains.forEach(([domain, count]) => {
                const li = document.createElement('li');
                li.innerHTML = `<span>${domain}</span> <strong>${count}</strong>`;
                list.appendChild(li);
            });
        } else {
            const li = document.createElement('li');
            li.textContent = 'No blocked domains yet';
            list.appendChild(li);
        }

        // Update chart
        const now = new Date().toLocaleTimeString();
        if (queriesChart.data.labels.length > 20) {
            queriesChart.data.labels.shift();
            queriesChart.data.datasets[0].data.shift();
            queriesChart.data.datasets[1].data.shift();
        }
        queriesChart.data.labels.push(now);
        queriesChart.data.datasets[0].data.push(data.total_queries);
        queriesChart.data.datasets[1].data.push(data.blocked_queries);
        queriesChart.update();

    } catch (error) {
        console.error("Failed to update stats:", error);
    }
}

setInterval(updateStats, 5000);
updateStats();
