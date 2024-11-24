package org.mastersdbis.proiectbi.customerTransaction;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomerTransactionService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /***
     * Returnează lista primilor clienți loiali, ordonați în funcție de totalul de bani cheltuiti de un client, frecventa cumparaturilor sau un scod de loialitate calculat pe baza ambelor criterii.
     *
     * @param topX Numărul de clienți cei mai loiali care vor fi returnați
     * @param loyaltyCriteria Criteriul de loialitate folosit pentru sortare (ex. "total_spent", "purchase_frequency", "combined")
     * @return Lista de clienți și informațiile despre frecvența achizițiilor, suma totală cheltuită și scorul de loialitate
     */
    public List<Map<String, Object>> getTopLoyalCustomers(int topX, String loyaltyCriteria) {
        String sql;
        switch (loyaltyCriteria.toLowerCase()) {
            case "total_spent":
                sql = """
            SELECT client_name, COUNT(tid) AS purchase_frequency, SUM(net_amount) AS total_spent,
                   SUM(net_amount) AS loyalty_score
            FROM customer_transactions
            GROUP BY client_name  
            ORDER BY total_spent DESC
            LIMIT ?
            """;
                break;
            case "purchase_frequency":
                sql = """
            SELECT client_name, COUNT(tid) AS purchase_frequency, SUM(net_amount) AS total_spent,
                   COUNT(tid) AS loyalty_score
            FROM customer_transactions
            GROUP BY client_name  
            ORDER BY purchase_frequency DESC
            LIMIT ?
            """;
                break;
            case "combined":
            default:
                sql = """
            SELECT client_name, COUNT(tid) AS purchase_frequency, SUM(net_amount) AS total_spent,
                   (SUM(net_amount) + COUNT(tid)) AS loyalty_score
            FROM customer_transactions
            GROUP BY client_name
            ORDER BY loyalty_score DESC
            LIMIT ?
            """;
                break;
        }
        return jdbcTemplate.queryForList(sql, topX);
    }

    /***
     * Returnează toți clienții din baza de date, si detalii precum: frecvența achizițiilor, suma totală cheltuită și întârzierile medii de plată.
     *
     * @return Lista de clienți cu informații demografice și statistici de achiziție
     */
    public List<Map<String, Object>> getAllCustomers() {
        String sql = """
        WITH distinct_clients AS (
            SELECT DISTINCT cid, client_name, age_group, gender
            FROM customer_transactions
        )
        SELECT dc.client_name,
               dc.age_group,
               dc.gender,
               COUNT(ct.tid) AS purchase_frequency,
               SUM(ct.net_amount) AS total_spent,
               AVG(EXTRACT(EPOCH FROM ct.pay_date - ct.purchase_date) / 86400) AS avg_payment_delay
        FROM distinct_clients dc
        JOIN customer_transactions ct ON dc.cid = ct.cid
        GROUP BY dc.cid, dc.client_name, dc.age_group, dc.gender
        ORDER BY dc.client_name
    """;
        return jdbcTemplate.queryForList(sql);
    }

    /***
     * Coralează impactul reducerilor în funcție de gen.
     *
     * @return Lista cu statistici de vânzări și venituri generate, împărțite pe gen
     */
    public List<Map<String, Object>> correlateDiscountsByGender() {
        String sql = """
        SELECT gender,
               COUNT(tid) AS total_sales,
               SUM(net_amount) AS total_revenue,
               SUM(CASE WHEN discount_availed THEN net_amount ELSE 0 END) AS revenue_with_discount,
               SUM(CASE WHEN discount_availed THEN 1 ELSE 0 END) AS discount_count
        FROM customer_transactions
        GROUP BY gender
        """;
        return jdbcTemplate.queryForList(sql);
    }

    /***
     * Coralează impactul reducerilor în funcție de grupul de vârstă.
     *
     * @return Lista cu statistici de vânzări și venituri generate, împărțite pe grupuri de vârstă
     */
    public List<Map<String, Object>> correlateDiscountsByAgeGroup() {
        String sql = """
        SELECT age_group,
               COUNT(tid) AS total_sales,
               SUM(net_amount) AS total_revenue,
               SUM(CASE WHEN discount_availed THEN net_amount ELSE 0 END) AS revenue_with_discount,
               SUM(CASE WHEN discount_availed THEN 1 ELSE 0 END) AS discount_count
        FROM customer_transactions
        GROUP BY age_group
        """;
        return jdbcTemplate.queryForList(sql);
    }

    /***
     * Coralează tipul de reducere cu vânzările.
     *
     * @return Lista cu numele reducerii, totalul vânzărilor, veniturile totale și valoarea medie a reducerii
     */
    public List<Map<String, Object>> correlateSalesWithDiscounts() {
        String sql = """
            SELECT 
                discount_name,
                COUNT(tid) AS total_sales,
                SUM(net_amount) AS total_revenue,
                AVG(discount_amount_inr) AS average_discount_amount
            FROM 
                customer_transactions
            WHERE 
                discount_availed = true
            GROUP BY 
                discount_name
            ORDER BY 
                total_revenue DESC
            """;
        return jdbcTemplate.queryForList(sql);
    }

    /***
     * Extrage fluxul de numerar istoric pe lună.
     *
     * @return Lista cu fluxurile de numerar totale lunare
     */
    public List<Map<String, Object>> getHistoricalCashFlow() {
        String sql = """
            SELECT 
                DATE_TRUNC('month', purchase_date) AS month,
                SUM(net_amount) AS total_cash_flow
            FROM 
                customer_transactions
            GROUP BY 
                month
            ORDER BY 
                month;
        """;
        return jdbcTemplate.queryForList(sql);
    }

    /***
     * Estimează fluxul de numerar viitor folosind regresie liniară pe baza datelor istorice.
     *
     * @param monthsAhead Numărul de luni pentru care se estimează fluxul de numerar
     * @return Lista cu estimările fluxului de numerar pentru fiecare lună în parte
     */
    public List<Double> estimateCashFlow(int monthsAhead) {
        List<Map<String, Object>> historicalData = getHistoricalCashFlow();
        SimpleRegression regression = new SimpleRegression();

        for (int i = 0; i < historicalData.size(); i++) {
            double monthIndex = i;
            BigDecimal cashFlowBigDecimal = (BigDecimal) historicalData.get(i).get("total_cash_flow");
            double cashFlow = cashFlowBigDecimal.doubleValue();
            regression.addData(monthIndex, cashFlow);
        }

        List<Double> futureCashFlows = new ArrayList<>();

        for (int month = 1; month <= monthsAhead; month++) {
            double predictedCashFlow = regression.predict(historicalData.size() + month);
            futureCashFlows.add(predictedCashFlow);
        }

        return futureCashFlows;
    }

    public ByteArrayOutputStream generateAllCustomersExcel(int topX, String tip) throws IOException {
        List<Map<String, Object>> customers = getExcelCustomersGrafic1();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("All Customers");

        Row headerRow = sheet.createRow(0);
        String[] headers = {"Client Name", "Purchase Frequency", "Total Spent"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(getHeaderCellStyle(workbook));
        }

        int rowIdx = 1;
        for (Map<String, Object> customer : customers) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue((String) customer.get("client_name"));
            row.createCell(1).setCellValue(((Number) customer.get("purchase_frequency")).longValue());
            row.createCell(2).setCellValue(((Number) customer.get("total_spent")).doubleValue());
        }

        createBarChart(sheet, customers, rowIdx, topX, tip);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        return bos;
    }


    public List<Map<String, Object>> getExcelCustomersGrafic1() {
        String sql = """
        SELECT client_name, COUNT(tid) AS purchase_frequency, SUM(net_amount) AS total_spent
        FROM customer_transactions
        GROUP BY client_name
        ORDER BY total_spent DESC
    """;
        return jdbcTemplate.queryForList(sql);
    }

    private void createBarChart(Sheet sheet, List<Map<String, Object>> customers, int numRows, int topX, String tip) {
        List<Map<String, Object>> topCustomers = customers.stream()
                .limit(topX)
                .toList();

        XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();

        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 5, 0, 15, 30);

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("Customer Spending and Purchase Frequency");
        chart.setTitleOverlay(false);

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Client Name");
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);


        XDDFDataSource<String> clientNames = XDDFDataSourcesFactory.fromStringCellRange(
                (XSSFSheet) sheet, new CellRangeAddress(1, topX, 0, 0));

        XDDFNumericalDataSource<Double> totalSpent = XDDFDataSourcesFactory.fromNumericCellRange(
                (XSSFSheet) sheet, new CellRangeAddress(1, topX, 2, 2));

        XDDFNumericalDataSource<Double> purchaseFrequency = XDDFDataSourcesFactory.fromNumericCellRange(
                (XSSFSheet) sheet, new CellRangeAddress(1, topX, 1, 1));

        XDDFChartData data = chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);

        if ("total_spent".equals(tip)) {
            XDDFChartData.Series series1 = data.addSeries(clientNames, totalSpent);
            series1.setTitle("Total Spent", null);
            series1.setFillProperties(new XDDFSolidFillProperties(XDDFColor.from(255, 0, 0)));
            leftAxis.setTitle("Amount");
        } else if ("purchase_frequency".equals(tip)) {
            XDDFChartData.Series series2 = data.addSeries(clientNames, purchaseFrequency);
            series2.setTitle("Purchase Frequency", null);
            series2.setFillProperties(new XDDFSolidFillProperties(XDDFColor.from(0, 0, 255)));
            leftAxis.setTitle("Frequency");
        }

        chart.plot(data);
    }


    private CellStyle getHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
