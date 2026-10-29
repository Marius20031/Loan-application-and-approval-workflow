package com.loanworkflow.approval;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Approval of one loan. Small loans need one approver; loans above the threshold
 * need two different approvers (the four-eyes principle). Nobody may approve
 * their own application, and nobody may approve twice.
 */
public class ApprovalRequest {

    static final BigDecimal FOUR_EYES_THRESHOLD = new BigDecimal("10000");

    private final User applicant;
    private final BigDecimal amount;
    private final Set<String> approvedBy = new LinkedHashSet<>();

    public ApprovalRequest(User applicant, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        this.applicant = applicant;
        this.amount = amount;
    }

    public int requiredApprovals() {
        return amount.compareTo(FOUR_EYES_THRESHOLD) > 0 ? 2 : 1;
    }

    public void approve(User approver) {
        if (approver.role() != Role.APPROVER) {
            throw new ApprovalException(approver.name() + " is not allowed to approve loans");
        }
        if (approver.name().equals(applicant.name())) {
            throw new ApprovalException("Applicants cannot approve their own loan");
        }
        if (isFullyApproved()) {
            throw new ApprovalException("Loan is already fully approved");
        }
        if (!approvedBy.add(approver.name())) {
            throw new ApprovalException(approver.name() + " has already approved this loan");
        }
    }

    public int approvals() {
        return approvedBy.size();
    }

    public boolean isFullyApproved() {
        return approvedBy.size() >= requiredApprovals();
    }
}
