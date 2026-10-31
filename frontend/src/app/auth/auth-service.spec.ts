import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService, LoginResponse } from './auth-service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('posts the credentials to /api/login', () => {
    let response: LoginResponse | undefined;
    service.login('alice', 'password123').subscribe((r) => (response = r));

    const req = http.expectOne('/api/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'alice', password: 'password123' });
    req.flush({ message: 'user connected' });

    expect(response).toEqual({ message: 'user connected' });
  });

  it('reports wrong credentials as a 401 error', () => {
    let status: number | undefined;
    service.login('alice', 'wrong').subscribe({
      error: (e: HttpErrorResponse) => (status = e.status),
    });

    http
      .expectOne('/api/login')
      .flush({ error: 'invalid credentials' }, { status: 401, statusText: 'Unauthorized' });

    expect(status).toBe(401);
  });
});
