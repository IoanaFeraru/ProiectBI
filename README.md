# Tema nr. 8 - Business Intelligence

## Obiective

1. **Identificarea clienților fideli** prin intermediul mai multor criterii:
    - Frecvența achizițiilor
    - Valoarea achizițiilor

2. **Clasificarea clienților**:
    - Clienți care plătesc prompt
    - Clienți cu întârzieri la plată

3. **Corelarea discounturilor** cu vânzarile vânzărilor, pe baza:
    - Genului clientului
    - Grupei de varsta a clientului
    - Tipului de discount

4. **Estimarea fluxului de numerar** pentru un anumit numar de luni următoare.

## Script pentru Crearea Bazei de Date în PostgreSQL

Următorul script SQL va crea tabelul necesar pentru stocarea tranzacțiilor clienților:

```sql
CREATE TABLE customer_transactions
(
    cid                 BIGINT NOT NULL,
    tid                 BIGINT NOT NULL,
    gender              VARCHAR(10),
    age_group           VARCHAR(20),
    purchase_date       TIMESTAMP,
    product_category    VARCHAR(50),
    discount_availed    BOOLEAN,
    discount_name       VARCHAR(50),
    discount_amount_inr NUMERIC(10, 2),
    gross_amount        NUMERIC(10, 2),
    net_amount          NUMERIC(10, 2),
    purchase_method     VARCHAR(50),
    purchaselocation    VARCHAR(100),
    pay_date            TIMESTAMP,
    client_name         VARCHAR(150),
    PRIMARY KEY (cid, tid)
);
```

## Sursa de date csv: [sursaBazaDeDate.csv](sursaBazaDeDate.csv)