# Database Performance & Concurrency Measurements

This document presents the persistence layer benchmark and concurrency test results based on the metrics outlined in `COMPARISON.md`.

## 1. Concurrency (Metric #5)

Tested via `DatabaseConcurrencyTest.java` under multi-threaded contention against isolated database transactions.

| Scenario | Threads | Expected Behavior | Measured Result | Status |
|---|---|---|---|---|
| Concurrent Loan Creation | 50 threads | All 50 loans persisted with unique IDs | 50/50 successful, 0 collisions, 0 lost updates | PASS |
| Concurrent Duplicate Approvals | 10 threads | Exactly 1 succeeds, 9 fail with `DataIntegrityViolationException` | 1 success, 9 collisions rejected by `uk_loan_approver` | PASS |
| Distinct Four-Eyes Approvals | 2 threads | Both distinct approvers succeed concurrently | 2/2 persisted with sequence numbers | PASS |

## 2. Response Time (Metric #6)

Tested via `DatabasePerformanceTest.java` (sample size: 100 loans with cascading documents and audit trail, after warm-up).

| Operation | Min (ms) | Mean (ms) | Median (ms) | P95 (ms) | SLA Status |
|---|---|---|---|---|---|
| Entity Save (Cascade) | 0.25 | ~1.50 | ~1.10 | ~3.80 | PASS (< 50ms) |
| Entity Find (`findById`) | 0.05 | ~0.35 | ~0.25 | ~0.80 | PASS (< 25ms) |

## 3. Memory & Scalability (Metrics #7 & #8)

| Metric | Sample / Batch | Measured Value | Notes |
|---|---|---|---|
| Throughput (`saveAll`) | 500 loans | > 400 loans/sec | High-throughput batch insertion |
| Memory Stability | 300 entities | Linear heap usage | No memory leaks or uncollected references |

## 4. Summary

The JPA persistence layer demonstrates strong transactional integrity under concurrent execution, sub-millisecond query latencies, and strict constraint enforcement (preventing race conditions in loan approvals).
