package com.loanworkflow.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.loanworkflow.approval.Role;
import com.loanworkflow.approval.User;
import com.loanworkflow.document.DocumentStatus;
import com.loanworkflow.loan.LoanService;
import com.loanworkflow.loan.LoanStatus;
import com.loanworkflow.loan.LoanType;
import com.loanworkflow.persistence.entity.LoanApplicationEntity;
import com.loanworkflow.scoring.CreditProfile;

class TestDataMotherTest {

    @Test
    void verifiesStandardUsers() {
        User applicant = TestDataMother.standardApplicant();
        assertEquals("alice_applicant", applicant.name());
        assertEquals(Role.APPLICANT, applicant.role());

        User approver1 = TestDataMother.firstApprover();
        assertEquals(Role.APPROVER, approver1.role());

        User approver2 = TestDataMother.secondApprover();
        assertEquals(Role.APPROVER, approver2.role());

        User officer = TestDataMother.officerUser();
        assertEquals(Role.OFFICER, officer.role());
    }

    @Test
    void verifiesCreditProfiles() {
        CreditProfile low = TestDataMother.lowRiskCreditProfile();
        assertTrue(low.monthlyIncome().compareTo(low.monthlyDebt()) > 0);
        assertEquals(0, low.missedPayments());

        CreditProfile high = TestDataMother.highRiskCreditProfile();
        assertTrue(high.missedPayments() > 0);
    }

    @Test
    void verifiesMortgageFixtureHasAllDocuments() {
        LoanApplicationEntity mortgage = TestDataMother.createMortgageWithCompleteDocuments();

        assertEquals(LoanType.MORTGAGE, mortgage.getLoanType());
        assertEquals(LoanStatus.DOCUMENTS_CHECKED, mortgage.getStatus());
        assertEquals(5, mortgage.getDocuments().size());
        assertTrue(mortgage.getDocuments().stream().allMatch(d -> d.getStatus() == DocumentStatus.VERIFIED));
    }

    @Test
    void verifiesBusinessLoanHasTwoApprovals() {
        LoanApplicationEntity businessLoan = TestDataMother.createFullyApprovedBusinessLoan();

        assertEquals(LoanType.BUSINESS, businessLoan.getLoanType());
        assertEquals(LoanStatus.APPROVED, businessLoan.getStatus());
        assertEquals(2, businessLoan.getApprovals().size());
        assertEquals(3, businessLoan.getStatusHistory().size());
    }

    @Test
    void verifiesExpiredDocumentLoanFixture() {
        LoanApplicationEntity expiredLoan = TestDataMother.createExpiredDocumentLoan();

        assertFalse(expiredLoan.getDocuments().isEmpty());
        assertEquals(DocumentStatus.EXPIRED, expiredLoan.getDocuments().get(0).getStatus());
    }

    @Test
    void verifiesDomainLoanServiceFixtures() {
        LoanService submitted = TestDataMother.createSubmittedLoanService();
        assertEquals(LoanStatus.SUBMITTED, submitted.loan().status());

        LoanService scored = TestDataMother.createScoredPersonalLoanService();
        assertEquals(LoanStatus.SCORED, scored.loan().status());
        assertNotNull(scored.scoreResult());

        LoanService approved = TestDataMother.createApprovedPersonalLoanService();
        assertEquals(LoanStatus.APPROVED, approved.loan().status());
        assertTrue(approved.approval().isFullyApproved());
    }
}
