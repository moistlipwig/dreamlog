import { HttpClient } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';

export class ApiHttp {
  private http = inject(HttpClient);

  get<T>(url: string, opts: any = {}) {
    return this.http.get<T>(environment.apiBaseUrl + url, {
      withCredentials: environment.withCredentials,
      ...opts
    });
  }

  post<T>(url: string, body: unknown, opts: any = {}) {
    return this.http.post<T>(environment.apiBaseUrl + url, body, {
      withCredentials: environment.withCredentials,
      ...opts
    });
  }

  // analogicznie: put/delete/patch – dorobimy później
}
