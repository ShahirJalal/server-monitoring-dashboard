import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Application, Status } from '../../models/application';

interface StatusSlice {
  status: Status;
  label: string;
  count: number;
  percent: number;
}

@Component({
  selector: 'app-status-summary',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './status-summary.component.html',
  styleUrl: './status-summary.component.css'
})
export class StatusSummaryComponent {

  private applicationsSignal = signal<Application[]>([]);

  @Input()
  set applications(value: Application[]) {
    this.applicationsSignal.set(value ?? []);
  }

  total = computed(() => this.applicationsSignal().length);

  private countOf(status: Status): number {
    return this.applicationsSignal().filter(app => app.status === status).length;
  }

  running = computed(() => this.countOf('RUNNING'));
  stopped = computed(() => this.countOf('STOPPED'));
  unknown = computed(() => this.countOf('UNKNOWN'));

  /** Drives the segmented proportion bar + legend, in a fixed status order. */
  slices = computed<StatusSlice[]>(() => {

    const total = this.total();
    const toPercent = (count: number) => (total === 0 ? 0 : (count / total) * 100);

    const allSlices: StatusSlice[] = [
      { status: 'RUNNING', label: 'Running', count: this.running(), percent: toPercent(this.running()) },
      { status: 'STOPPED', label: 'Stopped', count: this.stopped(), percent: toPercent(this.stopped()) },
      { status: 'UNKNOWN', label: 'Unknown', count: this.unknown(), percent: toPercent(this.unknown()) },
    ];

    return allSlices.filter(slice => slice.count > 0);
  });
}
