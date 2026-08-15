import { Status } from './application';

export interface StatusEvent {
  id: number;
  applicationId: number;
  applicationName: string;
  oldStatus: Status;
  newStatus: Status;
  occurredAt: string;
}
