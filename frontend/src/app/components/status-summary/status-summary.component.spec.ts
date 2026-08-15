import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StatusSummaryComponent } from './status-summary.component';
import { Application } from '../../models/application';

describe('StatusSummaryComponent', () => {

  let component: StatusSummaryComponent;
  let fixture: ComponentFixture<StatusSummaryComponent>;

  const apps: Application[] = [
    { id: 1, name: 'a', description: '', port: 1, status: 'RUNNING' },
    { id: 2, name: 'b', description: '', port: 2, status: 'RUNNING' },
    { id: 3, name: 'c', description: '', port: 3, status: 'STOPPED' },
    { id: 4, name: 'd', description: '', port: 4, status: 'UNKNOWN' },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusSummaryComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(StatusSummaryComponent);
    component = fixture.componentInstance;
  });

  it('counts applications by status', () => {
    component.applications = apps;
    expect(component.total()).toBe(4);
    expect(component.running()).toBe(2);
    expect(component.stopped()).toBe(1);
    expect(component.unknown()).toBe(1);
  });

  it('computes slice percentages and omits empty statuses', () => {
    component.applications = apps;
    const slices = component.slices();
    expect(slices.length).toBe(3);
    expect(slices.find(s => s.status === 'RUNNING')?.percent).toBe(50);
    expect(slices.find(s => s.status === 'STOPPED')?.percent).toBe(25);
  });

  it('handles an empty list without dividing by zero', () => {
    component.applications = [];
    expect(component.total()).toBe(0);
    expect(component.slices()).toEqual([]);
  });
});
