package com.loanworkflow.scoring;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Simple rule-based model. Starts at 500, then adjusts for debt-to-income ratio,
 * job stability and missed payments. The result is kept between 300 and 850.
 */
public class RuleBasedScoringStrategy implements ScoringStrategy {

    static final int MIN_SCORE = 300;
    static final int MAX_SCORE = 850;

    @Override
    public ScoreResult score(CreditProfile profile) {
        int score = 500;

        BigDecimal dti = profile.monthlyDebt().divide(profile.monthlyIncome(), MathContext.DECIMAL64);
        if (dti.compareTo(new BigDecimal("0.20")) < 0) {
            score += 150;
        } else if (dti.compareTo(new BigDecimal("0.40")) < 0) {
            score += 50;
        } else {
            score -= 100;
        }

        score += Math.min(profile.yearsEmployed(), 10) * 10;
        score -= profile.missedPayments() * 75;

        score = Math.max(MIN_SCORE, Math.min(MAX_SCORE, score));
        return new ScoreResult(score, riskFor(score));
    }

    private static RiskLevel riskFor(int score) {
        if (score >= 700) {
            return RiskLevel.LOW;
        }
        if (score >= 550) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.HIGH;
    }
}
