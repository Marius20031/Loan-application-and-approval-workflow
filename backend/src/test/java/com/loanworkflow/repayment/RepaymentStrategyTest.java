package com.loanworkflow.repayment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class RepaymentStrategyTest {

    private static final BigDecimal PRINCIPAL = new BigDecimal("12000.00");
    private static final BigDecimal RATE = new BigDecimal("12");

    @Test
    void annuityPaymentsAreEqualAndBalanceEndsAtZero() {
        List<Installment> s = new AnnuityStrategy().schedule(PRINCIPAL, RATE, 12);

        assertEquals(12, s.size());
        // known value: 12000 at 12% over 12 months -> 1066.19 per month
        assertEquals(new BigDecimal("1066.19"), s.get(0).payment());
        assertEquals(new BigDecimal("1066.19"), s.get(5).payment());
        assertEquals(0, s.get(11).remainingBalance().signum());
    }

    @Test
    void annuityWithZeroRateSplitsEvenly() {
        List<Installment> s = new AnnuityStrategy().schedule(PRINCIPAL, BigDecimal.ZERO, 12);

        assertEquals(new BigDecimal("1000.00"), s.get(0).payment());
        assertEquals(0, s.get(11).remainingBalance().signum());
    }

    @Test
    void fixedPrincipalKeepsPrincipalConstantAndPaymentsDecrease() {
        List<Installment> s = new FixedPrincipalStrategy().schedule(PRINCIPAL, RATE, 12);

        assertEquals(new BigDecimal("1000.00"), s.get(0).principal());
        assertEquals(new BigDecimal("1000.00"), s.get(5).principal());
        // first month interest = 12000 * 1% = 120.00
        assertEquals(new BigDecimal("120.00"), s.get(0).interest());
        assertTrue(s.get(0).payment().compareTo(s.get(11).payment()) > 0);
        assertEquals(0, s.get(11).remainingBalance().signum());
    }

    @Test
    void totalPrincipalRepaidEqualsLoanAmount() {
        for (RepaymentStrategy strategy : List.of(new AnnuityStrategy(), new FixedPrincipalStrategy())) {
            BigDecimal total = strategy.schedule(PRINCIPAL, RATE, 36).stream()
                    .map(Installment::principal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, PRINCIPAL.compareTo(total));
        }
    }
}
