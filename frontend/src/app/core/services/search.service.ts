import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SearchService {
  private query = new BehaviorSubject<string>('');
  readonly query$ = this.query.asObservable();

  setQuery(q: string) {
    this.query.next(q);
  }
}
