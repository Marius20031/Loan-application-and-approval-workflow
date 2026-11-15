package com.loanworkflow.persistence.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.loanworkflow.config.ClockConfig;
import com.loanworkflow.fixtures.TestDataMother;
import com.loanworkflow.loan.LoanService;
import com.loanworkflow.loan.LoanStatus;
import com.loanworkflow.persistence.entity.LoanApplicationEntity;
import com.loanworkflow.persistence.repository.LoanApplicationRepository;
import com.loanworkflow.persistence.repository.UserRepository;

@DataJpaTest
@ActiveProfiles("test")
@Import(ClockConfig.class)
class JpaLoanRepositoryAdapterTest {

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Clock clock;

    private JpaLoanRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaLoanRepositoryAdapter(loanApplicationRepository, userRepository, clock);
    }

    @Test
    void savesNewLoanAndPersistsToDatabase() {
        LoanService loan = TestDataMother.createSubmittedLoanService();

        String id = adapter.save(loan);

        assertNotNull(id);
        Optional<LoanService> found = adapter.findById(id);
        assertTrue(found.isPresent());
        assertEquals(LoanStatus.SUBMITTED, found.get().loan().status());
        assertEquals("alice_applicant", found.get().applicant().name());
        assertEquals(new BigDecimal("5000"), found.get().amount());

        Optional<LoanApplicationEntity> entity = loanApplicationRepository.findById(id);
        assertTrue(entity.isPresent());
        assertEquals("alice_applicant", entity.get().getApplicantUsername());
        assertEquals(LoanStatus.SUBMITTED, entity.get().getStatus());
    }

    @Test
    void persistsStatusChangesAndApprovals() {
        LoanService loan = TestDataMother.createApprovedPersonalLoanService();

        String id = adapter.save(loan);

        Optional<LoanApplicationEntity> entityOpt = loanApplicationRepository.findById(id);
        assertTrue(entityOpt.isPresent());
        LoanApplicationEntity entity = entityOpt.get();

        assertEquals(LoanStatus.APPROVED, entity.getStatus());
        assertEquals(1, entity.getApprovals().size());
        assertEquals("bob_approver", entity.getApprovals().get(0).getApproverName());
        assertTrue(entity.getStatusHistory().size() >= 3);
    }

    @Test
    void reconstructsLoanFromDatabaseAfterCacheClear() {
        LoanService loan = TestDataMother.createApprovedPersonalLoanService();
        String id = adapter.save(loan);

        adapter.clearCache();

        Optional<LoanService> reloaded = adapter.findById(id);
        assertTrue(reloaded.isPresent());
        assertEquals(LoanStatus.APPROVED, reloaded.get().loan().status());
        assertEquals("alice_applicant", reloaded.get().applicant().name());
        assertEquals(1, reloaded.get().approval().approvals());
        assertTrue(reloaded.get().approval().approvedBy().contains("bob_approver"));
    }

    @Test
    void returnsEmptyOptionalForUnknownId() {
        assertTrue(adapter.findById("non-existent-id").isEmpty());
    }

    @Test
    void everySaveGeneratesUniqueIdWhenNotSet() {
        LoanService loan1 = TestDataMother.createSubmittedLoanService();
        LoanService loan2 = TestDataMother.createSubmittedLoanService();

        String id1 = adapter.save(loan1);
        String id2 = adapter.save(loan2);

        assertNotEquals(id1, id2);
    }

    @Test
    void updatesExistingLoanOnSubsequentSave() {
        LoanService loan = TestDataMother.createSubmittedLoanService();
        String id = adapter.save(loan);

        loan.checkDocuments();
        String updatedId = adapter.save(loan);

        assertEquals(id, updatedId);
        LoanApplicationEntity entity = loanApplicationRepository.findById(id).orElseThrow();
        assertEquals(LoanStatus.DOCUMENTS_CHECKED, entity.getStatus());
    }
}
