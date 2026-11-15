import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Loan, LoanService } from '../loan-service';
import { LoanDetails } from './loan-details';

describe('LoanDetails', () => {
  let fixture: ComponentFixture<LoanDetails>;
  const get = vi.fn();

  async function open(id: string) {
    fixture = TestBed.createComponent(LoanDetails);
    fixture.componentRef.setInput('id', id);
    await fixture.whenStable();
  }

  beforeEach(async () => {
    get.mockReset();
    await TestBed.configureTestingModule({
      imports: [LoanDetails],
      providers: [provideRouter([]), { provide: LoanService, useValue: { get } }],
    }).compileComponents();
  });

  const page = () => fixture.nativeElement as HTMLElement;
  const stepClasses = () => Array.from(page().querySelectorAll('.steps li')).map((li) => li.className);

  it('shows the amount, the status and how far the loan got', async () => {
    get.mockReturnValue(of<Loan>({ id: 'abc', status: 'DOCUMENTS_CHECKED', amount: 25000 }));

    await open('abc');

    expect(get).toHaveBeenCalledWith('abc');
    expect(page().querySelector('h1')?.textContent).toContain('25,000');
    expect(page().querySelector('.status')?.textContent).toContain('Documents checked');
    expect(stepClasses()).toEqual(['done', 'done', 'next', 'todo', 'todo']);
  });

  it('marks every step done once the loan is repaying', async () => {
    get.mockReturnValue(of<Loan>({ id: 'abc', status: 'REPAYING', amount: 5000 }));

    await open('abc');

    expect(stepClasses()).toEqual(['done', 'done', 'done', 'done', 'done']);
  });

  it('explains when a loan was rejected', async () => {
    get.mockReturnValue(of<Loan>({ id: 'abc', status: 'REJECTED', amount: 5000 }));

    await open('abc');

    expect(page().querySelector('.rejected')?.textContent).toContain('rejected');
    expect(page().querySelector('.steps')).toBeNull();
  });

  it('says so when no loan has this reference', async () => {
    get.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));

    await open('missing');

    expect(page().querySelector('.not-found h1')?.textContent).toContain('Loan not found');
  });
});
