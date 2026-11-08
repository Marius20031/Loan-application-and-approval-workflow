# Comparison

We implement the same loan flow twice and compare the two implementations on maintainability, correctness and performance.

## The two implementations

Both sit behind the `LoanEngine` interface and answer the same endpoints. `loan.variant=patterns` (default) or `loan.variant=naive` picks one.

| | patterns | naive |
|---|---|---|
| Structure | many small classes | one class (`NaiveLoanEngine`) |
| Repayment and scoring | Strategy | conditions inside the methods |
| Loan status | State (`LoanStatus`) | text values |
| Notifications | Observer (`StatusListener`) | counters in the same class |
| Entry point | Facade (`LoanService`) | the engine itself |
| Wiring and storage | Dependency Injection, Repository | shared static store |

## What we compare and how

| # | Metric | How | Status |
|---|--------|-----|--------|
| 1 | Cyclomatic complexity | `python3 backend/measure/complexity.py <path>` | done |
| 2 | Change effort | add a new repayment method, scoring rule and status to both, then count files and lines with `git diff --stat` | planned |
| 3 | Code size | files and lines per variant | planned |
| 4 | Correctness | the same 29 HTTP tests on both (`EngineContractTest`), `cd backend && mvn test` | done |
| 5 | Concurrency | 10,000 parallel requests, then check statuses and counters | planned |
| 6 | Response time | run the engine directly, with warm-up and repeated runs, report mean and median | planned |
| 7 | Memory | heap before and after 10,000 loans | planned |
| 8 | Scalability | the same run with 10,000 and 100,000 loans | planned |

Performance is measured on the engine directly, without Spring and HTTP, so the framework does not hide differences. HTTP time is reported separately.

## Results so far

Cyclomatic complexity (`backend/measure/results-complexity.md`):

| Variant | Methods | Total | Worst method | Average |
|---|---|---|---|---|
| patterns | 45 | 92 | 8 | 2.0 |
| naive | 10 | 51 | 15 | 5.1 |

Per method, naive is higher. The total is higher for patterns, because the same logic is split over more methods and files. Both go in the report.

## Rules

- Same inputs, same rules and the same tests for both.
- We do not assume either variant wins on every metric.
- We report what we measure, including when there is no difference.
