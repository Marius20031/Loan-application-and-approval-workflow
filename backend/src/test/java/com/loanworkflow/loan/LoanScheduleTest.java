package com.loanworkflow.loan;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

abstract class LoanScheduleTest extends LoanApiTestBase {

    // Loans over 10,000 need two different approvers, so both sign here
    private String approvedLoan(String amount) throws Exception {
        String loan = createLoan(amount);
        postEmpty(loan + "/documents");
        postJson(loan + "/score", GOOD_PROFILE);
        postJson(loan + "/approve", "{\"approver\":\"anna\"}");
        postJson(loan + "/approve", "{\"approver\":\"ben\"}");
        return loan;
    }

    @Test
    void annuityScheduleHasEqualPayments() throws Exception {
        mvc.perform(get(approvedLoan("12000") + "/schedule?rate=12&months=12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].payment").value(1066.19))
                .andExpect(jsonPath("$[5].payment").value(1066.19))
                .andExpect(jsonPath("$[11].remainingBalance").value(0));
    }

    @Test
    void fixedPrincipalKeepsPrincipalConstant() throws Exception {
        mvc.perform(get(approvedLoan("12000") + "/schedule?method=fixed-principal&rate=12&months=12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].principal").value(1000.0))
                .andExpect(jsonPath("$[0].interest").value(120.0))
                .andExpect(jsonPath("$[11].remainingBalance").value(0));
    }

    @Test
    void scheduleIsNotAvailableBeforeApproval() throws Exception {
        mvc.perform(get(createLoan("12000") + "/schedule?rate=12&months=12"))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownMethodIsABadRequest() throws Exception {
        mvc.perform(get(approvedLoan("12000") + "/schedule?method=magic&rate=12&months=12"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void badNumbersAreABadRequest() throws Exception {
        String loan = approvedLoan("12000");
        mvc.perform(get(loan + "/schedule?rate=-1&months=12")).andExpect(status().isBadRequest());
        mvc.perform(get(loan + "/schedule?rate=12&months=0")).andExpect(status().isBadRequest());
    }

    @Test
    void unknownLoanIsNotFound() throws Exception {
        mvc.perform(get("/loans/does-not-exist/schedule?rate=12&months=12")).andExpect(status().isNotFound());
    }
}
