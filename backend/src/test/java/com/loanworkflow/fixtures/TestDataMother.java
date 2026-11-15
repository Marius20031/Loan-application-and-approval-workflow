package com.loanworkflow.fixtures;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.loanworkflow.approval.Role;
import com.loanworkflow.approval.User;
import com.loanworkflow.document.DocumentStatus;
import com.loanworkflow.document.DocumentType;
import com.loanworkflow.loan.LoanService;
import com.loanworkflow.loan.LoanStatus;
import com.loanworkflow.loan.LoanType;
import com.loanworkflow.persistence.entity.LoanApplicationEntity;
import com.loanworkflow.scoring.CreditProfile;

/**
 * Object Mother pattern implementation providing standard canonical test fixtures.
 */
public final class TestDataMother {

    private TestDataMother() {
        // utility class
    }

    public static User standardApplicant() {
        return new User("alice_applicant", Role.APPLICANT);
    }

    public static User firstApprover() {
        return new User("bob_approver", Role.APPROVER);
    }

    public static User secondApprover() {
        return new User("charlie_approver", Role.APPROVER);
    }

    public static User officerUser() {
        return new User("diana_officer", Role.OFFICER);
    }

    public static CreditProfile lowRiskCreditProfile() {
        return CreditProfileBuilder.aCreditProfile()
                .withMonthlyIncome("8000")
                .withMonthlyDebt("500")
                .withYearsEmployed(8)
                .withMissedPayments(0)
                .build();
    }

    public static CreditProfile mediumRiskCreditProfile() {
        return CreditProfileBuilder.aCreditProfile()
                .withMonthlyIncome("4000")
                .withMonthlyDebt("1200")
                .withYearsEmployed(3)
                .withMissedPayments(0)
                .build();
    }

    public static CreditProfile highRiskCreditProfile() {
        return CreditProfileBuilder.aCreditProfile()
                .withMonthlyIncome("1800")
                .withMonthlyDebt("1400")
                .withYearsEmployed(0)
                .withMissedPayments(3)
                .build();
    }

    public static LoanApplicationEntity createPendingPersonalLoan() {
        return LoanApplicationBuilder.aLoan()
                .withApplicant("alice_applicant")
                .withAmount("5000")
                .withLoanType(LoanType.PERSONAL)
                .withStatus(LoanStatus.SUBMITTED)
                .build();
    }

    public static LoanApplicationEntity createAutoLoanWithDocuments() {
        Instant future = Instant.now().plus(60, ChronoUnit.DAYS);
        return LoanApplicationBuilder.aLoan()
                .withApplicant("alice_applicant")
                .withAmount("20000")
                .withLoanType(LoanType.AUTO)
                .withStatus(LoanStatus.DOCUMENTS_CHECKED)
                .withDocument(DocumentType.IDENTITY_CARD, "file-id-001", DocumentStatus.VERIFIED, future)
                .withDocument(DocumentType.PROOF_OF_INCOME, "file-payslip-001", DocumentStatus.VERIFIED, future)
                .withDocument(DocumentType.COLLATERAL_PROOF, "file-car-001", DocumentStatus.VERIFIED, future)
                .build();
    }

    public static LoanApplicationEntity createMortgageWithCompleteDocuments() {
        Instant future = Instant.now().plus(120, ChronoUnit.DAYS);
        return LoanApplicationBuilder.aLoan()
                .withApplicant("alice_applicant")
                .withAmount("250000")
                .withLoanType(LoanType.MORTGAGE)
                .withStatus(LoanStatus.DOCUMENTS_CHECKED)
                .withDocument(DocumentType.IDENTITY_CARD, "mortgage-id", DocumentStatus.VERIFIED, future)
                .withDocument(DocumentType.PROOF_OF_INCOME, "mortgage-payslip", DocumentStatus.VERIFIED, future)
                .withDocument(DocumentType.TAX_RECORD, "mortgage-tax", DocumentStatus.VERIFIED, future)
                .withDocument(DocumentType.PROPERTY_DEED, "mortgage-deed", DocumentStatus.VERIFIED, future)
                .withDocument(DocumentType.FINANCIAL_STATEMENT, "mortgage-bank", DocumentStatus.VERIFIED, future)
                .build();
    }

    public static LoanApplicationEntity createExpiredDocumentLoan() {
        Instant past = Instant.now().minus(10, ChronoUnit.DAYS);
        return LoanApplicationBuilder.aLoan()
                .withApplicant("bob_applicant")
                .withAmount("7000")
                .withLoanType(LoanType.PERSONAL)
                .withStatus(LoanStatus.SUBMITTED)
                .withDocument(DocumentType.IDENTITY_CARD, "expired-id", DocumentStatus.EXPIRED, past)
                .build();
    }

    public static LoanApplicationEntity createFullyApprovedBusinessLoan() {
        Instant now = Instant.now();
        return LoanApplicationBuilder.aLoan()
                .withApplicant("business_corp")
                .withAmount("80000")
                .withLoanType(LoanType.BUSINESS)
                .withStatus(LoanStatus.APPROVED)
                .withApproval("bob_approver", 1, now.minus(2, ChronoUnit.HOURS))
                .withApproval("charlie_approver", 2, now.minus(1, ChronoUnit.HOURS))
                .withStatusChange(LoanStatus.SUBMITTED, LoanStatus.DOCUMENTS_CHECKED, now.minus(5, ChronoUnit.HOURS))
                .withStatusChange(LoanStatus.DOCUMENTS_CHECKED, LoanStatus.SCORED, now.minus(4, ChronoUnit.HOURS))
                .withStatusChange(LoanStatus.SCORED, LoanStatus.APPROVED, now.minus(1, ChronoUnit.HOURS))
                .build();
    }

    public static LoanService createSubmittedLoanService() {
        return LoanApplicationBuilder.aLoan()
                .withApplicant("alice_applicant")
                .withAmount("5000")
                .withStatus(LoanStatus.SUBMITTED)
                .buildService();
    }

    public static LoanService createScoredPersonalLoanService() {
        LoanService service = createSubmittedLoanService();
        service.checkDocuments();
        service.score(lowRiskCreditProfile());
        return service;
    }

    public static LoanService createApprovedPersonalLoanService() {
        LoanService service = createScoredPersonalLoanService();
        service.approve(firstApprover());
        return service;
    }
}
