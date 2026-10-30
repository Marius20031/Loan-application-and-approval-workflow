package com.loanworkflow.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class StatusListenerTest {

    @Test
    void listenerIsCalledForEachTransition() {
        LoanApplication loan = new LoanApplication();
        List<StatusChange> seen = new ArrayList<>();
        loan.addListener(seen::add);

        loan.moveTo(LoanStatus.DOCUMENTS_CHECKED);
        loan.moveTo(LoanStatus.SCORED);

        assertEquals(2, seen.size());
        assertEquals(LoanStatus.SCORED, seen.get(1).to());
    }

    @Test
    void listenerSeesTheNewStatus() {
        LoanApplication loan = new LoanApplication();
        List<LoanStatus> statusInsideCallback = new ArrayList<>();
        loan.addListener(c -> statusInsideCallback.add(loan.status()));

        loan.moveTo(LoanStatus.DOCUMENTS_CHECKED);

        assertEquals(List.of(LoanStatus.DOCUMENTS_CHECKED), statusInsideCallback);
    }

    @Test
    void allListenersAreNotified() {
        LoanApplication loan = new LoanApplication();
        int[] counts = {0, 0};
        loan.addListener(c -> counts[0]++);
        loan.addListener(c -> counts[1]++);

        loan.moveTo(LoanStatus.DOCUMENTS_CHECKED);

        assertEquals(1, counts[0]);
        assertEquals(1, counts[1]);
    }

    @Test
    void invalidTransitionDoesNotNotify() {
        LoanApplication loan = new LoanApplication();
        List<StatusChange> seen = new ArrayList<>();
        loan.addListener(seen::add);

        assertThrows(InvalidTransitionException.class, () -> loan.moveTo(LoanStatus.DISBURSED));

        assertTrue(seen.isEmpty());
    }
}
