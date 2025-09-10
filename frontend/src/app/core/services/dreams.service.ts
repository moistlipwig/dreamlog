import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiHttp } from '../http/api-http';
import { Dream } from '../models/dream';

@Injectable({ providedIn: 'root' })
export class DreamsService {
  private api = inject(ApiHttp);

  list(): Observable<Dream[]> {
    return this.api.get<Dream[]>('/dreams');
  }

  get(id: string): Observable<Dream> {
    return this.api.get<Dream>(`/dreams/${id}`);
  }

  create(dream: Partial<Dream>): Observable<Dream> {
    return this.api.post<Dream>('/dreams', dream);
  }

  update(id: string, dream: Partial<Dream>): Observable<Dream> {
    return this.api.post<Dream>(`/dreams/${id}`, dream);
  }

  delete(id: string) {
    return this.api.post<void>(`/dreams/${id}/delete`, {});
  }
}
