package com.loanworkflow.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(AuthService.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    private org.springframework.test.web.servlet.ResultActions login(String json) throws Exception {
        return mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void connected() throws Exception {
        login("{\"username\":\"alice\",\"password\":\"password123\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("user connected"));
    }

    @Test
    void rejected() throws Exception {
        login("{\"username\":\"alice\",\"password\":\"bad\"}")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingFields() throws Exception {
        login("{\"username\":\"alice\"}")
                .andExpect(status().isBadRequest());
    }
}
