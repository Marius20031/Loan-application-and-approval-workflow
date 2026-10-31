import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface LoginResponse {
  message: string;
}

/** Calls the backend auth endpoint. `/api` is forwarded to the backend by the dev proxy. */
@Service()
export class AuthService {
  private readonly http = inject(HttpClient);

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/login', { username, password });
  }
}
