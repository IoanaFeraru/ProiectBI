package org.mastersdbis.proiectbi;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/customer-transactions")
    public String customerTransactions() {
        return "customerTransactions";
    }
}
