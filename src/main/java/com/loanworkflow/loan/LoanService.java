package com.loanworkflow.loan;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

import com.loanworkflow.approval.ApprovalException;
import com.loanworkflow.approval.ApprovalRequest;
import com.loanworkflow.approval.User;
import com.loanworkflow.repayment.Installment;
import com.loanworkflow.repayment.RepaymentStrategy;
import com.loanworkflow.scoring.CreditProfile;
import com.loanworkflow.scoring.RiskLevel;
import com.loanworkflow.scoring.ScoreResult;
import com.loanworkflow.scoring.ScoringStrategy;

/**
 * Facade: one simple entry point for the whole workflow, so callers do not
 * have to coordinate the state machine, scoring and approval themselves.
 */
public class LoanService {

    private final LoanApplication loan;
    private final ApprovalRequest approval;
    private final BigDecimal amount;
    private final ScoringStrategy scoring;
    private ScoreResult scoreResult;

    public LoanService(User applicant, BigDecimal amount, ScoringStrategy scoring, Clock clock) {
        this.loan = new LoanApplication(clock);
        this.approval = new ApprovalRequest(applicant, amount);
        this.amount = amount;
        this.scoring = scoring;
    }

    public LoanApplication loan() {
        return loan;
    }

    public ScoreResult scoreResult() {
        return scoreResult;
    }

    // Step 1: an officer confirms the documents are complete
    public void checkDocuments() {
        loan.moveTo(LoanStatus.DOCUMENTS_CHECKED);
    }

    // Step 2: score the applicant; high risk is rejected automatically
    public void score(CreditProfile profile) {
        loan.moveTo(LoanStatus.SCORED);
        scoreResult = scoring.score(profile);
        if (scoreResult.risk() == RiskLevel.HIGH) {
            loan.moveTo(LoanStatus.REJECTED);
        }
    }

    // Step 3: approvers sign off; the loan is approved once enough have signed
    public void approve(User approver) {
        if (loan.status() != LoanStatus.SCORED) {
            throw new ApprovalException("Loan must be scored (and not rejected) before approval");
        }
        approval.approve(approver);
        if (approval.isFullyApproved()) {
            loan.moveTo(LoanStatus.APPROVED);
        }
    }

    // Step 4: pay the money out and start the repayment period
    public void disburse() {
        loan.moveTo(LoanStatus.DISBURSED);
        loan.moveTo(LoanStatus.REPAYING);
    }

    // Repayment plan, only available once the loan is approved
    public List<Installment> repaymentSchedule(RepaymentStrategy strategy, BigDecimal annualRatePercent, int months) {
        LoanStatus s = loan.status();
        if (s != LoanStatus.APPROVED && s != LoanStatus.DISBURSED && s != LoanStatus.REPAYING) {
            throw new IllegalStateException("Loan is not approved yet");
        }
        return strategy.schedule(amount, annualRatePercent, months);
    }
}
