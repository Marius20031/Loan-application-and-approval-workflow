import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AuthService } from '../auth/auth-service';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  const login = vi.fn();

  beforeEach(async () => {
    login.mockReset();
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [{ provide: AuthService, useValue: { login } }],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    await fixture.whenStable();
  });

  const page = () => fixture.nativeElement as HTMLElement;

  async function submit(username: string, password: string) {
    const [user, pass] = Array.from(page().querySelectorAll('input'));
    user.value = username;
    user.dispatchEvent(new Event('input'));
    pass.value = password;
    pass.dispatchEvent(new Event('input'));
    page().querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('does not call the backend when fields are empty', async () => {
    await submit('', '');

    expect(login).not.toHaveBeenCalled();
    expect(page().querySelectorAll('mat-error').length).toBe(2);
  });

  it('greets the user after a successful login', async () => {
    login.mockReturnValue(of({ message: 'user connected' }));

    await submit('alice', 'password123');

    expect(login).toHaveBeenCalledWith('alice', 'password123');
    expect(page().querySelector('.success')?.textContent).toContain('Welcome, alice');
  });

  it('shows an error for wrong credentials', async () => {
    login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401 })));

    await submit('alice', 'wrong');

    expect(page().querySelector('.error')?.textContent).toContain('Invalid username or password');
  });

  it('shows an error when the server cannot be reached', async () => {
    login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    await submit('alice', 'password123');

    expect(page().querySelector('.error')?.textContent).toContain('Could not reach the server');
  });
});
