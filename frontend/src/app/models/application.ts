export interface Application {

  id?: number;

  name: string;

  description: string;

  port: number;

  status: 'RUNNING' | 'STOPPED';

}