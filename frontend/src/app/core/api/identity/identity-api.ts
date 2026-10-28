import { HttpClient, HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { apiPath } from '../api-path';
import type { components } from '../schema';

export type UserResponse = components['schemas']['UserResponse'];

/** Spring Security's logout, which is a filter and not an endpoint of the spec. */
const LOGOUT_PATH = '/api/logout';

/** Endpoints of the backend's `identity` module. */
@Injectable({ providedIn: 'root' })
export class IdentityApi {
  private readonly http = inject(HttpClient);

  /** The user with the session; `401` without one. */
  me(context?: HttpContext): Observable<UserResponse> {
    return this.http.get<UserResponse>(apiPath('/api/me'), { context });
  }

  /** Ends the session: `204`. */
  logout(): Observable<null> {
    return this.http.post<null>(LOGOUT_PATH, null);
  }
}
