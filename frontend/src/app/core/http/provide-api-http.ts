import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import type { EnvironmentProviders } from '@angular/core';

import { errorInterceptor } from './error-interceptor';

/**
 * The `HttpClient` of the app: the error interceptor, and CSRF with the cookie and header of Spring
 * (Angular's defaults, explicit). Angular only adds the header to relative URLs, so it never reaches
 * the photo uploads to S3.
 */
export function provideApiHttp(): EnvironmentProviders {
  return provideHttpClient(
    withInterceptors([errorInterceptor]),
    withXsrfConfiguration({ cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN' }),
  );
}
