package com.loanworkflow.fixtures;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.loanworkflow.approval.ApprovalRequest;
import com.loanworkflow.approval.Role;
import com.loanworkflow.approval.User;
import com.loanworkflow.document.DocumentStatus;
import com.loanworkflow.document.DocumentType;
import com.loanworkflow.loan.LoanApplication;
import com.loanworkflow.loan.LoanService;
import com.loanworkflow.loan.LoanStatus;
import com.loanworkflow.loan.LoanType;
import com.loanworkflow.loan.StatusChange;
import com.loanworkflow.persistence.entity.ApprovalRecordEntity;
import com.loanworkflow.persistence.entity.DocumentEntity;
import com.loanworkflow.persistence.entity.LoanApplicationEntity;
import com.loanworkflow.persistence.entity.StatusChangeEntity;
import com.loanworkflow.scoring.RuleBasedScoringStrategy;
import com.loanworkflow.scoring.ScoringStrategy;

/**
 * Fluent builder for creating {@link LoanApplicationEntity} and {@link LoanService} test fixtures.
 */
public class LoanApplicationBuilder {

    private String id = UUID.randomUUID().toString();
    private String applicantUsername = "test_applicant";
    private BigDecimal amount = new BigDecimal("10000");
    private String currency = "EUR";
    private LoanType loanType = LoanType.PERSONAL;
    private LoanStatus status = LoanStatus.SUBMITTED;
    private Clock clock = Clock.systemUTC();
    private ScoringStrategy scoringStrategy = new RuleBasedScoringStrategy();

    private final List<DocumentEntity> documents = new ArrayList<>();
    private final List<ApprovalRecordEntity> approvals = new ArrayList<>();
    private final List<StatusChange> statusHistory = new ArrayList<>();

    public static LoanApplicationBuilder aLoan() {
        return new LoanApplicationBuilder();
    }

    public LoanApplicationBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public LoanApplicationBuilder withApplicant(String applicantUsername) {
        this.applicantUsername = applicantUsername;
        return this;
    }

    public LoanApplicationBuilder withAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public LoanApplicationBuilder withAmount(String amount) {
        this.amount = new BigDecimal(amount);
        return this;
    }

    public LoanApplicationBuilder withAmount(long amount) {
        this.amount = BigDecimal.valueOf(amount);
        return this;
    }

    public LoanApplicationBuilder withCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public LoanApplicationBuilder withLoanType(LoanType loanType) {
        this.loanType = loanType;
        return this;
    }

    public LoanApplicationBuilder withStatus(LoanStatus status) {
        this.status = status;
        return this;
    }

    public LoanApplicationBuilder withClock(Clock clock) {
        this.clock = clock;
        return this;
    }

    public LoanApplicationBuilder withScoringStrategy(ScoringStrategy scoringStrategy) {
        this.scoringStrategy = scoringStrategy;
        return this;
    }

    public LoanApplicationBuilder withDocument(DocumentEntity document) {
        this.documents.add(document);
        return this;
    }

    public LoanApplicationBuilder withDocument(
            DocumentType type,
            String fileReference,
            DocumentStatus documentStatus,
            Instant expiresAt
    ) {
        DocumentEntity doc = new DocumentEntity(type, fileReference, documentStatus, expiresAt);
        this.documents.add(doc);
        return this;
    }

    public LoanApplicationBuilder withApproval(String approverName, int sequenceNumber, Instant approvedAt) {
        this.approvals.add(new ApprovalRecordEntity(approverName, sequenceNumber, approvedAt));
        return this;
    }

    public LoanApplicationBuilder withApproval(String approverName) {
        int seq = this.approvals.size() + 1;
        this.approvals.add(new ApprovalRecordEntity(approverName, seq, clock.instant()));
        return this;
    }

    public LoanApplicationBuilder withStatusChange(LoanStatus from, LoanStatus to, Instant at) {
        this.statusHistory.add(new StatusChange(from, to, at));
        return this;
    }

    public LoanApplicationEntity buildEntity() {
        LoanApplicationEntity entity = new LoanApplicationEntity(applicantUsername, amount, loanType);
        entity.setId(id);
        entity.setCurrency(currency);
        entity.setStatus(status);

        for (DocumentEntity doc : documents) {
            entity.addDocument(doc);
        }
        for (ApprovalRecordEntity approval : approvals) {
            entity.addApproval(approval);
        }
        for (StatusChange change : statusHistory) {
            entity.addStatusChange(new StatusChangeEntity(
                    change.from(),
                    change.to(),
                    change.at(),
                    null
            ));
        }

        return entity;
    }

    public LoanService buildService() {
        User applicant = new User(applicantUsername, Role.APPLICANT);
        ApprovalRequest approval = new ApprovalRequest(applicant, amount);
        for (ApprovalRecordEntity record : approvals) {
            approval.restoreApprover(record.getApproverName());
        }

        LoanApplication loanApp = new LoanApplication(clock, status, statusHistory);
        LoanService service = new LoanService(loanApp, approval, amount, scoringStrategy, clock);
        service.setId(id);
        return service;
    }

    public LoanApplicationEntity build() {
        return buildEntity();
    }
}
