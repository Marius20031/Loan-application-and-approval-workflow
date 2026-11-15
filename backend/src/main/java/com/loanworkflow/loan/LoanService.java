package com.loanworkflow.loan;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

import com.loanworkflow.approval.ApprovalException;
import com.loanworkflow.approval.ApprovalRequest;
import com.loanworkflow.approval.User;
import com.loanworkflow.document.DocumentCheckService;
import com.loanworkflow.persistence.entity.DocumentEntity;
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
    private final Clock clock;
    private String id;
    private ScoreResult scoreResult;

    public LoanService(
            LoanApplication loan,
            ApprovalRequest approval,
            BigDecimal amount,
            ScoringStrategy scoring,
            Clock clock
    ) {
        this.loan = loan;
        this.approval = approval;
        this.amount = amount;
        this.scoring = scoring;
        this.clock = clock;
    }

    public LoanService(User applicant, BigDecimal amount, ScoringStrategy scoring, Clock clock) {
        this(
                new LoanApplication(clock),
                new ApprovalRequest(applicant, amount),
                amount,
                scoring,
                clock
        );
    }

    public String id() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Clock clock() {
        return clock;
    }

    public User applicant() {
        return approval.applicant();
    }

    public ApprovalRequest approval() {
        return approval;
    }

    public LoanApplication loan() {
        return loan;
    }

    public BigDecimal amount() {
        return amount;
    }

    public ScoreResult scoreResult() {
        return scoreResult;
    }

    // Step 1: an officer confirms the documents are complete
    public void checkDocuments() {
        loan.moveTo(LoanStatus.DOCUMENTS_CHECKED);
    }

    // Step 1 (validated): verifies that all required documents for loanType and amount are valid
    public void checkDocuments(
            DocumentCheckService checkService,
            LoanType loanType,
            List<DocumentEntity> documents
    ) {
        checkService.verifyDocuments(loanType, amount, documents);
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
