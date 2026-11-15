import { Component, inject } from '@angular/core';
import { AuthService } from '../auth/auth-service';

/** First page after sign-in: a greeting and how a loan moves through the workflow. */
@Component({
  selector: 'app-home',
  styleUrl: './home.css',
  templateUrl: './home.html',
})
export class Home {
  protected readonly user = inject(AuthService).currentUser;

  protected readonly steps = [
    { title: 'Submitted', text: 'The application is registered with an amount.' },
    { title: 'Documents', text: 'An officer checks that the documents are complete.' },
    { title: 'Scoring', text: 'Risk from income, debt and history. High risk is rejected.' },
    { title: 'Approval', text: 'One approver, or two different ones above 10,000.' },
    { title: 'Disbursement', text: 'The money is paid out and repayment starts.' },
  ];
}
