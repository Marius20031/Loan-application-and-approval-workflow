package com.loanworkflow.loan;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

abstract class LoanDocumentsTest extends LoanApiTestBase {

    @Test
    void documentCheckMovesTheLoanForward() throws Exception {
        String loan = createLoan("5000");

        postEmpty(loan + "/documents")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOCUMENTS_CHECKED"));
    }

    @Test
    void theNewStatusIsKept() throws Exception {
        String loan = createLoan("5000");
        postEmpty(loan + "/documents");

        mvc.perform(get(loan)).andExpect(jsonPath("$.status").value("DOCUMENTS_CHECKED"));
    }

    @Test
    void unknownLoanIsNotFound() throws Exception {
        postEmpty("/loans/does-not-exist/documents").andExpect(status().isNotFound());
    }

    @Test
    void checkingTwiceIsAConflict() throws Exception {
        String loan = createLoan("5000");
        postEmpty(loan + "/documents");

        postEmpty(loan + "/documents")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }
}
