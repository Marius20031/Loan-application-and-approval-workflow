package com.loanworkflow.persistence.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.loanworkflow.document.DocumentStatus;
import com.loanworkflow.document.DocumentType;
import com.loanworkflow.loan.LoanStatus;
import com.loanworkflow.loan.LoanType;
import com.loanworkflow.persistence.entity.ApprovalRecordEntity;
import com.loanworkflow.persistence.entity.DocumentEntity;
import com.loanworkflow.persistence.entity.LoanApplicationEntity;
import com.loanworkflow.persistence.entity.StatusChangeEntity;
import com.loanworkflow.persistence.repository.ApprovalRecordRepository;
import com.loanworkflow.persistence.repository.LoanApplicationRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Concurrency tests validating Metric #5 (Concurrency) from COMPARISON.md.
 * Verifies parallel loan creation, constraint enforcement under thread contention,
 * and data integrity across concurrent database transactions.
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DatabaseConcurrencyTest {

    @Autowired
    private LoanApplicationRepository loanRepository;

    @Autowired
    private ApprovalRecordRepository approvalRepository;

    @AfterEach
    void tearDown() {
        approvalRepository.deleteAll();
        loanRepository.deleteAll();
    }

    @Test
    void handlesConcurrentLoanSubmissionsWithoutDataLossOrCorruptedKeys() throws InterruptedException {
        int numberOfThreads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numberOfThreads);
        List<String> generatedIds = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCounter = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startSignal.await();

                    LoanApplicationEntity loan = new LoanApplicationEntity(
                            "applicant_" + index,
                            new BigDecimal("10000.00"),
                            LoanType.PERSONAL
                    );
                    DocumentEntity doc = new DocumentEntity(
                            DocumentType.IDENTITY_CARD,
                            "id_" + index + ".pdf"
                    );
                    doc.setStatus(DocumentStatus.PENDING);
                    loan.addDocument(doc);

                    StatusChangeEntity change = new StatusChangeEntity(
                            null,
                            LoanStatus.SUBMITTED,
                            Instant.now(),
                            "Concurrent submission " + index
                    );
                    loan.addStatusChange(change);

                    LoanApplicationEntity saved = loanRepository.save(loan);
                    generatedIds.add(saved.getId());
                    successCounter.incrementAndGet();
                } catch (Exception e) {
                    // unexpected error in thread
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        startSignal.countDown();
        boolean completed = doneSignal.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All concurrent tasks should complete within timeout");
        assertEquals(numberOfThreads, successCounter.get(), "Every thread should persist its loan");
        assertEquals(numberOfThreads, loanRepository.count(), "Repository must contain exactly all submitted loans");

        Set<String> uniqueIds = new HashSet<>(generatedIds);
        assertEquals(numberOfThreads, uniqueIds.size(), "Every persisted loan must have a unique ID");
    }

    @Test
    void preventsDuplicateApprovalsFromSameApproverUnderHighContention() throws InterruptedException {
        LoanApplicationEntity loan = new LoanApplicationEntity(
                "charlie_applicant",
                new BigDecimal("50000.00"),
                LoanType.MORTGAGE
        );
        loan.setStatus(LoanStatus.SCORED);
        LoanApplicationEntity savedLoan = loanRepository.save(loan);
        assertNotNull(savedLoan.getId());

        int concurrentApprovers = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentApprovers);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(concurrentApprovers);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger collisionCount = new AtomicInteger(0);

        for (int i = 0; i < concurrentApprovers; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    ApprovalRecordEntity approval = new ApprovalRecordEntity(
                            "approver_bob",
                            1,
                            Instant.now()
                    );
                    approval.setLoanApplication(savedLoan);
                    approvalRepository.save(approval);
                    successCount.incrementAndGet();
                } catch (DataIntegrityViolationException dive) {
                    collisionCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected exception
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean finished = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Approval contention tasks should complete within timeout");
        assertEquals(1, successCount.get(), "Only one approval should succeed for the same approver");
        assertEquals(concurrentApprovers - 1, collisionCount.get(), "Other concurrent attempts must be rejected");

        List<ApprovalRecordEntity> recorded = approvalRepository
                .findByLoanApplicationIdOrderBySequenceNumberAsc(savedLoan.getId());
        assertEquals(1, recorded.size(), "Database must enforce uk_loan_approver uniqueness constraint");
        assertEquals("approver_bob", recorded.get(0).getApproverName());
    }

    @Test
    void allowsDistinctConcurrentApproversUnderFourEyesPrinciple() throws InterruptedException {
        LoanApplicationEntity loan = new LoanApplicationEntity(
                "david_applicant",
                new BigDecimal("80000.00"),
                LoanType.BUSINESS
        );
        loan.setStatus(LoanStatus.SCORED);
        LoanApplicationEntity savedLoan = loanRepository.save(loan);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);

        List<String> approvers = List.of("officer_carol", "officer_dan");

        for (int i = 0; i < approvers.size(); i++) {
            final String approverName = approvers.get(i);
            final int seq = i + 1;
            executor.submit(() -> {
                try {
                    startGate.await();
                    ApprovalRecordEntity approval = new ApprovalRecordEntity(
                            approverName,
                            seq,
                            Instant.now()
                    );
                    approval.setLoanApplication(savedLoan);
                    approvalRepository.save(approval);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean finished = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished);
        assertEquals(2, successCount.get(), "Both distinct approvers should successfully record their approval");

        List<ApprovalRecordEntity> approvals = approvalRepository
                .findByLoanApplicationIdOrderBySequenceNumberAsc(savedLoan.getId());
        assertEquals(2, approvals.size(), "Both approvals must be persisted");
    }
}
