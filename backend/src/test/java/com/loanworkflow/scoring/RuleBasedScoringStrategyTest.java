package com.loanworkflow.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class RuleBasedScoringStrategyTest {

    private final ScoringStrategy strategy = new RuleBasedScoringStrategy();

    private static CreditProfile profile(String income, String debt, int years, int missed) {
        return new CreditProfile(new BigDecimal(income), new BigDecimal(debt), years, missed);
    }

    @Test
    void strongApplicantIsLowRisk() {
        // dti 10% (+150), 5 years (+50), no misses -> 700
        ScoreResult r = strategy.score(profile("5000", "500", 5, 0));
        assertEquals(700, r.score());
        assertEquals(RiskLevel.LOW, r.risk());
    }

    @Test
    void averageApplicantIsMediumRisk() {
        // dti 30% (+50), 2 years (+20), 0 misses -> 570
        ScoreResult r = strategy.score(profile("5000", "1500", 2, 0));
        assertEquals(570, r.score());
        assertEquals(RiskLevel.MEDIUM, r.risk());
    }

    @Test
    void heavyDebtAndMissedPaymentsIsHighRisk() {
        // dti 60% (-100), 1 year (+10), 2 misses (-150) -> 260 -> clamped to 300
        ScoreResult r = strategy.score(profile("5000", "3000", 1, 2));
        assertEquals(300, r.score());
        assertEquals(RiskLevel.HIGH, r.risk());
    }

    @Test
    void employmentBonusIsCappedAtTenYears() {
        int ten = strategy.score(profile("5000", "500", 10, 0)).score();
        int thirty = strategy.score(profile("5000", "500", 30, 0)).score();
        assertEquals(ten, thirty);
    }

    @Test
    void scoreNeverExceedsMaximum() {
        assertEquals(750, strategy.score(profile("10000", "0", 10, 0)).score());
    }

    @Test
    void debtRatioBoundariesAreExclusive() {
        // exactly 20% falls into the middle band, exactly 40% into the worst band
        assertEquals(550, strategy.score(profile("1000", "200", 0, 0)).score());
        assertEquals(400, strategy.score(profile("1000", "400", 0, 0)).score());
    }

    @Test
    void invalidProfilesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> profile("0", "0", 1, 0));
        assertThrows(IllegalArgumentException.class, () -> profile("1000", "-1", 1, 0));
        assertThrows(IllegalArgumentException.class, () -> profile("1000", "0", -1, 0));
        assertThrows(IllegalArgumentException.class, () -> profile("1000", "0", 1, -1));
    }
}
