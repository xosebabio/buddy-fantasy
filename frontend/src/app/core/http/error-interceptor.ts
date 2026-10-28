import {
  HttpContext,
  HttpContextToken,
  HttpErrorResponse,
  type HttpInterceptorFn,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { retry, tap, throwError } from 'rxjs';

import { ReturnUrl } from '../auth/return-url';
import { FailedRequests } from './failed-requests';

/** Statuses that the caller handles itself, so the interceptor lets them through untouched. */
export const HANDLED_STATUSES = new HttpContextToken<readonly number[]>(() => []);

/** The context of a request whose page handles these statuses itself (a `404` of its own…). */
export function handling(...statuses: number[]): HttpContext {
  return new HttpContext().set(HANDLED_STATUSES, statuses);
}

export const LOGIN_ROUTE = '/login';
export const FORBIDDEN_ROUTE = '/forbidden';
export const NOT_FOUND_ROUTE = '/not-found';

/**
 * The errors of the API that no page handles (FE-08): `401` saves the route and goes to the login,
 * `403` and `404` show their page keeping the URL, and a `5xx` or a network failure waits for the
 * user to retry. `400` and `409` go to the page, and so does any status it marks with `handling`.
 * Other origins (the photo uploads to S3) are left alone.
 */
export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/api/')) {
    return next(request);
  }
  const router = inject(Router);
  const returnUrl = inject(ReturnUrl);
  const failedRequests = inject(FailedRequests);
  const handled = request.context.get(HANDLED_STATUSES);
  const isGlobal = (error: unknown): error is HttpErrorResponse =>
    error instanceof HttpErrorResponse && !handled.includes(error.status);

  return next(request).pipe(
    retry({
      delay: (error: unknown) =>
        isGlobal(error) && isTransient(error)
          ? failedRequests.waitForRetry(error)
          : throwError(() => error),
    }),
    tap({
      error: (error: unknown) => {
        if (!isGlobal(error)) {
          return;
        }
        if (error.status === 401) {
          const navigation = router.currentNavigation();
          const url = navigation ? router.serializeUrl(navigation.extractedUrl) : router.url;
          if (!url.startsWith(LOGIN_ROUTE)) {
            returnUrl.save(url);
          }
          void router.navigateByUrl(LOGIN_ROUTE);
        } else if (error.status === 403) {
          void router.navigateByUrl(FORBIDDEN_ROUTE, { skipLocationChange: true });
        } else if (error.status === 404) {
          void router.navigateByUrl(NOT_FOUND_ROUTE, { skipLocationChange: true });
        }
      },
    }),
  );
};

function isTransient(error: HttpErrorResponse): boolean {
  return error.status === 0 || error.status >= 500;
}
