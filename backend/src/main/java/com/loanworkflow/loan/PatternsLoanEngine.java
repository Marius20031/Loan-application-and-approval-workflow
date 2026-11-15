package com.loanworkflow.loan;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.loanworkflow.approval.Role;
import com.loanworkflow.approval.User;
import com.loanworkflow.repayment.AnnuityStrategy;
import com.loanworkflow.repayment.FixedPrincipalStrategy;
import com.loanworkflow.repayment.Installment;
import com.loanworkflow.repayment.RepaymentStrategy;
import com.loanworkflow.scoring.CreditProfile;
import com.loanworkflow.scoring.RuleBasedScoringStrategy;
import com.loanworkflow.scoring.ScoreResult;

/** The default engine: it delegates to the domain classes (state, scoring, approval, repayment). */
@Service
@ConditionalOnProperty(name = "loan.variant", havingValue = "patterns", matchIfMissing = true)
public class PatternsLoanEngine implements LoanEngine {

    private final LoanRepository repository;

    public PatternsLoanEngine(LoanRepository repository) {
        this.repository = repository;
    }

    @Override
    public LoanResponse create(String applicant, BigDecimal amount) {
        if (applicant == null || applicant.isBlank() || amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("applicant and positive amount required");
        }
        LoanService loan = new LoanService(new User(applicant, Role.APPLICANT), amount,
                new RuleBasedScoringStrategy(), Clock.systemUTC());
        return toResponse(repository.save(loan), loan);
    }

    @Override
    public LoanResponse get(String id) {
        return toResponse(id, find(id));
    }

    @Override
    public LoanResponse checkDocuments(String id) {
        LoanService loan = find(id);
        loan.checkDocuments();
        return toResponse(id, loan);
    }

    @Override
    public ScoreResponse score(String id, BigDecimal monthlyIncome, BigDecimal monthlyDebt,
                               int yearsEmployed, int missedPayments) {
        LoanService loan = find(id);
        loan.score(new CreditProfile(monthlyIncome, monthlyDebt, yearsEmployed, missedPayments));
        ScoreResult result = loan.scoreResult();
        return new ScoreResponse(id, loan.loan().status(), result.score(), result.risk());
    }

    @Override
    public LoanResponse approve(String id, String approver) {
        if (approver == null || approver.isBlank()) {
            throw new IllegalArgumentException("approver is required");
        }
        LoanService loan = find(id);
        loan.approve(new User(approver, Role.APPROVER));
        return toResponse(id, loan);
    }

    @Override
    public LoanResponse disburse(String id) {
        LoanService loan = find(id);
        loan.disburse();
        return toResponse(id, loan);
    }

    @Override
    public List<Installment> schedule(String id, String method, BigDecimal rate, int months) {
        if (rate.signum() < 0 || months <= 0) {
            throw new IllegalArgumentException("rate must not be negative and months must be positive");
        }
        return find(id).repaymentSchedule(strategyFor(method), rate, months);
    }

    private static RepaymentStrategy strategyFor(String method) {
        return switch (method) {
            case "annuity" -> new AnnuityStrategy();
            case "fixed-principal" -> new FixedPrincipalStrategy();
            default -> throw new IllegalArgumentException("method must be annuity or fixed-principal");
        };
    }

    // Same 404 as before when the loan does not exist
    private LoanService find(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "loan not found"));
    }

    private static LoanResponse toResponse(String id, LoanService loan) {
        return new LoanResponse(id, loan.loan().status(), loan.amount());
    }
}
