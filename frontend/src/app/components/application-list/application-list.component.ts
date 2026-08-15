import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Application } from '../../models/application';
import { ApplicationService } from '../../services/application.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { StatusSummaryComponent } from '../status-summary/status-summary.component';

type SortColumn = 'name' | 'port' | 'status';

const EMPTY_APPLICATION: Application = {
  name: '',
  description: '',
  port: 0,
  status: 'UNKNOWN'
};

@Component({
  selector: 'app-application-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    StatusSummaryComponent
  ],
  templateUrl: './application-list.component.html',
  styleUrl: './application-list.component.css'
})
export class ApplicationListComponent implements OnInit {

  applications: Application[] = [];

  loading = false;
  errorMessage = '';

  newApplication: Application = { ...EMPTY_APPLICATION };

  editingId: number | null = null;
  editForm: Application = { ...EMPTY_APPLICATION };

  searchTerm = '';
  sortColumn: SortColumn = 'name';
  sortDirection: 'asc' | 'desc' = 'asc';

  constructor(
    private applicationService: ApplicationService,
    public authService: AuthService,
    private toastService: ToastService
  ) { }

  ngOnInit(): void {
    this.loadApplications();
  }

  loadApplications(): void {

    this.loading = true;
    this.errorMessage = '';

    this.applicationService.getApplications().subscribe({
      next: data => {
        this.applications = data;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Could not load applications. Is the backend running?';
        this.loading = false;
      }
    });
  }

  get visibleApplications(): Application[] {

    const term = this.searchTerm.trim().toLowerCase();

    const filtered = term
      ? this.applications.filter(app =>
          app.name.toLowerCase().includes(term) ||
          (app.description ?? '').toLowerCase().includes(term))
      : this.applications;

    const direction = this.sortDirection === 'asc' ? 1 : -1;

    return [...filtered].sort((a, b) => {
      const left = a[this.sortColumn];
      const right = b[this.sortColumn];
      if (left < right) return -1 * direction;
      if (left > right) return 1 * direction;
      return 0;
    });
  }

  sortBy(column: SortColumn): void {

    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
  }

  addApplication(): void {

    this.applicationService.addApplication(this.newApplication).subscribe({
      next: () => {
        this.loadApplications();
        this.newApplication = { ...EMPTY_APPLICATION };
        this.toastService.success('Application added.');
      },
      error: err => this.toastService.error(this.extractError(err, 'Could not add application.'))
    });
  }

  startEdit(app: Application): void {
    this.editingId = app.id ?? null;
    this.editForm = { ...app };
  }

  cancelEdit(): void {
    this.editingId = null;
  }

  saveEdit(): void {

    if (this.editingId == null) {
      return;
    }

    this.applicationService.updateApplication(this.editingId, this.editForm).subscribe({
      next: () => {
        this.loadApplications();
        this.editingId = null;
        this.toastService.success('Application updated.');
      },
      error: err => this.toastService.error(this.extractError(err, 'Could not update application.'))
    });
  }

  deleteApplication(id: number): void {

    if (!confirm('Delete this application?')) {
      return;
    }

    this.applicationService.deleteApplication(id).subscribe({
      next: () => {
        this.loadApplications();
        this.toastService.success('Application deleted.');
      },
      error: err => this.toastService.error(this.extractError(err, 'Could not delete application.'))
    });
  }

  private extractError(err: any, fallback: string): string {
    return err?.error?.message ?? fallback;
  }

  relativeTime(iso: string | null | undefined): string {

    if (!iso) {
      return 'never';
    }

    const seconds = Math.round((Date.now() - new Date(iso).getTime()) / 1000);

    if (seconds < 60) return `${seconds}s ago`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
    return `${Math.floor(seconds / 86400)}d ago`;
  }
}
