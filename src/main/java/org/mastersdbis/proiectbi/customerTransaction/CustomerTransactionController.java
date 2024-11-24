package org.mastersdbis.proiectbi.customerTransaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
public class CustomerTransactionController {

    @Autowired
    private CustomerTransactionService customerTransactionService;

    /***
     * Returnează o listă cu cei mai loiali clienți, în funcție de criteriul de loialitate specificat.
     *
     * @param topX Numărul de clienți cei mai loiali care trebuie returnați
     * @param loyaltyCriteria Criteriul de loialitate după care sunt ordonați clienții (ex. "total_spent", "purchase_frequency", "combined")
     * @return ResponseEntity cu lista de clienți cei mai loiali și statusul HTTP OK
     */
    @GetMapping("/top-loyal-customers")
    public ResponseEntity<List<Map<String, Object>>> getTopLoyalCustomers(
            @RequestParam int topX,
            @RequestParam String loyaltyCriteria) {
        List<Map<String, Object>> topCustomers = customerTransactionService.getTopLoyalCustomers(topX, loyaltyCriteria);
        return new ResponseEntity<>(topCustomers, HttpStatus.OK);
    }

    /***
     * Returnează lista completă a clienților din baza de date, incluzând detalii despre achiziții și statistici demografice.
     *
     * @return ResponseEntity cu lista de clienți și statusul HTTP OK
     */
    @GetMapping("/all-customers")
    public ResponseEntity<List<Map<String, Object>>> getAllCustomers() {
        List<Map<String, Object>> customers = customerTransactionService.getAllCustomers();
        return new ResponseEntity<>(customers, HttpStatus.OK);
    }

    /***
     * Corelează reducerile aplicate în funcție de gen.
     *
     * @return Lista de mapări conținând vânzările, veniturile și numărul de reduceri aplicate pe gen
     */
    @GetMapping("/correlate-discounts/gender")
    public List<Map<String, Object>> correlateDiscountsByGender() {
        return customerTransactionService.correlateDiscountsByGender();
    }

    /***
     * Corelează reducerile aplicate în funcție de grupul de vârstă.
     *
     * @return Lista de mapări conținând vânzările, veniturile și numărul de reduceri aplicate pe grupuri de vârstă
     */
    @GetMapping("/correlate-discounts/age-group")
    public List<Map<String, Object>> correlateDiscountsByAgeGroup() {
        return customerTransactionService.correlateDiscountsByAgeGroup();
    }

    /***
     * Corelează reducerile cu vânzările.
     *
     * @return Lista de mapări conținând tipurile de reduceri, vânzările totale și veniturile generate de acestea
     */
    @GetMapping("/correlate-discounts/sales")
    public List<Map<String, Object>> correlateSalesWithDiscounts() {
        return customerTransactionService.correlateSalesWithDiscounts();
    }

    /***
     * Returnează fluxul de numerar istoric, organizat pe luni.
     *
     * @return Lista de mapări reprezentând fluxul de numerar lunar total
     */
    @GetMapping("/historicalCashFlow")
    public List<Map<String, Object>> getHistoricalCashFlow() {
        return customerTransactionService.getHistoricalCashFlow();
    }

    /***
     * Estimează fluxul de numerar pentru lunile viitoare, în funcție de o perioadă specificată.
     * Valoarea estimată este generată folosind regresie liniară pe baza datelor istorice.
     *
     * @param monthsAhead Numărul de luni pentru care se dorește estimarea fluxului de numerar (între 0 și 12 luni)
     * @return ResponseEntity cu lista fluxurilor de numerar estimate și statusul HTTP OK; un status de eroare este returnat pentru o valoare nevalidă a parametrului
     */
    @GetMapping("/estimate-cash-flow")
    public ResponseEntity<List<Double>> estimateCashFlow(@RequestParam int monthsAhead) {
        if (monthsAhead < 0 || monthsAhead > 12) {
            return ResponseEntity.badRequest().body(null);
        }

        List<Double> estimatedCashFlows = customerTransactionService.estimateCashFlow(monthsAhead);
        return ResponseEntity.ok(estimatedCashFlows);
    }

    @GetMapping("/export-all-customers")
    public ResponseEntity<byte[]> exportAllCustomers(@RequestParam int topX, @RequestParam String loyaltyCriteria) {
        try {
            ByteArrayOutputStream excelFile = customerTransactionService.generateAllCustomersExcel(topX, loyaltyCriteria);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=customerLoyalty.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelFile.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
