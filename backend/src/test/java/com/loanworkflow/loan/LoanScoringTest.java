package com.loanworkflow.loan;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

@WebMvcTest(LoanController.class)
@Import({InMemoryLoanRepository.class, PatternsLoanEngine.class, ApiExceptionHandler.class})
class LoanScoringTest extends LoanApiTestBase {

    private String loanWithDocumentsChecked() throws Exception {
        String loan = createLoan("5000");
        postEmpty(loan + "/documents");
        return loan;
    }

    @Test
    void goodProfileIsScoredAndStaysOpen() throws Exception {
        postJson(loanWithDocumentsChecked() + "/score", GOOD_PROFILE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCORED"))
                .andExpect(jsonPath("$.score").value(700))
                .andExpect(jsonPath("$.risk").value("LOW"));
    }

    @Test
    void highRiskIsRejectedAutomatically() throws Exception {
        postJson(loanWithDocumentsChecked() + "/score", BAD_PROFILE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.risk").value("HIGH"));
    }

    @Test
    void cannotScoreBeforeTheDocumentCheck() throws Exception {
        postJson(createLoan("5000") + "/score", GOOD_PROFILE).andExpect(status().isConflict());
    }

    @Test
    void invalidProfileIsABadRequest() throws Exception {
        postJson(loanWithDocumentsChecked() + "/score",
                "{\"monthlyIncome\":0,\"monthlyDebt\":0,\"yearsEmployed\":1,\"missedPayments\":0}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownLoanIsNotFound() throws Exception {
        postJson("/loans/does-not-exist/score", GOOD_PROFILE).andExpect(status().isNotFound());
    }
}
