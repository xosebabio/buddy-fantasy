import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { FailedRequests } from './failed-requests';

describe('FailedRequests', () => {
  let failedRequests: FailedRequests;

  beforeEach(() => {
    failedRequests = TestBed.inject(FailedRequests);
  });

  it('has nothing pending at first', () => {
    expect(failedRequests.pending()).toBe(false);
  });

  it('retries every request held at that moment with one answer', async () => {
    const first = firstValueFrom(failedRequests.waitForRetry(new HttpErrorResponse({ status: 0 })));
    const second = firstValueFrom(
      failedRequests.waitForRetry(new HttpErrorResponse({ status: 503 })),
    );
    expect(failedRequests.pending()).toBe(true);

    failedRequests.retry();

    await expect(Promise.all([first, second])).resolves.toEqual([undefined, undefined]);
    expect(failedRequests.pending()).toBe(false);
  });

  it('fails with the original error when the user gives up', async () => {
    const error = new HttpErrorResponse({ status: 502 });
    const waiting = firstValueFrom(failedRequests.waitForRetry(error));

    failedRequests.dismiss();

    await expect(waiting).rejects.toBe(error);
    expect(failedRequests.pending()).toBe(false);
  });

  it('stops holding a request whose caller unsubscribes', () => {
    const subscription = failedRequests
      .waitForRetry(new HttpErrorResponse({ status: 500 }))
      .subscribe();

    subscription.unsubscribe();

    expect(failedRequests.pending()).toBe(false);
  });
});
