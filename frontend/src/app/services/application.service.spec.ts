import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { ApplicationService } from './application.service';
import { Application } from '../models/application';

describe('ApplicationService', () => {

  let service: ApplicationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ApplicationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getApplications GETs /api/applications', () => {

    const apps: Application[] = [
      { id: 1, name: 'api', description: '', port: 8080, status: 'RUNNING' }
    ];

    service.getApplications().subscribe(result => expect(result).toEqual(apps));

    const req = httpMock.expectOne('/api/applications');
    expect(req.request.method).toBe('GET');
    req.flush(apps);
  });

  it('updateApplication PUTs to /api/applications/:id', () => {

    const app: Application = { name: 'api', description: '', port: 8080, status: 'RUNNING' };

    service.updateApplication(1, app).subscribe();

    const req = httpMock.expectOne('/api/applications/1');
    expect(req.request.method).toBe('PUT');
    req.flush({ ...app, id: 1 });
  });
});
