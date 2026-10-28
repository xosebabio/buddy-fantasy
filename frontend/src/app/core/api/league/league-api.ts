import { HttpClient, HttpContext, httpResource, type HttpResourceRef } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { apiPath } from '../api-path';
import type { components } from '../schema';

export type MyLeagueResponse = components['schemas']['MyLeagueResponse'];
export type CreateLeagueRequest = components['schemas']['CreateLeagueRequest'];
export type CreateLeagueResponse = components['schemas']['CreateLeagueResponse'];
export type LeagueResponse = components['schemas']['LeagueResponse'];
export type InvitationTokenResponse = components['schemas']['InvitationTokenResponse'];
export type InvitationPreviewResponse = components['schemas']['InvitationPreviewResponse'];
export type JoinLeagueResponse = components['schemas']['JoinLeagueResponse'];
export type MemberResponse = components['schemas']['MemberResponse'];
export type LeagueSettingsBody = components['schemas']['LeagueSettingsBody'];
export type LeagueRole = MemberResponse['role'];

/**
 * Endpoints of the backend's `league` module. The reads that depend on a signal (the league or the
 * token of the route) return an `httpResource`, so they must be called in an injection context, such
 * as a field initializer of the page; a read whose argument is `undefined` stays idle.
 */
@Injectable({ providedIn: 'root' })
export class LeagueApi {
  private readonly http = inject(HttpClient);

  /** The user's leagues with their role (RF-08). */
  myLeagues(context?: HttpContext): Observable<MyLeagueResponse[]> {
    return this.http.get<MyLeagueResponse[]>(apiPath('/api/me/leagues'), { context });
  }

  /** Creates a league with the user as its admin, and its first invitation (RF-05). */
  create(body: CreateLeagueRequest): Observable<CreateLeagueResponse> {
    return this.http.post<CreateLeagueResponse>(apiPath('/api/leagues'), body);
  }

  league(
    leagueId: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<LeagueResponse | undefined> {
    return httpResource<LeagueResponse>(() => {
      const id = leagueId();
      return id === undefined ? undefined : { url: apiPath('/api/leagues/{id}', { id }), context };
    });
  }

  /** A new invitation link, which replaces the previous one (RF-06). */
  regenerateInvitation(leagueId: string): Observable<InvitationTokenResponse> {
    return this.http.post<InvitationTokenResponse>(
      apiPath('/api/leagues/{id}/invitation', { id: leagueId }),
      null,
    );
  }

  /** Leaves the league without a valid invitation link (RF-06). */
  revokeInvitation(leagueId: string): Observable<null> {
    return this.http.delete<null>(apiPath('/api/leagues/{id}/invitation', { id: leagueId }));
  }

  /** The league an invitation link leads to; `404` if the link is not valid (RF-06). */
  invitation(
    token: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<InvitationPreviewResponse | undefined> {
    return httpResource<InvitationPreviewResponse>(() => {
      const value = token();
      return value === undefined
        ? undefined
        : { url: apiPath('/api/invitations/{token}', { token: value }), context };
    });
  }

  /** Joins the league of an invitation link (RF-06). */
  join(token: string, context?: HttpContext): Observable<JoinLeagueResponse> {
    return this.http.post<JoinLeagueResponse>(
      apiPath('/api/invitations/{token}/join', { token }),
      null,
      { context },
    );
  }

  members(
    leagueId: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<MemberResponse[] | undefined> {
    return httpResource<MemberResponse[]>(() => {
      const id = leagueId();
      return id === undefined
        ? undefined
        : { url: apiPath('/api/leagues/{id}/members', { id }), context };
    });
  }

  /** Leaves the league; `409` `ADMIN_CANNOT_LEAVE` for its admin (RF-08). */
  leave(leagueId: string): Observable<null> {
    return this.http.delete<null>(apiPath('/api/leagues/{id}/members/me', { id: leagueId }));
  }

  /** Removes a member; `409` `ADMIN_CANNOT_LEAVE` for the admin (RF-08). */
  removeMember(leagueId: string, memberId: string): Observable<null> {
    return this.http.delete<null>(
      apiPath('/api/leagues/{id}/members/{memberId}', { id: leagueId, memberId }),
    );
  }

  /** The league's settings and scoring table (RF-07). */
  settings(
    leagueId: () => string | undefined,
    context?: HttpContext,
  ): HttpResourceRef<LeagueSettingsBody | undefined> {
    return httpResource<LeagueSettingsBody>(() => {
      const id = leagueId();
      return id === undefined
        ? undefined
        : { url: apiPath('/api/leagues/{id}/settings', { id }), context };
    });
  }

  /** Replaces the league's settings and scoring table (RF-07). */
  updateSettings(leagueId: string, body: LeagueSettingsBody): Observable<LeagueSettingsBody> {
    return this.http.put<LeagueSettingsBody>(
      apiPath('/api/leagues/{id}/settings', { id: leagueId }),
      body,
    );
  }
}
