export type Status = 'RUNNING' | 'STOPPED' | 'UNKNOWN';

export interface Application {

  id?: number;

  name: string;

  description: string;

  port: number;

  status: Status;

  lastCheckedAt?: string | null;

  lastStatusChangeAt?: string | null;

}
