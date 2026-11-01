package com.loanworkflow.repayment;

import java.math.BigDecimal;

/** One row of a repayment schedule. */
public record Installment(int number, BigDecimal payment, BigDecimal principal,
                          BigDecimal interest, BigDecimal remainingBalance) {
}
