import { HttpContext, HttpContextToken, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { IdentityApi, type UserResponse } from './identity-api';

const USER: UserResponse = {
  id: 'u1',
  email: 'iago@example.com',
  displayName: 'Iago',
  avatarUrl: null,
};

describe('IdentityApi', () => {
  let api: IdentityApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(IdentityApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('gets the user with the session, with the given context', async () => {
    const token = new HttpContextToken(() => false);
    const user = firstValueFrom(api.me(new HttpContext().set(token, true)));

    const request = http.expectOne({ method: 'GET', url: '/api/me' });
    expect(request.request.context.get(token)).toBe(true);
    request.flush(USER);

    expect(await user).toEqual(USER);
  });

  it('logs out with a POST', async () => {
    const done = firstValueFrom(api.logout(), { defaultValue: undefined });

    http.expectOne({ method: 'POST', url: '/api/logout' }).flush(null, {
      status: 204,
      statusText: 'No Content',
    });

    await expect(done).resolves.toBeNull();
  });
});
