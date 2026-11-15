package com.loanworkflow.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.loanworkflow.scoring.CreditProfile;

class CreditProfileBuilderTest {

    @Test
    void buildsDefaultCreditProfile() {
        CreditProfile profile = CreditProfileBuilder.aCreditProfile().build();

        assertEquals(new BigDecimal("5000"), profile.monthlyIncome());
        assertEquals(new BigDecimal("500"), profile.monthlyDebt());
        assertEquals(5, profile.yearsEmployed());
        assertEquals(0, profile.missedPayments());
    }

    @Test
    void buildsCustomizedCreditProfile() {
        CreditProfile profile = CreditProfileBuilder.aCreditProfile()
                .withMonthlyIncome("9500.50")
                .withMonthlyDebt("1200.00")
                .withYearsEmployed(10)
                .withMissedPayments(2)
                .build();

        assertEquals(new BigDecimal("9500.50"), profile.monthlyIncome());
        assertEquals(new BigDecimal("1200.00"), profile.monthlyDebt());
        assertEquals(10, profile.yearsEmployed());
        assertEquals(2, profile.missedPayments());
    }

    @Test
    void rejectsNegativeIncomeThroughCreditProfileValidation() {
        CreditProfileBuilder builder = CreditProfileBuilder.aCreditProfile()
                .withMonthlyIncome("-100");

        assertThrows(IllegalArgumentException.class, builder::build);
    }
}
