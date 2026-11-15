import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Loan, LoanService, LoanStatus } from '../loan-service';
import { LoanActions } from './loan-actions';

describe('LoanActions', () => {
  let fixture: ComponentFixture<LoanActions>;
  const api = { checkDocuments: vi.fn(), score: vi.fn(), approve: vi.fn(), disburse: vi.fn() };
  const changed = vi.fn();

  async function show(status: LoanStatus, amount = 25000) {
    fixture = TestBed.createComponent(LoanActions);
    fixture.componentRef.setInput('loan', { id: 'abc', status, amount } satisfies Loan);
    fixture.componentInstance.changed.subscribe(changed);
    await fixture.whenStable();
  }

  beforeEach(async () => {
    Object.values(api).forEach((fn) => fn.mockReset());
    changed.mockReset();
    await TestBed.configureTestingModule({
      imports: [LoanActions],
      providers: [{ provide: LoanService, useValue: api }],
    }).compileComponents();
  });

  const page = () => fixture.nativeElement as HTMLElement;

  async function fill(values: string[]) {
    page()
      .querySelectorAll('input')
      .forEach((input, i) => {
        input.value = values[i];
        input.dispatchEvent(new Event('input'));
      });
    page().querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  async function click() {
    page().querySelector<HTMLButtonElement>('button')!.click();
    await fixture.whenStable();
  }

  it('checks the documents of a submitted loan', async () => {
    api.checkDocuments.mockReturnValue(of({ id: 'abc', status: 'DOCUMENTS_CHECKED', amount: 25000 }));
    await show('SUBMITTED');

    await click();

    expect(api.checkDocuments).toHaveBeenCalledWith('abc');
    expect(changed).toHaveBeenCalledWith(expect.objectContaining({ status: 'DOCUMENTS_CHECKED' }));
  });

  it('scores the applicant and shows the score and risk', async () => {
    api.score.mockReturnValue(of({ id: 'abc', status: 'SCORED', score: 720, risk: 'LOW' }));
    await show('DOCUMENTS_CHECKED');

    await fill(['5000', '500', '4', '0']);

    expect(api.score).toHaveBeenCalledWith('abc', {
      monthlyIncome: 5000,
      monthlyDebt: 500,
      yearsEmployed: 4,
      missedPayments: 0,
    });
    expect(page().querySelector('.score-value')?.textContent).toContain('720');
    expect(page().querySelector('.risk')?.textContent).toContain('LOW risk');
    expect(changed).toHaveBeenCalledWith(expect.objectContaining({ status: 'SCORED' }));
  });

  it('does not score with missing figures', async () => {
    await show('DOCUMENTS_CHECKED');

    await fill(['', '', '', '']);

    expect(api.score).not.toHaveBeenCalled();
    expect(page().querySelectorAll('mat-error').length).toBe(4);
  });

  it('asks for a second approver when the first one is not enough', async () => {
    api.approve.mockReturnValue(of({ id: 'abc', status: 'SCORED', amount: 25000 }));
    await show('SCORED');

    await fill(['bob']);

    expect(api.approve).toHaveBeenCalledWith('abc', 'bob');
    expect(page().querySelector('.info')?.textContent).toContain('second, different approver');
    // The cleared field is ready for the next approver, without an error message
    expect(page().querySelector('input')?.value).toBe('');
    expect(page().querySelector('mat-error')).toBeNull();
  });

  it('shows why an approval was refused', async () => {
    api.approve.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: { error: 'bob has already approved this loan' } })),
    );
    await show('SCORED');

    await fill(['bob']);

    expect(page().querySelector('.error')?.textContent).toContain('bob has already approved this loan');
    expect(changed).not.toHaveBeenCalled();
  });

  it('disburses an approved loan', async () => {
    api.disburse.mockReturnValue(of({ id: 'abc', status: 'REPAYING', amount: 25000 }));
    await show('APPROVED');

    await click();

    expect(api.disburse).toHaveBeenCalledWith('abc');
    expect(changed).toHaveBeenCalledWith(expect.objectContaining({ status: 'REPAYING' }));
  });
});
