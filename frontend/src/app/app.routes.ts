import { Routes } from '@angular/router';
import { authGuard } from './auth/auth-guard';
import { Login } from './login/login';
import { Shell } from './shell/shell';

export const routes: Routes = [
  { path: 'login', component: Login },
  {
    // Everything except the login page needs a signed-in user.
    // Pages are loaded on demand, so the first download stays small.
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', loadComponent: () => import('./home/home').then((m) => m.Home) },
      {
        path: 'loans/new',
        loadComponent: () => import('./loans/new-loan/new-loan').then((m) => m.NewLoan),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
