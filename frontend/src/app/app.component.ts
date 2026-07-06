import { Component } from '@angular/core';
import { ApplicationListComponent } from './components/application-list/application-list.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [ApplicationListComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {}