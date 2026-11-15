import { HttpClient } from '@angular/common/http';
import { Service, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

export interface LoginResponse {
  message: string;
}

/**
 * Calls the backend auth endpoint. `/api` is forwarded to the backend by the dev proxy.
 * Remembers who signed in until the page is reloaded (the backend has no sessions yet).
 */
@Service()
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly user = signal<string | null>(null);

  /** Username of the signed-in user, or null. */
  readonly currentUser = this.user.asReadonly();

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>('/api/login', { username, password })
      .pipe(tap(() => this.user.set(username)));
  }

  logout(): void {
    this.user.set(null);
  }
}
