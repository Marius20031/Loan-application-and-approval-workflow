package com.loanworkflow.loan;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A loan application moving through its lifecycle. The Clock is injected so
 * transition timestamps can be tested at any date and time.
 */
public class LoanApplication {

    private final Clock clock;
    private LoanStatus status = LoanStatus.SUBMITTED;
    private final List<StatusChange> history = new ArrayList<>();

    public LoanApplication(Clock clock) {
        this.clock = clock;
    }

    public LoanApplication() {
        this(Clock.systemUTC());
    }

    public LoanStatus status() {
        return status;
    }

    public List<StatusChange> history() {
        return Collections.unmodifiableList(history);
    }

    public void moveTo(LoanStatus next) {
        if (!status.canMoveTo(next)) {
            throw new InvalidTransitionException(status, next);
        }
        history.add(new StatusChange(status, next, clock.instant()));
        status = next;
    }
}
