import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type ToastVariant = 'success' | 'danger' | 'info';

export interface Toast {
  id: number;
  message: string;
  variant: ToastVariant;
}

/**
 * Minimal toast/notification store, rendered by ToastContainerComponent. Kept
 * framework-free (Bootstrap classes only) since Bootstrap is already the app's
 * only styling dependency -- PrimeNG's toast would need its own theme/animations
 * wiring the app doesn't otherwise use.
 */
@Injectable({
  providedIn: 'root'
})
export class ToastService {

  private nextId = 1;
  private toastsSubject = new BehaviorSubject<Toast[]>([]);
  toasts$ = this.toastsSubject.asObservable();

  private show(message: string, variant: ToastVariant): void {

    const toast: Toast = { id: this.nextId++, message, variant };
    this.toastsSubject.next([...this.toastsSubject.value, toast]);

    setTimeout(() => this.dismiss(toast.id), 5000);
  }

  success(message: string): void {
    this.show(message, 'success');
  }

  error(message: string): void {
    this.show(message, 'danger');
  }

  info(message: string): void {
    this.show(message, 'info');
  }

  dismiss(id: number): void {
    this.toastsSubject.next(this.toastsSubject.value.filter(t => t.id !== id));
  }
}
