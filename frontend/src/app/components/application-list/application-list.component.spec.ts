import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { ApplicationListComponent } from './application-list.component';
import { Application } from '../../models/application';

describe('ApplicationListComponent', () => {

  let component: ApplicationListComponent;
  let fixture: ComponentFixture<ApplicationListComponent>;
  let httpMock: HttpTestingController;

  const apps: Application[] = [
    { id: 1, name: 'zeta', description: 'z app', port: 9000, status: 'RUNNING' },
    { id: 2, name: 'alpha', description: 'a app', port: 8000, status: 'STOPPED' },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationListComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    httpMock = TestBed.inject(HttpTestingController);
    httpMock.expectOne('/api/applications').flush(apps);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('sorts by name ascending by default', () => {
    expect(component.visibleApplications.map(a => a.name)).toEqual(['alpha', 'zeta']);
  });

  it('reverses sort direction on repeated sortBy calls', () => {
    component.sortBy('name');
    expect(component.sortDirection).toBe('desc');
    expect(component.visibleApplications.map(a => a.name)).toEqual(['zeta', 'alpha']);
  });

  it('filters by search term across name and description', () => {
    component.searchTerm = 'z app';
    expect(component.visibleApplications.map(a => a.name)).toEqual(['zeta']);
  });
});
