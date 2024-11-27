package org.mastersdbis.proiectbi.customerTransaction;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    //MAPS
    public List<Map<String, Object>> getAgeGroupByState(String state) {
        String sql = """
        SELECT
            purchaselocation AS state,
            age_group,
            SUM(net_amount) AS total_sales
        FROM customer_transactions
        WHERE purchaselocation = ?
        GROUP BY purchaselocation, age_group
        ORDER BY state;
    """;
        return jdbcTemplate.queryForList(sql, state);
    }

    public List<Map<String, Object>> getProduseByState(String state) {
        String sql = """
        SELECT
            purchaselocation AS state,
            product_category AS produs,
            COUNT(product_category) AS produsTotal
        FROM customer_transactions
        WHERE purchaselocation = ?
        GROUP BY purchaselocation, product_category
        ORDER BY state;
    """;
        return jdbcTemplate.queryForList(sql, state);
    }

    public List<Map<String, Object>> getVanzariState() {
        String sql = """
                SELECT
                    purchaselocation AS state,
                    ROUND(SUM(net_amount)) AS total_sales
                FROM customer_transactions
                GROUP BY purchaselocation
                ORDER BY state;
                """;
        return jdbcTemplate.queryForList(sql);
    }
}
