package com.loanworkflow.repayment;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Equal principal every month; payments start high and decrease as interest falls. */
public class FixedPrincipalStrategy implements RepaymentStrategy {

    @Override
    public List<Installment> schedule(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        BigDecimal monthlyRate = annualRatePercent.divide(BigDecimal.valueOf(1200), MathContext.DECIMAL64);
        BigDecimal principalPart = principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);

        List<Installment> result = new ArrayList<>();
        BigDecimal balance = principal;
        for (int i = 1; i <= months; i++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal part = (i == months) ? balance : principalPart;
            balance = balance.subtract(part);
            result.add(new Installment(i, part.add(interest), part, interest, balance));
        }
        return result;
    }
}
