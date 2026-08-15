import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {

  username = '';
  password = '';
  submitting = false;
  errorMessage = '';

  constructor(
    private authService: AuthService,
    private toastService: ToastService,
    private router: Router
  ) { }

  submit(): void {

    if (!this.username || !this.password) {
      return;
    }

    this.submitting = true;
    this.errorMessage = '';

    this.authService.login(this.username, this.password).subscribe({
      next: user => {
        this.submitting = false;
        this.toastService.success(`Signed in as ${user.username}`);
        this.router.navigate(['/']);
      },
      error: () => {
        this.submitting = false;
        this.errorMessage = 'Invalid username or password.';
      }
    });
  }
}
