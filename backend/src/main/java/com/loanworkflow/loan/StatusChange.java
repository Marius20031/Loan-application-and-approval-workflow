package com.loanworkflow.loan;

import java.time.Instant;

/** One entry in a loan's audit trail. */
public record StatusChange(LoanStatus from, LoanStatus to, Instant at) {
}
