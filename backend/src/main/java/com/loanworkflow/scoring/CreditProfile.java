package com.loanworkflow.scoring;

import java.math.BigDecimal;

/** The financial facts about an applicant that scoring is based on. */
public record CreditProfile(BigDecimal monthlyIncome, BigDecimal monthlyDebt,
                            int yearsEmployed, int missedPayments) {

    public CreditProfile {
        if (monthlyIncome == null || monthlyIncome.signum() <= 0) {
            throw new IllegalArgumentException("monthlyIncome must be positive");
        }
        if (monthlyDebt == null || monthlyDebt.signum() < 0) {
            throw new IllegalArgumentException("monthlyDebt must not be negative");
        }
        if (yearsEmployed < 0 || missedPayments < 0) {
            throw new IllegalArgumentException("yearsEmployed and missedPayments must not be negative");
        }
    }
}
