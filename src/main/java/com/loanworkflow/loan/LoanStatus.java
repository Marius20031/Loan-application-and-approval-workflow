package com.loanworkflow.loan;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a loan. Each state knows which states it may move to,
 * so invalid jumps (e.g. SUBMITTED -> DISBURSED) are rejected in one place.
 */
public enum LoanStatus {
    SUBMITTED,
    DOCUMENTS_CHECKED,
    SCORED,
    APPROVED,
    DISBURSED,
    REPAYING,
    CLOSED,
    REJECTED;

    private Set<LoanStatus> allowedNext() {
        return switch (this) {
            case SUBMITTED -> EnumSet.of(DOCUMENTS_CHECKED, REJECTED);
            case DOCUMENTS_CHECKED -> EnumSet.of(SCORED, REJECTED);
            case SCORED -> EnumSet.of(APPROVED, REJECTED);
            case APPROVED -> EnumSet.of(DISBURSED);
            case DISBURSED -> EnumSet.of(REPAYING);
            case REPAYING -> EnumSet.of(CLOSED);
            case CLOSED, REJECTED -> EnumSet.noneOf(LoanStatus.class);
        };
    }

    public boolean canMoveTo(LoanStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isTerminal() {
        return allowedNext().isEmpty();
    }
}
