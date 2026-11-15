import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, output, signal } from '@angular/core';
import {
  FormGroupDirective,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Observable, finalize } from 'rxjs';
import { CreditProfile, Loan, LoanService, ScoreResult, errorMessage } from '../loan-service';

/** The action for the loan's current step: check documents, score, approve or disburse. */
@Component({
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  selector: 'app-loan-actions',
  styleUrl: './loan-actions.css',
  templateUrl: './loan-actions.html',
})
export class LoanActions {
  private readonly loans = inject(LoanService);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly loan = input.required<Loan>();
  /** Emits the loan after every step, so the page can show the new status. */
  readonly changed = output<Loan>();

  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly scoreResult = signal<ScoreResult | null>(null);
  protected readonly needsSecondApprover = signal(false);

  protected readonly scoreForm = this.fb.group({
    monthlyIncome: this.amount([Validators.required, Validators.min(1)]),
    monthlyDebt: this.amount([Validators.required, Validators.min(0)]),
    yearsEmployed: this.amount([Validators.required, Validators.min(0)]),
    missedPayments: this.amount([Validators.required, Validators.min(0)]),
  });
  protected readonly approveForm = this.fb.group({ approver: ['', Validators.required] });

  protected checkDocuments(): void {
    this.run(this.loans.checkDocuments(this.loan().id), (loan) => this.changed.emit(loan));
  }

  protected score(): void {
    if (this.scoreForm.invalid) {
      this.scoreForm.markAllAsTouched();
      return;
    }
    const profile = this.scoreForm.getRawValue() as CreditProfile;
    this.run(this.loans.score(this.loan().id, profile), (result) => {
      this.scoreResult.set(result);
      this.changed.emit({ ...this.loan(), status: result.status });
    });
  }

  protected approve(form: FormGroupDirective): void {
    if (this.approveForm.invalid) {
      this.approveForm.markAllAsTouched();
      return;
    }
    const { approver } = this.approveForm.getRawValue();
    this.run(this.loans.approve(this.loan().id, approver.trim()), (loan) => {
      // Above 10,000 the first approval keeps the loan SCORED until a second approver signs
      this.needsSecondApprover.set(loan.status === 'SCORED');
      // resetForm also clears the "submitted" state, so the empty field is not shown as an error
      form.resetForm();
      this.changed.emit(loan);
    });
  }

  protected disburse(): void {
    this.run(this.loans.disburse(this.loan().id), (loan) => this.changed.emit(loan));
  }

  private run<T>(request: Observable<T>, done: (value: T) => void): void {
    this.busy.set(true);
    this.error.set(null);
    request.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: done,
      error: (e: HttpErrorResponse) => this.error.set(errorMessage(e)),
    });
  }

  private amount(validators: ValidatorFn[]) {
    return this.fb.control<number | null>(null, validators);
  }
}
