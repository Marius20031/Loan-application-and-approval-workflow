package com.loanworkflow.approval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ApprovalRequestTest {

    private final User applicant = new User("carol", Role.APPLICANT);
    private final User anna = new User("anna", Role.APPROVER);
    private final User ben = new User("ben", Role.APPROVER);
    private final User officer = new User("olga", Role.OFFICER);

    private ApprovalRequest small() {
        return new ApprovalRequest(applicant, new BigDecimal("5000"));
    }

    private ApprovalRequest large() {
        return new ApprovalRequest(applicant, new BigDecimal("50000"));
    }

    @Test
    void smallLoanNeedsOneApproval() {
        ApprovalRequest r = small();
        assertEquals(1, r.requiredApprovals());
        r.approve(anna);
        assertTrue(r.isFullyApproved());
    }

    @Test
    void largeLoanNeedsTwoDifferentApprovers() {
        ApprovalRequest r = large();
        assertEquals(2, r.requiredApprovals());

        r.approve(anna);
        assertFalse(r.isFullyApproved());
        r.approve(ben);
        assertTrue(r.isFullyApproved());
        assertEquals(2, r.approvals());
    }

    @Test
    void thresholdAmountItselfOnlyNeedsOneApproval() {
        ApprovalRequest r = new ApprovalRequest(applicant, new BigDecimal("10000"));
        assertEquals(1, r.requiredApprovals());
        assertEquals(2, new ApprovalRequest(applicant, new BigDecimal("10000.01")).requiredApprovals());
    }

    @Test
    void sameApproverCannotApproveTwice() {
        ApprovalRequest r = large();
        r.approve(anna);
        assertThrows(ApprovalException.class, () -> r.approve(anna));
        assertFalse(r.isFullyApproved());
    }

    @Test
    void applicantCannotApproveOwnLoan() {
        User selfApprover = new User("carol", Role.APPROVER);
        assertThrows(ApprovalException.class, () -> small().approve(selfApprover));
    }

    @Test
    void onlyApproversMayApprove() {
        assertThrows(ApprovalException.class, () -> small().approve(officer));
        assertThrows(ApprovalException.class, () -> small().approve(applicant));
    }

    @Test
    void cannotApproveAfterFullyApproved() {
        ApprovalRequest r = small();
        r.approve(anna);
        assertThrows(ApprovalException.class, () -> r.approve(ben));
    }

    @Test
    void amountMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new ApprovalRequest(applicant, BigDecimal.ZERO));
    }
}
