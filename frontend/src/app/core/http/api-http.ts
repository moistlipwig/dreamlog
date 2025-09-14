import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { environment } from '../../../environments/environment';

// Derive HttpClient option types from Angular's own method signatures
export type HttpGetOptions = Parameters<HttpClient["get"]>[1];
export type HttpPostOptions = Parameters<HttpClient["post"]>[2];

@Injectable({ providedIn: 'root' })
export class ApiHttp {
  private http = inject(HttpClient);

  get<T>(url: string, opts: Partial<HttpGetOptions> = {}) {
    const options = {
      withCredentials: environment.withCredentials,
      ...opts
    };
    // We intentionally allow any HttpClient options shape here (observe/responseType/etc.)
    return this.http.get<T>(environment.apiBaseUrl + url, options as HttpGetOptions);
  }

  post<T>(url: string, body: unknown, opts: Partial<HttpPostOptions> = {}) {
    const options = {
      withCredentials: environment.withCredentials,
      ...opts
    };
    // We intentionally allow any HttpClient options shape here (observe/responseType/etc.)
    return this.http.post<T>(environment.apiBaseUrl + url, body, options as HttpPostOptions);
  }

  // analogicznie: put/delete/patch – dorobimy później
}
