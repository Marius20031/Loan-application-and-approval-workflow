package com.loanworkflow.loan;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.loanworkflow.repayment.Installment;
import com.loanworkflow.scoring.CreditProfile;

@RestController
public class LoanController {

    public record CreateLoanRequest(String applicant, BigDecimal amount) {
    }

    public record ApproveRequest(String approver) {
    }

    private final LoanEngine engine;

    public LoanController(LoanEngine engine) {
        this.engine = engine;
    }

    // A new application is saved and starts as SUBMITTED
    @PostMapping("/loans")
    public ResponseEntity<LoanResponse> create(@RequestBody CreateLoanRequest request) {
        LoanResponse loan = engine.create(request.applicant(), request.amount());
        return ResponseEntity.created(URI.create("/loans/" + loan.id())).body(loan);
    }

    @GetMapping("/loans/{id}")
    public LoanResponse get(@PathVariable String id) {
        return engine.get(id);
    }

    // An officer confirms the documents are complete
    @PostMapping("/loans/{id}/documents")
    public LoanResponse checkDocuments(@PathVariable String id) {
        return engine.checkDocuments(id);
    }

    // The applicant's finances are scored; a HIGH risk loan is rejected right away
    @PostMapping("/loans/{id}/score")
    public ScoreResponse score(@PathVariable String id, @RequestBody CreditProfile profile) {
        return engine.score(id, profile.monthlyIncome(), profile.monthlyDebt(),
                profile.yearsEmployed(), profile.missedPayments());
    }

    // An approver signs off. Large loans need two different approvers.
    @PostMapping("/loans/{id}/approve")
    public LoanResponse approve(@PathVariable String id, @RequestBody ApproveRequest request) {
        return engine.approve(id, request.approver());
    }

    // The money is paid out and the repayment period starts
    @PostMapping("/loans/{id}/disburse")
    public LoanResponse disburse(@PathVariable String id) {
        return engine.disburse(id);
    }

    // Repayment plan for an approved loan. method = annuity or fixed-principal.
    @GetMapping("/loans/{id}/schedule")
    public List<Installment> schedule(@PathVariable String id,
                                      @RequestParam(defaultValue = "annuity") String method,
                                      @RequestParam BigDecimal rate,
                                      @RequestParam int months) {
        return engine.schedule(id, method, rate, months);
    }
}
