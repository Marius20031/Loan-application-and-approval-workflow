import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type LoanStatus =
  | 'SUBMITTED'
  | 'DOCUMENTS_CHECKED'
  | 'SCORED'
  | 'APPROVED'
  | 'DISBURSED'
  | 'REPAYING'
  | 'CLOSED'
  | 'REJECTED';

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type RepaymentMethod = 'annuity' | 'fixed-principal';

export interface Loan {
  id: string;
  status: LoanStatus;
  amount: number;
}

export interface CreditProfile {
  monthlyIncome: number;
  monthlyDebt: number;
  yearsEmployed: number;
  missedPayments: number;
}

export interface ScoreResult {
  id: string;
  status: LoanStatus;
  score: number;
  risk: RiskLevel;
}

export interface Installment {
  number: number;
  payment: number;
  principal: number;
  interest: number;
  remainingBalance: number;
}

export const STATUS_LABELS: Record<LoanStatus, string> = {
  SUBMITTED: 'Submitted',
  DOCUMENTS_CHECKED: 'Documents checked',
  SCORED: 'Scored',
  APPROVED: 'Approved',
  DISBURSED: 'Disbursed',
  REPAYING: 'Repaying',
  CLOSED: 'Closed',
  REJECTED: 'Rejected',
};

/** The backend answers 400/409 with `{ error: "..." }`; show that text when there is one. */
export function errorMessage(e: HttpErrorResponse): string {
  if (e.status === 0) {
    return 'Could not reach the server. Please try again.';
  }
  return e.error?.error ?? 'Something went wrong. Please try again.';
}

/** Calls the loan endpoints of the backend, one method per step of the workflow. */
@Service()
export class LoanService {
  private readonly http = inject(HttpClient);

  create(applicant: string, amount: number): Observable<Loan> {
    return this.http.post<Loan>('/api/loans', { applicant, amount });
  }

  get(id: string): Observable<Loan> {
    return this.http.get<Loan>(this.url(id));
  }

  checkDocuments(id: string): Observable<Loan> {
    return this.http.post<Loan>(this.url(id, 'documents'), {});
  }

  score(id: string, profile: CreditProfile): Observable<ScoreResult> {
    return this.http.post<ScoreResult>(this.url(id, 'score'), profile);
  }

  approve(id: string, approver: string): Observable<Loan> {
    return this.http.post<Loan>(this.url(id, 'approve'), { approver });
  }

  disburse(id: string): Observable<Loan> {
    return this.http.post<Loan>(this.url(id, 'disburse'), {});
  }

  schedule(id: string, method: RepaymentMethod, rate: number, months: number): Observable<Installment[]> {
    return this.http.get<Installment[]>(this.url(id, 'schedule'), {
      params: { method, rate, months },
    });
  }

  private url(id: string, action?: string): string {
    const base = `/api/loans/${encodeURIComponent(id)}`;
    return action ? `${base}/${action}` : base;
  }
}
