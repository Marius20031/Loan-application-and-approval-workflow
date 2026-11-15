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
    private final List<StatusListener> listeners = new ArrayList<>();

    public LoanApplication(Clock clock, LoanStatus initialStatus, List<StatusChange> initialHistory) {
        this.clock = clock;
        this.status = initialStatus != null ? initialStatus : LoanStatus.SUBMITTED;
        if (initialHistory != null) {
            this.history.addAll(initialHistory);
        }
    }

    public LoanApplication(Clock clock, LoanStatus initialStatus) {
        this(clock, initialStatus, Collections.emptyList());
    }

    public LoanApplication(Clock clock) {
        this(clock, LoanStatus.SUBMITTED, Collections.emptyList());
    }

    public LoanApplication() {
        this(Clock.systemUTC());
    }

    public Clock clock() {
        return clock;
    }

    public LoanStatus status() {
        return status;
    }

    public List<StatusChange> history() {
        return Collections.unmodifiableList(history);
    }

    public void restoreStatusChange(StatusChange change) {
        if (change != null) {
            history.add(change);
        }
    }

    // Observers are told about every change (audit, notifications, ...)
    public void addListener(StatusListener listener) {
        listeners.add(listener);
    }

    public void moveTo(LoanStatus next) {
        if (!status.canMoveTo(next)) {
            throw new InvalidTransitionException(status, next);
        }
        StatusChange change = new StatusChange(status, next, clock.instant());
        history.add(change);
        status = next;
        // notify only after the state is updated, so listeners see the new status
        listeners.forEach(l -> l.onStatusChange(change));
    }
}
