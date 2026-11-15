import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../auth/auth-service';

/** Frame for every page after sign-in: top bar with the user and a sign-out button. */
@Component({
  imports: [RouterLink, RouterOutlet, MatButtonModule, MatIconModule, MatToolbarModule],
  selector: 'app-shell',
  styleUrl: './shell.css',
  templateUrl: './shell.html',
})
export class Shell {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly user = this.auth.currentUser;

  protected signOut(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
