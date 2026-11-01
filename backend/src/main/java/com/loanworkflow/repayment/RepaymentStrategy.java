package com.loanworkflow.repayment;

import java.math.BigDecimal;
import java.util.List;

/** Strategy pattern: each repayment method builds its own schedule. */
public interface RepaymentStrategy {

    List<Installment> schedule(BigDecimal principal, BigDecimal annualRatePercent, int months);
}
