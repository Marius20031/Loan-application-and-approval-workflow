package com.loanworkflow.naive;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.loanworkflow.loan.LoanEngine;
import com.loanworkflow.loan.LoanResponse;
import com.loanworkflow.loan.LoanStatus;
import com.loanworkflow.loan.ScoreResponse;
import com.loanworkflow.repayment.Installment;
import com.loanworkflow.scoring.RiskLevel;

/** The whole loan flow in one class: store, scoring, approval, repayment and notifications. */
@Service
@ConditionalOnProperty(name = "loan.variant", havingValue = "naive")
public class NaiveLoanEngine implements LoanEngine {

    // One store shared by the whole application
    static final Map<String, Loan> LOANS = new ConcurrentHashMap<>();
    static int emailsSent = 0;
    static int smsSent = 0;

    static class Loan {
        String applicant;
        BigDecimal amount;
        String status = "SUBMITTED";
        Set<String> approvers = new HashSet<>();
        int score;
        String risk;
    }

    @Override
    public LoanResponse create(String applicant, BigDecimal amount) {
        if (applicant == null || applicant.isBlank() || amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("applicant and positive amount required");
        }
        Loan loan = new Loan();
        loan.applicant = applicant;
        loan.amount = amount;
        String id = UUID.randomUUID().toString();
        LOANS.put(id, loan);
        return response(id, loan);
    }

    @Override
    public LoanResponse get(String id) {
        return response(id, find(id));
    }

    @Override
    public LoanResponse checkDocuments(String id) {
        Loan loan = find(id);
        synchronized (loan) {
            if (!loan.status.equals("SUBMITTED")) {
                throw new IllegalStateException("Cannot move loan from " + loan.status + " to DOCUMENTS_CHECKED");
            }
            loan.status = "DOCUMENTS_CHECKED";
            notifyAll(id, loan.status);
        }
        return response(id, loan);
    }

    @Override
    public ScoreResponse score(String id, BigDecimal income, BigDecimal debt, int years, int missed) {
        Loan loan = find(id);
        if (income == null || income.signum() <= 0) {
            throw new IllegalArgumentException("monthlyIncome must be positive");
        }
        if (debt == null || debt.signum() < 0) {
            throw new IllegalArgumentException("monthlyDebt must not be negative");
        }
        if (years < 0 || missed < 0) {
            throw new IllegalArgumentException("yearsEmployed and missedPayments must not be negative");
        }
        synchronized (loan) {
            if (!loan.status.equals("DOCUMENTS_CHECKED")) {
                throw new IllegalStateException("Cannot move loan from " + loan.status + " to SCORED");
            }
            int score = 500;
            BigDecimal dti = debt.divide(income, MathContext.DECIMAL64);
            if (dti.compareTo(new BigDecimal("0.20")) < 0) {
                score += 150;
            } else if (dti.compareTo(new BigDecimal("0.40")) < 0) {
                score += 50;
            } else {
                score -= 100;
            }
            score += Math.min(years, 10) * 10;
            score -= missed * 75;
            if (score < 300) {
                score = 300;
            }
            if (score > 850) {
                score = 850;
            }
            String risk;
            if (score >= 700) {
                risk = "LOW";
            } else if (score >= 550) {
                risk = "MEDIUM";
            } else {
                risk = "HIGH";
            }
            loan.score = score;
            loan.risk = risk;
            loan.status = risk.equals("HIGH") ? "REJECTED" : "SCORED";
            notifyAll(id, loan.status);
            return new ScoreResponse(id, LoanStatus.valueOf(loan.status), score, RiskLevel.valueOf(risk));
        }
    }

    @Override
    public LoanResponse approve(String id, String approver) {
        if (approver == null || approver.isBlank()) {
            throw new IllegalArgumentException("approver is required");
        }
        Loan loan = find(id);
        synchronized (loan) {
            if (!loan.status.equals("SCORED")) {
                throw new IllegalStateException("Loan must be scored (and not rejected) before approval");
            }
            if (approver.equals(loan.applicant)) {
                throw new IllegalStateException("Applicants cannot approve their own loan");
            }
            if (!loan.approvers.add(approver)) {
                throw new IllegalStateException(approver + " has already approved this loan");
            }
            int required = loan.amount.compareTo(new BigDecimal("10000")) > 0 ? 2 : 1;
            if (loan.approvers.size() >= required) {
                loan.status = "APPROVED";
                notifyAll(id, loan.status);
            }
        }
        return response(id, loan);
    }

    @Override
    public LoanResponse disburse(String id) {
        Loan loan = find(id);
        synchronized (loan) {
            if (!loan.status.equals("APPROVED")) {
                throw new IllegalStateException("Cannot move loan from " + loan.status + " to DISBURSED");
            }
            loan.status = "REPAYING";
            notifyAll(id, loan.status);
        }
        return response(id, loan);
    }

    @Override
    public List<Installment> schedule(String id, String method, BigDecimal rate, int months) {
        if (rate.signum() < 0 || months <= 0) {
            throw new IllegalArgumentException("rate must not be negative and months must be positive");
        }
        Loan loan = find(id);
        if (!method.equals("annuity") && !method.equals("fixed-principal")) {
            throw new IllegalArgumentException("method must be annuity or fixed-principal");
        }
        if (!loan.status.equals("APPROVED") && !loan.status.equals("REPAYING")) {
            throw new IllegalStateException("Loan is not approved yet");
        }
        BigDecimal principal = loan.amount;
        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(1200), MathContext.DECIMAL64);
        List<Installment> result = new ArrayList<>();
        BigDecimal balance = principal;
        if (method.equals("annuity")) {
            BigDecimal payment;
            if (monthlyRate.signum() == 0) {
                payment = principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
            } else {
                BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(months, MathContext.DECIMAL64);
                BigDecimal denominator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(factor, MathContext.DECIMAL64));
                payment = principal.multiply(monthlyRate).divide(denominator, MathContext.DECIMAL64)
                        .setScale(2, RoundingMode.HALF_UP);
            }
            for (int i = 1; i <= months; i++) {
                BigDecimal interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal part = payment.subtract(interest);
                if (i == months) {
                    part = balance;
                }
                balance = balance.subtract(part);
                result.add(new Installment(i, part.add(interest), part, interest, balance));
            }
        } else {
            BigDecimal part = principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
            for (int i = 1; i <= months; i++) {
                BigDecimal interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal thisPart = (i == months) ? balance : part;
                balance = balance.subtract(thisPart);
                result.add(new Installment(i, thisPart.add(interest), thisPart, interest, balance));
            }
        }
        return result;
    }

    // Every status change sends an e-mail and an SMS
    private void notifyAll(String id, String status) {
        emailsSent++;
        smsSent++;
    }

    private Loan find(String id) {
        Loan loan = LOANS.get(id);
        if (loan == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "loan not found");
        }
        return loan;
    }

    private LoanResponse response(String id, Loan loan) {
        String status = loan.status.equals("REPAYING") ? "REPAYING" : loan.status;
        return new LoanResponse(id, LoanStatus.valueOf(status), loan.amount);
    }
}
