package com.loanworkflow.loan;

import java.math.BigDecimal;
import java.util.List;

import com.loanworkflow.repayment.Installment;

/**
 * The loan operations behind the REST endpoints. The controller only talks to
 * this interface, so the logic can be swapped without touching the HTTP layer.
 */
public interface LoanEngine {

    LoanResponse create(String applicant, BigDecimal amount);

    LoanResponse get(String id);

    LoanResponse checkDocuments(String id);

    ScoreResponse score(String id, BigDecimal monthlyIncome, BigDecimal monthlyDebt,
                        int yearsEmployed, int missedPayments);

    LoanResponse approve(String id, String approver);

    LoanResponse disburse(String id);

    List<Installment> schedule(String id, String method, BigDecimal rate, int months);
}
