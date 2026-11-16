package com.loanworkflow.persistence.benchmark;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.loanworkflow.document.DocumentStatus;
import com.loanworkflow.document.DocumentType;
import com.loanworkflow.loan.LoanStatus;
import com.loanworkflow.loan.LoanType;
import com.loanworkflow.persistence.entity.DocumentEntity;
import com.loanworkflow.persistence.entity.LoanApplicationEntity;
import com.loanworkflow.persistence.entity.StatusChangeEntity;
import com.loanworkflow.persistence.repository.LoanApplicationRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Benchmark tests measuring metrics from COMPARISON.md:
 * - Metric #6: Response time (mean, median, p95)
 * - Metric #7: Memory (heap usage before and after batch)
 * - Metric #8: Scalability (throughput and batch persistence)
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DatabasePerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(DatabasePerformanceTest.class);

    @Autowired
    private LoanApplicationRepository loanRepository;

    @BeforeEach
    void warmUp() {
        // Warm-up to ensure JIT compiler and Hibernate metadata caches are initialized
        for (int i = 0; i < 20; i++) {
            LoanApplicationEntity loan = createSampleLoan("warmup_" + i);
            LoanApplicationEntity saved = loanRepository.save(loan);
            loanRepository.findById(saved.getId());
        }
        loanRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        loanRepository.deleteAll();
    }

    @Test
    void measuresResponseTimeMetricsForSaveAndFindOperations() {
        int sampleSize = 100;
        List<Double> saveLatenciesMs = new ArrayList<>();
        List<Double> findLatenciesMs = new ArrayList<>();
        List<String> ids = new ArrayList<>();

        for (int i = 0; i < sampleSize; i++) {
            LoanApplicationEntity loan = createSampleLoan("bench_user_" + i);

            long startSave = System.nanoTime();
            LoanApplicationEntity saved = loanRepository.save(loan);
            long endSave = System.nanoTime();

            saveLatenciesMs.add((endSave - startSave) / 1_000_000.0);
            ids.add(saved.getId());
        }

        for (String id : ids) {
            long startFind = System.nanoTime();
            var found = loanRepository.findById(id);
            long endFind = System.nanoTime();

            assertTrue(found.isPresent());
            findLatenciesMs.add((endFind - startFind) / 1_000_000.0);
        }

        Collections.sort(saveLatenciesMs);
        Collections.sort(findLatenciesMs);

        double meanSave = calculateMean(saveLatenciesMs);
        double medianSave = calculatePercentile(saveLatenciesMs, 50);
        double p95Save = calculatePercentile(saveLatenciesMs, 95);

        double meanFind = calculateMean(findLatenciesMs);
        double medianFind = calculatePercentile(findLatenciesMs, 50);
        double p95Find = calculatePercentile(findLatenciesMs, 95);

        log.info("=== Database Performance Metrics (Sample size: {}) ===", sampleSize);
        log.info("Save (ms) -> Mean: {:.3f}, Median: {:.3f}, P95: {:.3f}", meanSave, medianSave, p95Save);
        log.info("Find (ms) -> Mean: {:.3f}, Median: {:.3f}, P95: {:.3f}", meanFind, medianFind, p95Find);

        // Sanity assertions on latency: in-memory H2 operations should be sub-50ms on average
        assertTrue(meanSave < 50.0, "Mean save latency should be under 50ms in test environment");
        assertTrue(meanFind < 25.0, "Mean find latency should be under 25ms in test environment");
        assertTrue(medianSave > 0.0);
        assertTrue(medianFind > 0.0);
    }

    @Test
    void measuresScalabilityAndBatchPersistenceThroughput() {
        int batchSize = 500;
        List<LoanApplicationEntity> batch = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            batch.add(createSampleLoan("batch_user_" + i));
        }

        long startBatch = System.nanoTime();
        List<LoanApplicationEntity> saved = loanRepository.saveAll(batch);
        long endBatch = System.nanoTime();

        double elapsedSeconds = (endBatch - startBatch) / 1_000_000_000.0;
        double throughput = batchSize / elapsedSeconds;

        log.info("=== Scalability Metrics (Batch size: {}) ===", batchSize);
        log.info("Batch persistence time: {:.3f} seconds", elapsedSeconds);
        log.info("Throughput: {:.2f} loans/second", throughput);

        assertNotNull(saved);
        assertFalse(saved.isEmpty());
        assertTrue(throughput > 50.0, "Throughput should exceed 50 entities/second");
    }

    @Test
    void measuresHeapMemoryBeforeAndAfterBatchOperations() {
        int count = 300;
        System.gc();
        long initialMemoryBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        List<LoanApplicationEntity> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(createSampleLoan("mem_user_" + i));
        }
        loanRepository.saveAll(list);

        long afterPersistenceBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryDeltaKb = (afterPersistenceBytes - initialMemoryBytes) / 1024;

        log.info("=== Memory Metrics ===");
        log.info("Memory delta for {} entities: {} KB", count, memoryDeltaKb);

        assertTrue(loanRepository.count() >= count);
    }

    private LoanApplicationEntity createSampleLoan(String username) {
        LoanApplicationEntity loan = new LoanApplicationEntity(
                username,
                new BigDecimal("15000.00"),
                LoanType.AUTO
        );
        loan.setStatus(LoanStatus.DOCUMENTS_CHECKED);

        DocumentEntity doc = new DocumentEntity(
                DocumentType.IDENTITY_CARD,
                "id_" + username + ".pdf"
        );
        doc.setStatus(DocumentStatus.VERIFIED);
        loan.addDocument(doc);

        StatusChangeEntity change = new StatusChangeEntity(
                LoanStatus.SUBMITTED,
                LoanStatus.DOCUMENTS_CHECKED,
                Instant.now(),
                "Docs checked"
        );
        loan.addStatusChange(change);
        return loan;
    }

    private double calculateMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double calculatePercentile(List<Double> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) {
            return 0.0;
        }
        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
    }
}
