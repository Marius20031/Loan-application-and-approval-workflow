import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../auth/auth-service';
import { LoanService } from '../loan-service';
import { NewLoan } from './new-loan';

describe('NewLoan', () => {
  let fixture: ComponentFixture<NewLoan>;
  const create = vi.fn();

  beforeEach(async () => {
    create.mockReset();
    await TestBed.configureTestingModule({
      imports: [NewLoan],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { currentUser: signal('alice') } },
        { provide: LoanService, useValue: { create } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NewLoan);
    await fixture.whenStable();
  });

  const page = () => fixture.nativeElement as HTMLElement;
  const inputs = () => Array.from(page().querySelectorAll('input'));

  async function submit(amount: string) {
    const amountInput = inputs()[1];
    amountInput.value = amount;
    amountInput.dispatchEvent(new Event('input'));
    page().querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('fills in the signed-in user as applicant', () => {
    expect(inputs()[0].value).toBe('alice');
  });

  it('does not submit without a positive amount', async () => {
    await submit('0');

    expect(create).not.toHaveBeenCalled();
    expect(page().querySelector('mat-error')?.textContent).toContain('greater than 0');
  });

  it('submits the application and shows its reference', async () => {
    create.mockReturnValue(of({ id: 'abc-123', status: 'SUBMITTED', amount: 25000 }));

    await submit('25000');

    expect(create).toHaveBeenCalledWith('alice', 25000);
    expect(page().querySelector('.reference')?.textContent).toContain('abc-123');
    expect(page().querySelector('.status')?.textContent).toContain('Submitted');
  });

  it('shows the error sent by the backend', async () => {
    create.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: { error: 'applicant and positive amount required' } })),
    );

    await submit('500');

    expect(page().querySelector('.error')?.textContent).toContain('applicant and positive amount required');
  });
});
