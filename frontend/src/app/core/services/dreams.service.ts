import {inject, Injectable} from '@angular/core';
import {Observable, of} from 'rxjs';

import {ApiHttp} from '../http/api-http';
import {CreateDreamRequest, CreatedResponse, Dream, PagedResponse, UpdateDreamRequest,} from '../models/dream';

/**
 * Service for managing dream entries (CRUD operations).
 * Base endpoint: /api/dreams
 */
@Injectable({providedIn: 'root'})
export class DreamsService {
  private readonly api = inject(ApiHttp);
  private readonly baseUrl = '/dreams';

  /**
   * Get paginated dreams.
   * @param page zero-based page number (default: 0)
   * @param size number of items per page (default: 20)
   * @param sort sorting criteria in format "property,direction" (default: "date,desc")
   */
  list(page = 0, size = 20, sort = 'date,desc'): Observable<PagedResponse<Dream>> {
    return this.api.get<PagedResponse<Dream>>(
      `${this.baseUrl}?page=${page}&size=${size}&sort=${sort}`,
    );
  }

  /**
   * Get single dream by ID.
   */
  get(id: string): Observable<Dream> {
    return this.api.get<Dream>(`${this.baseUrl}/${id}`);
  }

  /**
   * Create new dream entry.
   */
  create(request: CreateDreamRequest): Observable<CreatedResponse> {
    return this.api.post<CreatedResponse>(this.baseUrl, request);
  }

  /**
   * Update existing dream entry (PUT - full replacement).
   */
  update(id: string, request: UpdateDreamRequest): Observable<void> {
    return this.api.put<void>(`${this.baseUrl}/${id}`, request);
  }

  /**
   * Delete dream entry.
   */
  delete(id: string): Observable<void> {
    return this.api.delete<void>(`${this.baseUrl}/${id}`);
  }

  /**
   * Search dreams by query string.
   * Minimum 3 characters required.
   * @param query search query
   * @returns array of matching dreams
   */
  search(query: string): Observable<Dream[]> {
    if (!query || query.trim().length < 3) {
      return of([]);
    }
    return this.api.get<Dream[]>(
      `${this.baseUrl}/search?query=${encodeURIComponent(query.trim())}`,
    );
  }
}
