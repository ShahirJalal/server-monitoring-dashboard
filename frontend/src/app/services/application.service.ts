import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Application } from '../models/application';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApplicationService {

  private apiUrl = `${environment.apiUrl}/applications`;

  constructor(private http: HttpClient) { }

  getApplications(): Observable<Application[]> {
    return this.http.get<Application[]>(this.apiUrl);
  }

  addApplication(application: Application): Observable<Application> {
    return this.http.post<Application>(this.apiUrl, application);
  }

  updateApplication(id: number, application: Application): Observable<Application> {
    return this.http.put<Application>(`${this.apiUrl}/${id}`, application);
  }

  deleteApplication(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

}
