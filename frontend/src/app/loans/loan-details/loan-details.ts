import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { catchError, of, throwError } from 'rxjs';
import { LoanService, LoanStatus, STATUS_LABELS } from '../loan-service';

const STEPS = ['Submitted', 'Documents checked', 'Scored', 'Approved', 'Disbursed'];

// Index of the last step in STEPS that a loan with this status has completed
const REACHED: Record<LoanStatus, number> = {
  SUBMITTED: 0,
  DOCUMENTS_CHECKED: 1,
  SCORED: 2,
  APPROVED: 3,
  DISBURSED: 4,
  REPAYING: 4,
  CLOSED: 4,
  REJECTED: -1,
};

/** One loan: amount, status and progress through the workflow. */
@Component({
  imports: [DecimalPipe, RouterLink, MatButtonModule, MatIconModule],
  selector: 'app-loan-details',
  styleUrl: './loan-details.css',
  templateUrl: './loan-details.html',
})
export class LoanDetails {
  private readonly loans = inject(LoanService);

  /** Loan id from the URL (/loans/:id). */
  readonly id = input.required<string>();

  // null means the backend has no loan with this id
  protected readonly loan = rxResource({
    params: () => this.id(),
    stream: ({ params: id }) =>
      this.loans
        .get(id)
        .pipe(
          catchError((e: HttpErrorResponse) => (e.status === 404 ? of(null) : throwError(() => e))),
        ),
  });
  protected readonly steps = STEPS;
  protected readonly statusLabels = STATUS_LABELS;

  protected stepState(index: number, status: LoanStatus): 'done' | 'next' | 'todo' {
    const reached = REACHED[status];
    if (index <= reached) {
      return 'done';
    }
    return index === reached + 1 ? 'next' : 'todo';
  }
}
