import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiHttp } from '../http/api-http';
import { UserStats } from '../models/user-stats';

/**
 * Service for fetching user statistics.
 * Endpoint: /api/stats/me
 */
@Injectable({ providedIn: 'root' })
export class UserStatsService {
  private readonly api = inject(ApiHttp);

  /**
   * Get statistics for the current authenticated user.
   * Returns total dreams count and most common mood.
   */
  getMyStats(): Observable<UserStats> {
    return this.api.get<UserStats>('/stats/me');
  }
}
