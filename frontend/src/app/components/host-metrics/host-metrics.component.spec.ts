import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { HostMetricsComponent } from './host-metrics.component';
import { HostMetrics } from '../../models/system';

describe('HostMetricsComponent', () => {

  let component: HostMetricsComponent;
  let fixture: ComponentFixture<HostMetricsComponent>;
  let httpMock: HttpTestingController;

  const metrics: HostMetrics = {
    cpuUsagePercent: 42,
    memoryUsedBytes: 4_000_000_000,
    memoryTotalBytes: 8_000_000_000,
    diskUsedBytes: 92_000_000_000,
    diskTotalBytes: 100_000_000_000,
    uptimeSeconds: 90000
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HostMetricsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(HostMetricsComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('builds meters from fetched metrics', () => {
    fixture.detectChanges();
    httpMock.expectOne('/api/system/metrics').flush(metrics);

    const meters = component.meters;
    expect(meters.length).toBe(3);
    expect(meters.find(m => m.label === 'CPU')?.percent).toBe(42);
  });

  it('flags disk as a warning past the threshold', () => {
    fixture.detectChanges();
    httpMock.expectOne('/api/system/metrics').flush(metrics);

    expect(component.diskWarning).not.toBeNull();
    expect(component.diskWarning?.label).toBe('Disk');
  });

  it('formats uptime as days/hours', () => {
    fixture.detectChanges();
    httpMock.expectOne('/api/system/metrics').flush(metrics);

    expect(component.uptimeLabel).toBe('1d 1h');
  });

  it('handles a failed fetch without throwing', () => {
    fixture.detectChanges();
    httpMock.expectOne('/api/system/metrics').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(component.loading).toBeFalse();
    expect(component.meters).toEqual([]);
  });
});
