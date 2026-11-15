package com.loanworkflow.fixtures;

import java.math.BigDecimal;

import com.loanworkflow.scoring.CreditProfile;

/**
 * Fluent builder for creating {@link CreditProfile} test fixtures.
 */
public class CreditProfileBuilder {

    private BigDecimal monthlyIncome = new BigDecimal("5000");
    private BigDecimal monthlyDebt = new BigDecimal("500");
    private int yearsEmployed = 5;
    private int missedPayments = 0;

    public static CreditProfileBuilder aCreditProfile() {
        return new CreditProfileBuilder();
    }

    public CreditProfileBuilder withMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
        return this;
    }

    public CreditProfileBuilder withMonthlyIncome(String monthlyIncome) {
        this.monthlyIncome = new BigDecimal(monthlyIncome);
        return this;
    }

    public CreditProfileBuilder withMonthlyDebt(BigDecimal monthlyDebt) {
        this.monthlyDebt = monthlyDebt;
        return this;
    }

    public CreditProfileBuilder withMonthlyDebt(String monthlyDebt) {
        this.monthlyDebt = new BigDecimal(monthlyDebt);
        return this;
    }

    public CreditProfileBuilder withYearsEmployed(int yearsEmployed) {
        this.yearsEmployed = yearsEmployed;
        return this;
    }

    public CreditProfileBuilder withMissedPayments(int missedPayments) {
        this.missedPayments = missedPayments;
        return this;
    }

    public CreditProfile build() {
        return new CreditProfile(monthlyIncome, monthlyDebt, yearsEmployed, missedPayments);
    }
}
