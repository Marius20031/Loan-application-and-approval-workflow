import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../auth/auth-service';
import { Shell } from './shell';

describe('Shell', () => {
  let fixture: ComponentFixture<Shell>;
  const logout = vi.fn();

  beforeEach(async () => {
    logout.mockReset();
    await TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { currentUser: signal('alice'), logout } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Shell);
    await fixture.whenStable();
  });

  const page = () => fixture.nativeElement as HTMLElement;

  it('shows who is signed in', () => {
    expect(page().querySelector('.user')?.textContent).toContain('alice');
  });

  it('signs out and goes back to the login page', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    page().querySelector<HTMLButtonElement>('.sign-out')!.click();

    expect(logout).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith('/login');
  });
});
