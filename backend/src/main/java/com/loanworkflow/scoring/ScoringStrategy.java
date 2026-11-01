package com.loanworkflow.scoring;

/** Strategy pattern: different scoring models can be swapped in without changing callers. */
public interface ScoringStrategy {

    ScoreResult score(CreditProfile profile);
}
