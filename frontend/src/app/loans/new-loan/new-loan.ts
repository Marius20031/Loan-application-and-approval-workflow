import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../auth/auth-service';
import { Loan, LoanService, STATUS_LABELS, errorMessage } from '../loan-service';

@Component({
  imports: [
    DecimalPipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  selector: 'app-new-loan',
  styleUrl: './new-loan.css',
  templateUrl: './new-loan.html',
})
export class NewLoan {
  private readonly loans = inject(LoanService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly form = this.fb.group({
    applicant: [inject(AuthService).currentUser() ?? '', Validators.required],
    amount: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
  });
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly created = signal<Loan | null>(null);
  protected readonly statusLabels = STATUS_LABELS;

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { applicant, amount } = this.form.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.loans
      .create(applicant.trim(), amount!)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (loan) => this.created.set(loan),
        error: (e: HttpErrorResponse) => this.error.set(errorMessage(e)),
      });
  }

  protected startOver(): void {
    this.created.set(null);
    this.form.controls.amount.reset();
  }
}
