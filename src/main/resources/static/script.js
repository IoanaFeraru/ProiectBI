// Graficul de loialitate

let loyaltyChart;
/**
 * Obține cei mai loiali clienți pe baza unui criteriu de loialitate dat și actualizează graficul de loialitate cu datele.
 * Recuperează criteriul de loialitate selectat de utilizator și cei mai buni X clienți care trebuie afișați.
 */
async function fetchTopLoyalCustomers() {
    const topX = document.getElementById("topX").value;
    const loyaltyCriteria = document.querySelector('input[name="loyaltyCriteria"]:checked').value;

    try {
        const response = await fetch(`/transactions/top-loyal-customers?topX=${topX}&loyaltyCriteria=${loyaltyCriteria}`);
        if (!response.ok) throw new Error('Network response was not ok');
        const data = await response.json();
        updateChart(data);
    } catch (error) {
        console.error('Failed to fetch top loyal customers:', error);
    }
}

/**
 * Actualizează graficul de loialitate cu datele obținute. Dacă există deja o instanță de grafic, aceasta este distrusă pentru a permite re-redarea.
 * Această funcție creează un grafic cu bare cu tooltips care afișează informații detaliate despre scorurile de loialitate, frecvența achizițiilor și totalul cheltuit.
 */
function updateChart(data) {
    if (data.length === 0) {
        console.error('No data available for the chart');
        return;
    }

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

// Tabelul cu toți clienții

/**
 * Obține toate datele clienților de la server și le afișează într-un tabel.
 * Afișează un indicator de încărcare în timpul obținerii datelor și actualizează tabelul folosind datele clienților obținute.
 */
async function fetchAllCustomers() {
    document.getElementById('loading').style.display = 'block';
    try {
        const response = await fetch(`/transactions/all-customers`);
        if (!response.ok) throw new Error('Network response was not ok');
        const customers = await response.json();
        updateTable(customers);
    } catch (error) {
        console.error('Failed to fetch customers:', error);
    } finally {
        document.getElementById('loading').style.display = 'none';
    }
}

/**
 * Actualizează tabelul de clienți cu datele obținute și aplică color coding pentru întârzierile medii la plată.
 * Rândurile cu întârzieri care depășesc un prag definit de utilizator sunt evidențiate pentru o identificare ușoară.
 */
function updateTable(customers) {
    const tableBody = document.querySelector('#customersTable tbody');
    tableBody.innerHTML = '';

    const delayThreshold = parseInt(document.getElementById('delayThreshold').value, 10) || 0;

    customers.forEach(customer => {
        const row = document.createElement('tr');

        const avgPaymentDelay = customer.avg_payment_delay;
        row.style.backgroundColor = avgPaymentDelay > delayThreshold ? 'red' : ''; // Highlight bad delays

        row.innerHTML = `
            <td>${customer.client_name}</td>
            <td>${customer.age_group}</td>
            <td>${customer.gender}</td>
            <td>${customer.purchase_frequency}</td>
            <td>${customer.total_spent.toFixed(2)}</td>
            <td>${avgPaymentDelay.toFixed(2)}</td>
        `;
        tableBody.appendChild(row);
    });
}

/** Atașează un listener de evenimente pentru inputul pragului de întârziere
 */
document.getElementById('delayThreshold').addEventListener('input', fetchAllCustomers);

/**
 * Filtrează tabelul de clienți pe baza criteriilor de vârstă și gen selectate de utilizator.
 * Afișează rândurile care se potrivesc cu opțiunile de filtrare selectate.
 */
function filterTable() {
    const ageGroupFilter = document.getElementById('ageGroupFilter').value;
    const genderFilter = document.getElementById('genderFilter').value;
    const rows = document.querySelectorAll('#customersTable tbody tr');

    rows.forEach(row => {
        const ageGroupCell = row.children[1].textContent; // Assuming age group is in the second cell
        const genderCell = row.children[2].textContent; // Assuming gender is in the third cell

        // Check if the row matches the selected filters
        const ageGroupMatch = ageGroupFilter === '' || ageGroupCell === ageGroupFilter;
        const genderMatch = genderFilter === '' || genderCell === genderFilter;

        // Display the row if it matches both filters
        if (ageGroupMatch && genderMatch) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
}

/**
 * Sortează tabelul de clienți pe baza indexului coloanei specificate. Suportă sortarea pentru coloane de text și numerice.
 * Comută între ordinea de sortare ascendentă și descendentă.
 */
let sortDirection = Array.from({ length: 6 }, () => true);
function sortTable(columnIndex) {
    const table = document.getElementById("customersTable");
    const tbody = table.getElementsByTagName("tbody")[0];
    const rows = Array.from(tbody.getElementsByTagName("tr"));

    sortDirection[columnIndex] = !sortDirection[columnIndex];

    rows.sort((a, b) => {
        const aText = a.children[columnIndex].textContent;
        const bText = b.children[columnIndex].textContent;

        const isNumericColumn = columnIndex === 4 || columnIndex === 5;

        // Convertește în numere dacă este numeric
        const aValue = isNumericColumn ? parseFloat(aText) : aText;
        const bValue = isNumericColumn ? parseFloat(bText) : bText;

        return sortDirection[columnIndex]
            ? (aValue > bValue ? 1 : -1) // Asc
            : (aValue < bValue ? 1 : -1); // Desc
    });

    tbody.innerHTML = '';
    rows.forEach(row => tbody.appendChild(row));

    // ToDO - Fix on update arrows, aren't shown in column 4 and 5
    const arrows = document.querySelectorAll('.sort-arrow');
    arrows.forEach((arrow, index) => {
        arrow.classList.remove('asc', 'desc');
        if (index === columnIndex) {
            arrow.classList.add(sortDirection[columnIndex] ? 'asc' : 'desc');
        }
    });
}

// Graficele corelațiilor
/**
 * Obține datele JSON de la un URL specificat.
 * @param {string} url - URL-ul de la care sunt obținute datele.
 * @returns {Promise<Object>} - O promisiune care se rezolvă în datele JSON.
 * @throws {Error} - Aruncă o eroare dacă răspunsul rețelei nu este reușit.
 */
async function fetchData(url) {
    const response = await fetch(url);
    if (!response.ok) {
        throw new Error('Network response was not ok ' + response.statusText);
    }
    return response.json();
}

/**
 * Afișează graficele de corelație bazat pe tipul de corelație (gen, grup de varsta, tip de discount).
 */

async function renderDiscountsByGenderChart() {
    const genderData = await fetchData('/transactions/correlate-discounts/gender');
    const labels = genderData.map(gender => gender.gender);
    const revenueData = genderData.map(gender => gender.total_revenue);

    const ctx = document.getElementById('discountsByGenderChart').getContext('2d');
    new Chart(ctx, {
        type: 'polarArea',
        data: {
            labels: labels,
            datasets: [{
                label: 'Total Revenue by Gender',
                data: revenueData,
                backgroundColor: ['rgba(255, 99, 132, 0.6)', 'rgba(54, 162, 235, 0.6)'],
                borderColor: ['rgba(255, 99, 132, 1)', 'rgba(54, 162, 235, 1)'],
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            scales: {
                r: {
                    beginAtZero: true
                }
            }
        }
    });
}

async function renderDiscountsByAgeGroupChart() {
    const ageGroupData = await fetchData('/transactions/correlate-discounts/age-group');
    const labels = ageGroupData.map(group => group.age_group);
    const revenueData = ageGroupData.map(group => group.total_revenue);

    const ctx = document.getElementById('discountsByAgeGroupChart').getContext('2d');
    new Chart(ctx, {
        type: 'polarArea',
        data: {
            labels: labels,
            datasets: [{
                label: 'Total Revenue by Age Group',
                data: revenueData,
                backgroundColor: 'rgba(255, 206, 86, 0.6)',
                borderColor: 'rgba(255, 206, 86, 1)',
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            scales: {
                r: {
                    beginAtZero: true
                }
            }
        }
    });
}

async function renderDiscountsByTypeChart() {
    const discountsData = await fetchData('/transactions/correlate-discounts/sales');
    const labels = discountsData.map(discount => discount.discount_name);
    const revenueData = discountsData.map(discount => discount.total_revenue);

    const ctx = document.getElementById('discountsByTypeChart').getContext('2d');
    new Chart(ctx, {
        type: 'polarArea',
        data: {
            labels: labels,
            datasets: [{
                label: 'Total Revenue by Discount Type',
                data: revenueData,
                backgroundColor: 'rgba(153, 102, 255, 0.6)',
                borderColor: 'rgba(153, 102, 255, 1)',
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            scales: {
                r: {
                    beginAtZero: true
                }
            }
        }
    });
}

//Estimare flux de bani

/**
 * Obține datele estimate pentru fluxul de numerar viitor pe baza numărului de luni specificat.
 * @param {number} monthsAhead - Numărul de luni în avans pentru estimarea fluxului de numerar.
 * @returns {Promise<Array>} - Un array cu valorile estimate ale fluxului de numerar.
 */
let cashFlowChart;
async function estimateFutureCashFlow(monthsAhead) {
    try {
        const response = await fetch(`/transactions/estimate-cash-flow?monthsAhead=${monthsAhead}`);
        if (!response.ok) {
            throw new Error(`Network response was not ok: ${response.statusText}`);
        }
        const estimatedData = await response.json();
        if (!Array.isArray(estimatedData)) {
            console.error('Estimated data is not an array:', estimatedData);
            return [];
        }
        return estimatedData;
    } catch (error) {
        console.error('Error fetching estimated cash flow:', error);
        return [];
    }
}

/**
 * Obține datele istorice ale fluxului de numerar.
 * @returns {Promise<Array>} - Un array cu valorile istorice ale fluxului de numerar.
 */
async function fetchHistoricalCashFlow() {
    try {
        const response = await fetch('/transactions/historicalCashFlow');
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        const data = await response.json();
        return data.map(item => item.total_cash_flow);
    } catch (error) {
        console.error('Error fetching historical cash flow:', error);
        return [];
    }
}

/**
 * Redă un grafic liniar care afișează datele istorice și estimate ale fluxului de numerar.
 * Utilizează datele obținute de la endpoint-urile istorice și estimate ale fluxului de numerar.
 * @param {number} monthsAhead - Numărul de luni viitoare pentru estimare.
 */
async function renderCashFlowTrendChart(monthsAhead) {
    try {
        const historicalData = await fetchHistoricalCashFlow();
        const estimatedData = await estimateFutureCashFlow(monthsAhead);

        //TODO - should be shown month/year
        const totalHistoricalMonths = historicalData.length;
        const labels = [
            ...historicalData.map((_, index) => `Month ${index + 1}`),
            ...new Array(monthsAhead).fill('').map((_, index) => `Month ${totalHistoricalMonths + index + 1}`)
        ];

        const historicalDataset = {
            label: 'Historical Cash Flow',
            data: historicalData,
            borderColor: 'blue',
            backgroundColor: 'rgba(0, 0, 255, 0.1)',
            fill: true,
        };

        const transitionDataset = {
            label: 'Transition Line',
            data: [
                ...new Array(totalHistoricalMonths - 1).fill(null),
                historicalData[totalHistoricalMonths - 1],
                estimatedData[0]
            ],
            borderColor: 'orange',
            backgroundColor: 'rgba(255, 165, 0, 0.1)',
            fill: true,
            borderDash: [5, 5]
        };

        const estimatedDataset = {
            label: 'Estimated Cash Flow',
            data: [
                ...new Array(totalHistoricalMonths).fill(null),
                ...estimatedData.slice(1)
            ],
            borderColor: 'orange',
            backgroundColor: 'rgba(255, 165, 0, 0.1)',
            fill: true,
            borderDash: [5, 5]
        };

        const ctx = document.getElementById('cashFlowChart').getContext('2d');

        if (cashFlowChart) {
            cashFlowChart.destroy();
        }

        cashFlowChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [historicalDataset, transitionDataset, estimatedDataset],
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true,
                    },
                },
            },
        });
    } catch (error) {
        console.error('Error rendering cash flow trend chart:', error);
    }
}

/**
 * Actualizează graficul de flux de numerar pe baza inputului de la slider.
 * Ascultă evenimentul input pe elementul slider și actualizează dinamic graficul.
 */
document.getElementById('monthSlider').addEventListener('input', async (event) => {
    const monthsAhead = parseInt(event.target.value);
    document.getElementById('selectedMonths').textContent = monthsAhead;
    await renderCashFlowTrendChart(monthsAhead);
});

/**
 * Inițializează graficele și datele atunci când pagina se încarcă.
 * Setează inițial luni în avans pentru fluxul de numerar la 6 luni.
 */
document.addEventListener('DOMContentLoaded', async () => {
    const initialMonthsAhead = 6; // Set initial estimate to 6 months
    await renderCashFlowTrendChart(initialMonthsAhead);
});

/**
 * Configurează obținerea datelor despre clienți și redarea graficelor la încărcarea paginii.
 * Inițializează sliderul și afișează datele inițiale ale graficului.
 */
window.onload = function() {
    fetchTopLoyalCustomers();
    fetchAllCustomers();
    document.getElementById("topXValue").innerText = document.getElementById("topX").value;

    document.getElementById("topX").addEventListener("input", function() {
        document.getElementById("topXValue").innerText = this.value;
        fetchTopLoyalCustomers();
    });

    renderDiscountsByGenderChart();
    renderDiscountsByAgeGroupChart();
    renderDiscountsByTypeChart();
};
