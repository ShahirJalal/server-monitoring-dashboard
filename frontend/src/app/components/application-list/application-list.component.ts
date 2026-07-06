import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { Application } from '../../models/application';
import { ApplicationService } from '../../services/application.service';

@Component({
  selector: 'app-application-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './application-list.component.html',
  styleUrl: './application-list.component.css'
})
export class ApplicationListComponent implements OnInit {

  applications: Application[] = [];

  newApplication: Application = {
    name: '',
    description: '',
    port: 0,
    status: 'RUNNING'
  };

  constructor(private applicationService: ApplicationService) { }

  ngOnInit(): void {
    this.loadApplications();
  }

  loadApplications(): void {
    this.applicationService.getApplications().subscribe({
      next: data => this.applications = data,
      error: err => console.error(err)
    });
  }

  addApplication(): void {

    this.applicationService.addApplication(this.newApplication)
      .subscribe(() => {

        this.loadApplications();

        this.newApplication = {
          name: '',
          description: '',
          port: 0,
          status: 'RUNNING'
        };

      });

  }

  deleteApplication(id: number): void {

    if (!confirm('Delete this application?')) {
      return;
    }

    this.applicationService.deleteApplication(id)
      .subscribe(() => {
        this.loadApplications();
      });

  }

}