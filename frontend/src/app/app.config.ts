import {provideHttpClient, withInterceptors, withXsrfConfiguration, HttpInterceptorFn} from '@angular/common/http';
import {ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection,} from '@angular/core';
import {provideRouter} from '@angular/router';

import {authInterceptor} from '@core/http/auth-interceptor';
import {errorInterceptor} from '@core/http/error-interceptor';

import {routes} from './app.routes';

const typedAuthInterceptor: HttpInterceptorFn = authInterceptor as unknown as HttpInterceptorFn;
const typedErrorInterceptor: HttpInterceptorFn = errorInterceptor as unknown as HttpInterceptorFn;
const httpInterceptors: HttpInterceptorFn[] = [typedAuthInterceptor, typedErrorInterceptor];

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({eventCoalescing: true}),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors(httpInterceptors),
      withXsrfConfiguration({
        cookieName: 'XSRF-TOKEN',
        headerName: 'X-XSRF-TOKEN',
      })
    ),
  ],
};
