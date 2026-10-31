package com.loanworkflow.loan;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoanController {

    public record CreateLoanRequest(String applicant, BigDecimal amount) {
    }

    // Start of the loan API: a new application begins as SUBMITTED
    @PostMapping("/loans")
    public ResponseEntity<Map<String, String>> create(@RequestBody CreateLoanRequest request) {
        if (request.applicant() == null || request.applicant().isBlank()
                || request.amount() == null || request.amount().signum() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "applicant and positive amount required"));
        }
        return ResponseEntity.status(201).body(
                Map.of("id", UUID.randomUUID().toString(), "status", LoanStatus.SUBMITTED.name()));
    }
}
