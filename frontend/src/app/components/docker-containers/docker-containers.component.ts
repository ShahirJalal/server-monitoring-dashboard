import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SystemService } from '../../services/system.service';
import { DockerContainer } from '../../models/system';

@Component({
  selector: 'app-docker-containers',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './docker-containers.component.html',
  styleUrl: './docker-containers.component.css'
})
export class DockerContainersComponent implements OnInit {

  containers: DockerContainer[] = [];
  loading = true;
  errorMessage = '';

  constructor(private systemService: SystemService) { }

  ngOnInit(): void {
    this.load();
  }

  load(): void {

    this.loading = true;
    this.errorMessage = '';

    this.systemService.getContainers().subscribe({
      next: containers => {
        this.containers = containers;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Could not load containers.';
        this.loading = false;
      }
    });
  }
}
