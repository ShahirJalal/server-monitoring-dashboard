import { Routes } from '@angular/router';
import { ApplicationListComponent } from './components/application-list/application-list.component';
import { LoginComponent } from './components/login/login.component';

export const routes: Routes = [
  { path: '', component: ApplicationListComponent },
  { path: 'login', component: LoginComponent },
  { path: '**', redirectTo: '' }
];
