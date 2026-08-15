import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { HostMetrics, DockerContainer } from '../models/system';

@Injectable({
  providedIn: 'root'
})
export class SystemService {

  private apiUrl = `${environment.apiUrl}/system`;

  constructor(private http: HttpClient) { }

  getMetrics(): Observable<HostMetrics> {
    return this.http.get<HostMetrics>(`${this.apiUrl}/metrics`);
  }

  getContainers(): Observable<DockerContainer[]> {
    return this.http.get<DockerContainer[]>(`${this.apiUrl}/containers`);
  }
}
