document.getElementById("topX").addEventListener("input", function() {
    document.getElementById("topXValue").innerText = this.value;
});

document.getElementById("exportButton").addEventListener("click", async () => {
    const topX = document.getElementById("topX").value;
    const loyaltyCriteria = document.querySelector('input[name="loyaltyCriteria"]:checked').value;

    try {
        const response = await fetch(`/transactions/export-all-customers?topX=${topX}&loyaltyCriteria=${loyaltyCriteria}`);
        if (!response.ok) throw new Error("Export failed");

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'customerLoyalty.xlsx';
        a.click();
    } catch (error) {
        console.error("Error exporting data:", error);
    }
});
