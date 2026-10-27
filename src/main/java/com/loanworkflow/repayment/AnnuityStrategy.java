package com.loanworkflow.repayment;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Equal monthly payments; the interest share shrinks and the principal share grows. */
public class AnnuityStrategy implements RepaymentStrategy {

    private static final MathContext MC = MathContext.DECIMAL64;

    @Override
    public List<Installment> schedule(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        BigDecimal monthlyRate = annualRatePercent.divide(BigDecimal.valueOf(1200), MC);
        BigDecimal payment = payment(principal, monthlyRate, months);

        List<Installment> result = new ArrayList<>();
        BigDecimal balance = principal;
        for (int i = 1; i <= months; i++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPart = payment.subtract(interest);
            if (i == months) {
                // absorb rounding drift so the loan ends at exactly zero
                principalPart = balance;
            }
            balance = balance.subtract(principalPart);
            result.add(new Installment(i, principalPart.add(interest), principalPart, interest, balance));
        }
        return result;
    }

    private BigDecimal payment(BigDecimal principal, BigDecimal monthlyRate, int months) {
        if (monthlyRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }
        // P * r / (1 - (1 + r)^-n)
        BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(months, MC);
        BigDecimal denominator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(factor, MC));
        return principal.multiply(monthlyRate).divide(denominator, MC).setScale(2, RoundingMode.HALF_UP);
    }
}
