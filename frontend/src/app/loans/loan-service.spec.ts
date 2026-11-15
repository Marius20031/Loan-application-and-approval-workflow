import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { LoanService, errorMessage } from './loan-service';

describe('LoanService', () => {
  let service: LoanService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(LoanService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('creates a loan', () => {
    service.create('alice', 25000).subscribe();

    const req = http.expectOne('/api/loans');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ applicant: 'alice', amount: 25000 });
    req.flush({ id: 'abc', status: 'SUBMITTED', amount: 25000 });
  });

  it('reads a loan by id', () => {
    service.get('abc').subscribe();

    expect(http.expectOne('/api/loans/abc').request.method).toBe('GET');
  });

  it('posts each workflow step to its own endpoint', () => {
    service.checkDocuments('abc').subscribe();
    service.score('abc', { monthlyIncome: 5000, monthlyDebt: 500, yearsEmployed: 4, missedPayments: 0 }).subscribe();
    service.approve('abc', 'bob').subscribe();
    service.disburse('abc').subscribe();

    expect(http.expectOne('/api/loans/abc/documents').request.method).toBe('POST');
    expect(http.expectOne('/api/loans/abc/score').request.body).toEqual({
      monthlyIncome: 5000,
      monthlyDebt: 500,
      yearsEmployed: 4,
      missedPayments: 0,
    });
    expect(http.expectOne('/api/loans/abc/approve').request.body).toEqual({ approver: 'bob' });
    expect(http.expectOne('/api/loans/abc/disburse').request.method).toBe('POST');
  });

  it('asks for the repayment schedule with method, rate and months', () => {
    service.schedule('abc', 'fixed-principal', 12, 24).subscribe();

    const req = http.expectOne((r) => r.url === '/api/loans/abc/schedule');
    expect(req.request.params.get('method')).toBe('fixed-principal');
    expect(req.request.params.get('rate')).toBe('12');
    expect(req.request.params.get('months')).toBe('24');
  });
});

describe('errorMessage', () => {
  it('uses the message sent by the backend', () => {
    const e = new HttpErrorResponse({ status: 409, error: { error: 'Cannot move loan from SUBMITTED to APPROVED' } });
    expect(errorMessage(e)).toBe('Cannot move loan from SUBMITTED to APPROVED');
  });

  it('explains when the server cannot be reached', () => {
    expect(errorMessage(new HttpErrorResponse({ status: 0 }))).toContain('Could not reach the server');
  });

  it('falls back to a generic message', () => {
    expect(errorMessage(new HttpErrorResponse({ status: 500 }))).toContain('Something went wrong');
  });
});
