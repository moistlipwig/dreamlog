import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface DayCount {
  date: string;
  count: number;
}

@Injectable({ providedIn: 'root' })
export class CalendarService {
  private cache = new Map<string, number>();
  private countsSubject = new BehaviorSubject<DayCount[]>([]);
  readonly counts$ = this.countsSubject.asObservable();

  setCounts(data: DayCount[]) {
    data.forEach((d) => this.cache.set(d.date, d.count));
    this.countsSubject.next(data);
  }
}
