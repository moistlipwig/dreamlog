import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  CreateDreamEntryRequest,
  DreamEntry,
  UpdateDreamEntryRequest,
} from '../models/dream-entry.model';

/**
 * Serwis do zarządzania wpisami snów (CRUD operations).
 *
 * Endpoint bazowy: /api/dreams
 */
@Injectable({
  providedIn: 'root',
})
export class DreamEntryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/dreams';

  /**
   * Pobiera wszystkie wpisy snów.
   */
  getAll(): Observable<DreamEntry[]> {
    return this.http.get<DreamEntry[]>(this.baseUrl);
  }

  /**
   * Pobiera pojedynczy wpis snu po ID.
   */
  getById(id: string): Observable<DreamEntry> {
    return this.http.get<DreamEntry>(`${this.baseUrl}/${id}`);
  }

  /**
   * Tworzy nowy wpis snu.
   */
  create(request: CreateDreamEntryRequest): Observable<DreamEntry> {
    return this.http.post<DreamEntry>(this.baseUrl, request);
  }

  /**
   * Aktualizuje istniejący wpis snu.
   */
  update(id: string, request: UpdateDreamEntryRequest): Observable<DreamEntry> {
    return this.http.put<DreamEntry>(`${this.baseUrl}/${id}`, request);
  }

  /**
   * Usuwa wpis snu.
   */
  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
