import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription, interval, startWith, switchMap } from 'rxjs';
import { SystemService } from '../../services/system.service';
import { HostMetrics } from '../../models/system';

type Severity = 'good' | 'warning' | 'critical';

interface Meter {
  label: string;
  percent: number | null;
  detail: string;
  severity: Severity;
}

const POLL_INTERVAL_MS = 15000;
const WARNING_THRESHOLD = 70;
const CRITICAL_THRESHOLD = 90;

@Component({
  selector: 'app-host-metrics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './host-metrics.component.html',
  styleUrl: './host-metrics.component.css'
})
export class HostMetricsComponent implements OnInit, OnDestroy {

  metrics: HostMetrics | null = null;
  loading = true;
  private subscription?: Subscription;

  constructor(private systemService: SystemService) { }

  ngOnInit(): void {
    this.subscription = interval(POLL_INTERVAL_MS).pipe(
      startWith(0),
      switchMap(() => this.systemService.getMetrics())
    ).subscribe({
      next: metrics => {
        this.metrics = metrics;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  get meters(): Meter[] {

    if (!this.metrics) {
      return [];
    }

    const cpuPercent = this.metrics.cpuUsagePercent;
    const memPercent = this.ratio(this.metrics.memoryUsedBytes, this.metrics.memoryTotalBytes);
    const diskPercent = this.ratio(this.metrics.diskUsedBytes, this.metrics.diskTotalBytes);

    return [
      {
        label: 'CPU',
        percent: cpuPercent,
        detail: cpuPercent === null ? 'unavailable' : `${cpuPercent.toFixed(0)}%`,
        severity: this.severityOf(cpuPercent)
      },
      {
        label: 'Memory',
        percent: memPercent,
        detail: memPercent === null ? 'unavailable' :
          `${this.formatBytes(this.metrics.memoryUsedBytes)} / ${this.formatBytes(this.metrics.memoryTotalBytes)}`,
        severity: this.severityOf(memPercent)
      },
      {
        label: 'Disk',
        percent: diskPercent,
        detail: diskPercent === null ? 'unavailable' :
          `${this.formatBytes(this.metrics.diskUsedBytes)} / ${this.formatBytes(this.metrics.diskTotalBytes)}`,
        severity: this.severityOf(diskPercent)
      }
    ];
  }

  get diskWarning(): Meter | null {
    const disk = this.meters.find(m => m.label === 'Disk');
    return disk && disk.percent !== null && disk.percent >= WARNING_THRESHOLD ? disk : null;
  }

  get uptimeLabel(): string {

    const seconds = this.metrics?.uptimeSeconds;
    if (seconds == null) {
      return 'unavailable';
    }

    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);

    if (days > 0) return `${days}d ${hours}h`;
    const minutes = Math.floor((seconds % 3600) / 60);
    if (hours > 0) return `${hours}h ${minutes}m`;
    return `${minutes}m`;
  }

  private ratio(used: number | null, total: number | null): number | null {
    if (used == null || total == null || total === 0) {
      return null;
    }
    return (used / total) * 100;
  }

  private severityOf(percent: number | null): Severity {
    if (percent === null) return 'good';
    if (percent >= CRITICAL_THRESHOLD) return 'critical';
    if (percent >= WARNING_THRESHOLD) return 'warning';
    return 'good';
  }

  private formatBytes(bytes: number | null): string {

    if (bytes == null) {
      return 'unavailable';
    }

    const gb = bytes / (1024 * 1024 * 1024);
    if (gb >= 1) {
      return `${gb.toFixed(1)} GB`;
    }

    const mb = bytes / (1024 * 1024);
    return `${mb.toFixed(0)} MB`;
  }
}
