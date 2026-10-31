import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { errorInterceptor } from '../http/error-interceptor';
import { ReturnUrl } from './return-url';
import { answerSession, LEAGUES, USER } from './session.testing';
import { BROWSER_LOCATION, SessionStore } from './session-store';

describe('SessionStore', () => {
  let store: SessionStore;
  let http: HttpTestingController;
  const location = { assign: vi.fn() };

  beforeEach(() => {
    sessionStorage.clear();
    location.assign.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: BROWSER_LOCATION, useValue: location },
      ],
    });
    store = TestBed.inject(SessionStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('loads the user and their leagues', async () => {
    const loaded = store.load();

    answerSession(http, USER);
    await loaded;

    expect(store.user()).toEqual(USER);
    expect(store.leagues()).toEqual(LEAGUES);
    expect(store.league('l2')?.role).toBe('MANAGER');
    expect(store.league('other')).toBeUndefined();
  });

  it('has no session after a 401, without going to the login', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl');
    const loaded = store.load();

    answerSession(http, null);
    await loaded;

    expect(store.user()).toBeNull();
    expect(store.leagues()).toEqual([]);
    expect(navigate).not.toHaveBeenCalled();
    expect(TestBed.inject(ReturnUrl).consume()).toBeNull();
  });

  it('asks for the session only once', async () => {
    const first = store.load();
    const second = store.load();

    answerSession(http, USER);
    await Promise.all([first, second]);
    await store.load();

    http.expectNone('/api/me');
  });

  it('reads the leagues again', async () => {
    const loaded = store.load();
    answerSession(http, USER);
    await loaded;

    const reloaded = firstValueFrom(store.reloadLeagues());
    http.expectOne('/api/me/leagues').flush(LEAGUES.slice(0, 1));
    await reloaded;

    expect(store.leagues()).toEqual(LEAGUES.slice(0, 1));
    expect(store.user()).toEqual(USER);
  });

  it('leaves the app to log in with Google', () => {
    store.login();

    expect(location.assign).toHaveBeenCalledWith('/oauth2/authorization/google');
    expect(TestBed.inject(ReturnUrl).consume()).toBeNull();
  });

  it('saves the route to come back to after the login', () => {
    store.login('/invite/tok');

    expect(TestBed.inject(ReturnUrl).consume()).toBe('/invite/tok');
  });

  it('ends the session and goes to the login', async () => {
    const loaded = store.load();
    answerSession(http, USER);
    await loaded;
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    const loggedOut = firstValueFrom(store.logout());
    const request = http.expectOne('/api/logout');
    expect(request.request.method).toBe('POST');
    request.flush(null, { status: 204, statusText: 'No Content' });
    await loggedOut;

    expect(store.user()).toBeNull();
    expect(store.leagues()).toEqual([]);
    expect(navigate).toHaveBeenCalledWith('/login');
  });
});
