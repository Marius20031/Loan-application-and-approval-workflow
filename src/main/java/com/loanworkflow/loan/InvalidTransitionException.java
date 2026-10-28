package com.loanworkflow.loan;

public class InvalidTransitionException extends RuntimeException {

    public InvalidTransitionException(LoanStatus from, LoanStatus to) {
        super("Cannot move loan from " + from + " to " + to);
    }
}
