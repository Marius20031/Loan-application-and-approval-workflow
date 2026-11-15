package com.loanworkflow.loan;

import org.junit.jupiter.api.Nested;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.loanworkflow.naive.NaiveLoanEngine;

/**
 * The same HTTP tests run against both engines. If a test passes for one and
 * fails for the other, the two variants do not behave the same.
 */
class EngineContractTest {

    @Nested
    @WebMvcTest(LoanController.class)
    @Import({InMemoryLoanRepository.class, PatternsLoanEngine.class, ApiExceptionHandler.class})
    class Patterns {

        @Nested
        class Create extends LoanControllerTest {
        }

        @Nested
        class Documents extends LoanDocumentsTest {
        }

        @Nested
        class Scoring extends LoanScoringTest {
        }

        @Nested
        class Approval extends LoanApprovalTest {
        }

        @Nested
        class Schedule extends LoanScheduleTest {
        }
    }

    @Nested
    @WebMvcTest(LoanController.class)
    @Import({NaiveLoanEngine.class, ApiExceptionHandler.class})
    @TestPropertySource(properties = "loan.variant=naive")
    class Naive {

        @Nested
        class Create extends LoanControllerTest {
        }

        @Nested
        class Documents extends LoanDocumentsTest {
        }

        @Nested
        class Scoring extends LoanScoringTest {
        }

        @Nested
        class Approval extends LoanApprovalTest {
        }

        @Nested
        class Schedule extends LoanScheduleTest {
        }
    }
}
