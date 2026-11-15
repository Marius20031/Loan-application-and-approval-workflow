package com.loanworkflow.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.loanworkflow.document.DocumentStatus;
import com.loanworkflow.document.DocumentType;
import com.loanworkflow.loan.LoanService;
import com.loanworkflow.loan.LoanStatus;
import com.loanworkflow.loan.LoanType;
import com.loanworkflow.persistence.entity.LoanApplicationEntity;

class LoanApplicationBuilderTest {

    @Test
    void buildsDefaultEntity() {
        LoanApplicationEntity entity = LoanApplicationBuilder.aLoan().buildEntity();

        assertNotNull(entity.getId());
        assertEquals("test_applicant", entity.getApplicantUsername());
        assertEquals(new BigDecimal("10000"), entity.getAmount());
        assertEquals("EUR", entity.getCurrency());
        assertEquals(LoanType.PERSONAL, entity.getLoanType());
        assertEquals(LoanStatus.SUBMITTED, entity.getStatus());
    }

    @Test
    void buildsEntityWithDocumentsApprovalsAndHistory() {
        Instant fixedNow = Instant.parse("2026-06-01T10:00:00Z");
        LoanApplicationEntity entity = LoanApplicationBuilder.aLoan()
                .withId("custom-id-123")
                .withApplicant("daniela")
                .withAmount("75000")
                .withCurrency("USD")
                .withLoanType(LoanType.BUSINESS)
                .withStatus(LoanStatus.APPROVED)
                .withDocument(DocumentType.IDENTITY_CARD, "ref-id", DocumentStatus.VERIFIED, fixedNow.plusSeconds(86400))
                .withApproval("bob_approver", 1, fixedNow)
                .withStatusChange(LoanStatus.SUBMITTED, LoanStatus.DOCUMENTS_CHECKED, fixedNow)
                .buildEntity();

        assertEquals("custom-id-123", entity.getId());
        assertEquals("daniela", entity.getApplicantUsername());
        assertEquals(new BigDecimal("75000"), entity.getAmount());
        assertEquals("USD", entity.getCurrency());
        assertEquals(LoanType.BUSINESS, entity.getLoanType());
        assertEquals(LoanStatus.APPROVED, entity.getStatus());
        assertEquals(1, entity.getDocuments().size());
        assertEquals(1, entity.getApprovals().size());
        assertEquals(1, entity.getStatusHistory().size());
    }

    @Test
    void buildsDomainLoanService() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);
        LoanService service = LoanApplicationBuilder.aLoan()
                .withApplicant("elena")
                .withAmount(5000)
                .withClock(fixedClock)
                .withApproval("bob_approver")
                .buildService();

        assertEquals("elena", service.applicant().name());
        assertEquals(BigDecimal.valueOf(5000), service.amount());
        assertEquals(1, service.approval().approvals());
        assertEquals(fixedClock, service.clock());
    }
}
