import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../auth/auth-service';
import { Home } from './home';

describe('Home', () => {
  it('greets the user and lists the five workflow steps', async () => {
    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [provideRouter([]), { provide: AuthService, useValue: { currentUser: signal('alice') } }],
    }).compileComponents();

    const fixture = TestBed.createComponent(Home);
    await fixture.whenStable();
    const page = fixture.nativeElement as HTMLElement;

    expect(page.querySelector('h1')?.textContent).toContain('Hello, alice');
    expect(page.querySelectorAll('.steps li').length).toBe(5);
  });
});
