package com.loanworkflow.loan;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(LoanController.class)
@Import({InMemoryLoanRepository.class, PatternsLoanEngine.class, ApiExceptionHandler.class})
class LoanControllerTest {

    @Autowired
    private MockMvc mvc;

    private ResultActions create(String json) throws Exception {
        return mvc.perform(post("/loans").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String createAndGetLocation(String json) throws Exception {
        return create(json).andReturn().getResponse().getHeader("Location");
    }

    @Test
    void createsLoanAsSubmitted() throws Exception {
        create("{\"applicant\":\"carol\",\"amount\":5000}")
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.amount").value(5000));
    }

    @Test
    void rejectsZeroAmount() throws Exception {
        create("{\"applicant\":\"carol\",\"amount\":0}").andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMissingApplicant() throws Exception {
        create("{\"amount\":5000}").andExpect(status().isBadRequest());
    }

    @Test
    void createdLoanCanBeFetchedWithTheLocationHeader() throws Exception {
        String location = createAndGetLocation("{\"applicant\":\"carol\",\"amount\":5000}");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.amount").value(5000));
    }

    @Test
    void twoLoansGetDifferentLocations() throws Exception {
        assertNotEquals(
                createAndGetLocation("{\"applicant\":\"carol\",\"amount\":5000}"),
                createAndGetLocation("{\"applicant\":\"carol\",\"amount\":5000}"));
    }

    @Test
    void unknownLoanIsNotFound() throws Exception {
        mvc.perform(get("/loans/does-not-exist")).andExpect(status().isNotFound());
    }
}
