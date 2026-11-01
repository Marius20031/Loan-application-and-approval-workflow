package com.loanworkflow.loan;

/** Observer: gets called after every successful status change. */
@FunctionalInterface
public interface StatusListener {

    void onStatusChange(StatusChange change);
}
