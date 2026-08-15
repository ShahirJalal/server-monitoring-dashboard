import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { DockerContainersComponent } from './docker-containers.component';
import { DockerContainer } from '../../models/system';

describe('DockerContainersComponent', () => {

  let component: DockerContainersComponent;
  let fixture: ComponentFixture<DockerContainersComponent>;
  let httpMock: HttpTestingController;

  const containers: DockerContainer[] = [
    { id: 'abc123', name: 'backend', image: 'server-monitoring-dashboard-backend', state: 'running', status: 'Up 2 hours' }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DockerContainersComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(DockerContainersComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads containers on init', () => {
    fixture.detectChanges();
    httpMock.expectOne('/api/system/containers').flush(containers);

    expect(component.containers.length).toBe(1);
    expect(component.loading).toBeFalse();
  });

  it('shows an error message when the fetch fails', () => {
    fixture.detectChanges();
    httpMock.expectOne('/api/system/containers').flush(null, { status: 500, statusText: 'Server Error' });

    expect(component.errorMessage).not.toBe('');
  });
});
