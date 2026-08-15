import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';
import { ToastService } from './services/toast.service';
import { ToastContainerComponent } from './components/toast-container/toast-container.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet, ToastContainerComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {

  constructor(
    public authService: AuthService,
    private toastService: ToastService
  ) { }

  ngOnInit(): void {
    this.authService.restoreSession().subscribe();
  }

  logout(): void {
    this.authService.logout().subscribe(() => this.toastService.info('Signed out.'));
  }
}
