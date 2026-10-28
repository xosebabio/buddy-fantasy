import { HttpClient, HttpContext, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import type { MockInstance } from 'vitest';

import { ReturnUrl } from '../auth/return-url';
import { errorInterceptor, handling } from './error-interceptor';
import { FailedRequests } from './failed-requests';

describe('errorInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;
  let router: Router;
  let navigate: MockInstance<Router['navigateByUrl']>;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => {
    http.verify();
  });

  function get(url: string, context?: HttpContext): Promise<unknown> {
    return firstValueFrom(client.get(url, { context }));
  }

  function fail(url: string, status: number): void {
    http.expectOne(url).flush({ status }, { status, statusText: 'Error' });
  }

  it('sends to the login on a 401, saving the route to go back to', async () => {
    vi.spyOn(router, 'url', 'get').mockReturnValue('/invite/tok');
    const result = get('/api/me/leagues');

    fail('/api/me/leagues', 401);

    await expect(result).rejects.toMatchObject({ status: 401 });
    expect(navigate).toHaveBeenCalledWith('/login');
    expect(TestBed.inject(ReturnUrl).consume()).toBe('/invite/tok');
  });

  it('does not save the login itself as the route to go back to', async () => {
    vi.spyOn(router, 'url', 'get').mockReturnValue('/login?error');
    const result = get('/api/me/leagues');

    fail('/api/me/leagues', 401);

    await expect(result).rejects.toMatchObject({ status: 401 });
    expect(TestBed.inject(ReturnUrl).consume()).toBeNull();
  });

  it.each([
    [403, '/forbidden'],
    [404, '/not-found'],
  ])('shows the page of a %i keeping the URL', async (status, page) => {
    const result = get('/api/leagues/l1');

    fail('/api/leagues/l1', status);

    await expect(result).rejects.toMatchObject({ status });
    expect(navigate).toHaveBeenCalledWith(page, { skipLocationChange: true });
  });

  it.each([400, 409])('leaves a %i to the page', async (status) => {
    const result = get('/api/leagues/l1');

    fail('/api/leagues/l1', status);

    await expect(result).rejects.toMatchObject({ status });
    expect(navigate).not.toHaveBeenCalled();
  });

  it('leaves to the page the statuses it handles', async () => {
    const result = get('/api/invitations/tok', handling(404));

    fail('/api/invitations/tok', 404);

    await expect(result).rejects.toMatchObject({ status: 404 });
    expect(navigate).not.toHaveBeenCalled();
  });

  it('leaves other origins alone', async () => {
    const result = get('https://bucket.example/photo');

    fail('https://bucket.example/photo', 403);

    await expect(result).rejects.toMatchObject({ status: 403 });
    expect(navigate).not.toHaveBeenCalled();
  });

  it('holds a 5xx until the user retries, and sends the request again', async () => {
    const failedRequests = TestBed.inject(FailedRequests);
    const result = get('/api/me/leagues');

    fail('/api/me/leagues', 503);
    expect(failedRequests.pending()).toBe(true);

    failedRequests.retry();
    http.expectOne('/api/me/leagues').flush([]);

    expect(await result).toEqual([]);
    expect(failedRequests.pending()).toBe(false);
  });

  it('fails with the original error of a network failure when the user gives up', async () => {
    const failedRequests = TestBed.inject(FailedRequests);
    const result = get('/api/me/leagues');

    http.expectOne('/api/me/leagues').error(new ProgressEvent('error'));
    failedRequests.dismiss();

    await expect(result).rejects.toMatchObject({ status: 0 });
    expect(failedRequests.pending()).toBe(false);
    expect(navigate).not.toHaveBeenCalled();
  });
});
