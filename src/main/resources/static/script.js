let loyaltyChart;

// Fetch top loyal customers and update chart
async function fetchTopLoyalCustomers() {
    const topX = document.getElementById("topX").value;
    const loyaltyCriteria = document.querySelector('input[name="loyaltyCriteria"]:checked').value;

    const response = await fetch(`/transactions/top-loyal-customers?topX=${topX}&loyaltyCriteria=${loyaltyCriteria}`);
    const data = await response.json();

    updateChart(data);
}

document.getElementById("topX").addEventListener("input", function() {
    document.getElementById("topXValue").innerText = this.value;
    fetchTopLoyalCustomers();
});

// Update chart with fetched data
function updateChart(data) {
    const labels = data.map(item => item.client_name);
    const values = data.map(item => item.loyalty_score);
    const tooltipsData = data.map(item => ({
        name: item.client_name,
        loyaltyScore: item.loyalty_score,
        purchaseFrequency: item.purchase_frequency,
        totalSpent: item.total_spent
    }));

    if (loyaltyChart) {
        loyaltyChart.destroy();
    }

    const ctx = document.getElementById('loyaltyChart').getContext('2d');
    loyaltyChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Loyalty Score',
                data: values,
                backgroundColor: 'rgba(75, 192, 192, 0.6)',
                borderColor: 'rgba(75, 192, 192, 1)',
                borderWidth: 1
            }]
        },
        options: {
            scales: {
                y: { beginAtZero: true }
            },
            plugins: {
                tooltip: {
                    callbacks: {
                        label: function(tooltipItem) {
                            const index = tooltipItem.dataIndex;
                            const tooltipData = tooltipsData[index];
                            return [
                                `Name: ${tooltipData.name}`,
                                `Loyalty Score: ${tooltipData.loyaltyScore}`,
                                `Times Purchased: ${tooltipData.purchaseFrequency}`,
                                `Total Spent: ${tooltipData.totalSpent} $`
                            ];
                        }
                    }
                }
            }
        }
    });
}

let currentPage = 1;
const rowsPerPage = 100;

// Fetch customers for current page
async function fetchAllCustomers(page = 1) {
    const offset = (page - 1) * rowsPerPage;

    const response = await fetch(`/transactions/all-customers?offset=${offset}&limit=${rowsPerPage}`);
    const customers = await response.json();

    updateTable(customers);
    updatePaginationButtons(customers.length); // Update pagination buttons
}

// Update customer table
function updateTable(customers) {
    const tableBody = document.querySelector('#customersTable tbody');
    tableBody.innerHTML = ''; // Clear table before repopulating

    customers.forEach(customer => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${customer.client_name}</td>
            <td>${customer.age_group}</td>
            <td>${customer.gender}</td>
            <td>${customer.purchase_frequency}</td>
            <td>${customer.total_spent.toFixed(2)}</td>
            <td>${customer.avg_payment_delay.toFixed(2)}</td>
        `;
        tableBody.appendChild(row);
    });
}

// Manage pagination buttons
function updatePaginationButtons(dataLength) {
    const nextPageButton = document.getElementById('nextPage');
    const prevPageButton = document.getElementById('prevPage');

    // Disable next button if no more pages
    nextPageButton.disabled = dataLength < rowsPerPage;

    // Disable previous button on first page
    prevPageButton.disabled = currentPage === 1;
}

// Add event listeners for pagination
document.getElementById('nextPage').addEventListener('click', () => {
    currentPage++;
    fetchAllCustomers(currentPage);
});

document.getElementById('prevPage').addEventListener('click', () => {
    if (currentPage > 1) {
        currentPage--;
        fetchAllCustomers(currentPage);
    }
});

window.onload = function() {
    fetchTopLoyalCustomers();
    fetchAllCustomers();
    document.getElementById("topXValue").innerText = document.getElementById("topX").value;
};
