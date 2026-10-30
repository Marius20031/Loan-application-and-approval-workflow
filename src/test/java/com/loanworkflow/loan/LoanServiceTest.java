package com.loanworkflow.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.loanworkflow.approval.ApprovalException;
import com.loanworkflow.approval.Role;
import com.loanworkflow.approval.User;
import com.loanworkflow.repayment.AnnuityStrategy;
import com.loanworkflow.scoring.CreditProfile;
import com.loanworkflow.scoring.RiskLevel;
import com.loanworkflow.scoring.RuleBasedScoringStrategy;

class LoanServiceTest {

    private final User applicant = new User("carol", Role.APPLICANT);
    private final User anna = new User("anna", Role.APPROVER);
    private final User ben = new User("ben", Role.APPROVER);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), ZoneOffset.UTC);

    private static final CreditProfile GOOD =
            new CreditProfile(new BigDecimal("5000"), new BigDecimal("500"), 5, 0);
    private static final CreditProfile BAD =
            new CreditProfile(new BigDecimal("5000"), new BigDecimal("3000"), 1, 2);

    private LoanService service(String amount) {
        return new LoanService(applicant, new BigDecimal(amount), new RuleBasedScoringStrategy(), clock);
    }

    @Test
    void smallLoanGoesThroughTheWholeWorkflow() {
        LoanService s = service("5000");
        s.checkDocuments();
        s.score(GOOD);
        s.approve(anna);
        s.disburse();

        assertEquals(LoanStatus.REPAYING, s.loan().status());
        assertEquals(RiskLevel.LOW, s.scoreResult().risk());
    }

    @Test
    void largeLoanStaysScoredUntilSecondApproval() {
        LoanService s = service("50000");
        s.checkDocuments();
        s.score(GOOD);

        s.approve(anna);
        assertEquals(LoanStatus.SCORED, s.loan().status());

        s.approve(ben);
        assertEquals(LoanStatus.APPROVED, s.loan().status());
    }

    @Test
    void highRiskApplicantIsRejectedAutomatically() {
        LoanService s = service("5000");
        s.checkDocuments();
        s.score(BAD);

        assertEquals(LoanStatus.REJECTED, s.loan().status());
        assertThrows(ApprovalException.class, () -> s.approve(anna));
    }

    @Test
    void cannotApproveBeforeScoring() {
        LoanService s = service("5000");
        s.checkDocuments();
        assertThrows(ApprovalException.class, () -> s.approve(anna));
    }

    @Test
    void cannotScoreBeforeDocumentCheck() {
        assertThrows(InvalidTransitionException.class, () -> service("5000").score(GOOD));
    }

    @Test
    void cannotDisburseBeforeApproval() {
        LoanService s = service("5000");
        s.checkDocuments();
        s.score(GOOD);
        assertThrows(InvalidTransitionException.class, s::disburse);
    }

    @Test
    void scheduleOnlyAvailableAfterApproval() {
        LoanService s = service("5000");
        s.checkDocuments();
        s.score(GOOD);
        assertThrows(IllegalStateException.class,
                () -> s.repaymentSchedule(new AnnuityStrategy(), new BigDecimal("12"), 12));

        s.approve(anna);
        assertEquals(12, s.repaymentSchedule(new AnnuityStrategy(), new BigDecimal("12"), 12).size());
    }

    @Test
    void historyRecordsEveryStepWithTheClockTime() {
        LoanService s = service("5000");
        s.checkDocuments();
        s.score(GOOD);
        s.approve(anna);
        s.disburse();

        // docs, scored, approved, disbursed, repaying
        assertEquals(5, s.loan().history().size());
        assertTrue(s.loan().history().stream().allMatch(c -> c.at().equals(Instant.parse("2026-10-30T10:00:00Z"))));
    }
}
