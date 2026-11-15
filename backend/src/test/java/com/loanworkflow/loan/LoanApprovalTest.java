package com.loanworkflow.loan;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

abstract class LoanApprovalTest extends LoanApiTestBase {

    private String scoredLoan(String amount) throws Exception {
        String loan = createLoan(amount);
        postEmpty(loan + "/documents");
        postJson(loan + "/score", GOOD_PROFILE);
        return loan;
    }

    private static String approver(String name) {
        return "{\"approver\":\"" + name + "\"}";
    }

    @Test
    void smallLoanIsApprovedByOneApprover() throws Exception {
        postJson(scoredLoan("5000") + "/approve", approver("anna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void largeLoanWaitsForASecondApprover() throws Exception {
        String loan = scoredLoan("50000");

        postJson(loan + "/approve", approver("anna"))
                .andExpect(jsonPath("$.status").value("SCORED"));
        postJson(loan + "/approve", approver("ben"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void sameApproverCannotApproveTwice() throws Exception {
        String loan = scoredLoan("50000");
        postJson(loan + "/approve", approver("anna"));

        postJson(loan + "/approve", approver("anna")).andExpect(status().isConflict());
    }

    @Test
    void applicantCannotApproveTheirOwnLoan() throws Exception {
        postJson(scoredLoan("5000") + "/approve", approver("carol")).andExpect(status().isConflict());
    }

    @Test
    void cannotApproveBeforeScoring() throws Exception {
        postJson(createLoan("5000") + "/approve", approver("anna")).andExpect(status().isConflict());
    }

    @Test
    void missingApproverIsABadRequest() throws Exception {
        postJson(scoredLoan("5000") + "/approve", "{}").andExpect(status().isBadRequest());
    }

    @Test
    void approvedLoanCanBeDisbursed() throws Exception {
        String loan = scoredLoan("5000");
        postJson(loan + "/approve", approver("anna"));

        postEmpty(loan + "/disburse")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REPAYING"));
        mvc.perform(get(loan)).andExpect(jsonPath("$.status").value("REPAYING"));
    }

    @Test
    void cannotDisburseBeforeApproval() throws Exception {
        postEmpty(scoredLoan("5000") + "/disburse").andExpect(status().isConflict());
    }
}
