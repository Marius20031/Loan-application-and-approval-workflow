package com.loanworkflow.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LoanApplicationTest {

    private static Clock at(String isoInstant) {
        return Clock.fixed(Instant.parse(isoInstant), ZoneOffset.UTC);
    }

    private static final List<LoanStatus> HAPPY_PATH = List.of(
            LoanStatus.DOCUMENTS_CHECKED, LoanStatus.SCORED, LoanStatus.APPROVED,
            LoanStatus.DISBURSED, LoanStatus.REPAYING, LoanStatus.CLOSED);

    @Test
    void startsAsSubmitted() {
        assertEquals(LoanStatus.SUBMITTED, new LoanApplication().status());
    }

    @Test
    void walksTheWholeHappyPath() {
        LoanApplication loan = new LoanApplication();
        HAPPY_PATH.forEach(loan::moveTo);

        assertEquals(LoanStatus.CLOSED, loan.status());
        assertEquals(HAPPY_PATH.size(), loan.history().size());
        assertTrue(loan.status().isTerminal());
    }

    @Test
    void cannotSkipSteps() {
        LoanApplication loan = new LoanApplication();
        assertThrows(InvalidTransitionException.class, () -> loan.moveTo(LoanStatus.DISBURSED));
        assertEquals(LoanStatus.SUBMITTED, loan.status());
        assertTrue(loan.history().isEmpty());
    }

    @Test
    void cannotGoBackwards() {
        LoanApplication loan = new LoanApplication();
        loan.moveTo(LoanStatus.DOCUMENTS_CHECKED);
        assertThrows(InvalidTransitionException.class, () -> loan.moveTo(LoanStatus.SUBMITTED));
    }

    @Test
    void canBeRejectedBeforeApproval() {
        LoanApplication loan = new LoanApplication();
        loan.moveTo(LoanStatus.DOCUMENTS_CHECKED);
        loan.moveTo(LoanStatus.SCORED);
        loan.moveTo(LoanStatus.REJECTED);
        assertTrue(loan.status().isTerminal());
    }

    @Test
    void cannotRejectAfterApproval() {
        assertFalse(LoanStatus.APPROVED.canMoveTo(LoanStatus.REJECTED));
        assertFalse(LoanStatus.DISBURSED.canMoveTo(LoanStatus.REJECTED));
    }

    @Test
    void terminalStatesAllowNothing() {
        for (LoanStatus next : LoanStatus.values()) {
            assertFalse(LoanStatus.CLOSED.canMoveTo(next));
            assertFalse(LoanStatus.REJECTED.canMoveTo(next));
        }
    }

    // --- time-dependent behaviour, tested at different days and times ---

    @ParameterizedTest
    @ValueSource(strings = {
            "2026-01-01T00:00:00Z",   // start of year, midnight
            "2026-02-28T23:59:59Z",   // end of February
            "2028-02-29T12:00:00Z",   // leap day
            "2026-10-25T01:30:00Z",   // around a DST change in Europe
            "2026-12-31T23:59:59Z"    // end of year
    })
    void recordsTheTimeOfTheTransition(String iso) {
        LoanApplication loan = new LoanApplication(at(iso));
        loan.moveTo(LoanStatus.DOCUMENTS_CHECKED);

        StatusChange change = loan.history().get(0);
        assertEquals(Instant.parse(iso), change.at());
        assertEquals(LoanStatus.SUBMITTED, change.from());
        assertEquals(LoanStatus.DOCUMENTS_CHECKED, change.to());
    }

    @Test
    void historyIsInOrderAcrossDays() {
        Instant[] times = {
                Instant.parse("2026-11-01T09:00:00Z"),
                Instant.parse("2026-11-01T13:00:00Z"),
                Instant.parse("2026-11-02T18:00:00Z")
        };
        int[] index = {0};
        Clock ticking = new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return times[index[0]++]; }
        };

        LoanApplication loan = new LoanApplication(ticking);
        loan.moveTo(LoanStatus.DOCUMENTS_CHECKED);
        loan.moveTo(LoanStatus.SCORED);
        loan.moveTo(LoanStatus.APPROVED);

        List<StatusChange> h = loan.history();
        assertEquals(times[0], h.get(0).at());
        assertEquals(times[1], h.get(1).at());
        assertEquals(times[2], h.get(2).at());
        assertTrue(h.get(1).at().isAfter(h.get(0).at()));
    }

    @Test
    void historyCannotBeModifiedFromOutside() {
        LoanApplication loan = new LoanApplication();
        assertThrows(UnsupportedOperationException.class, () -> loan.history().clear());
    }
}
