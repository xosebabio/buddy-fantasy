import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';

import { Toasts } from '../../ui/toast/toasts';
import { FailedRequests } from './failed-requests';
import { showFailedRequests } from './failed-requests-notice';

describe('showFailedRequests', () => {
  let failedRequests: FailedRequests;
  let toasts: Toasts;

  beforeEach(() => {
    failedRequests = TestBed.inject(FailedRequests);
    toasts = TestBed.inject(Toasts);
    TestBed.runInInjectionContext(showFailedRequests);
  });

  function fail(): { retried: () => boolean; gaveUp: () => boolean } {
    let retried = false;
    let gaveUp = false;
    failedRequests.waitForRetry(new HttpErrorResponse({ status: 503 })).subscribe({
      next: () => (retried = true),
      error: () => (gaveUp = true),
    });
    TestBed.tick();
    return { retried: () => retried, gaveUp: () => gaveUp };
  }

  function shownToast() {
    const [toast, ...others] = toasts.toasts();
    expect(others).toEqual([]);
    return toast;
  }

  it('shows one toast with retry while requests wait', () => {
    fail();
    fail();

    const toast = shownToast();
    expect(toast).toMatchObject({
      message: 'No se pudo conectar con el servidor.',
      tone: 'danger',
      action: { label: 'Reintentar' },
    });
  });

  it('sends the requests again when the user retries', () => {
    const request = fail();

    toasts.act(shownToast()?.id ?? -1);

    expect(request.retried()).toBe(true);
    expect(toasts.toasts()).toEqual([]);
  });

  it('gives the requests up when the user closes it', () => {
    const request = fail();

    toasts.dismiss(shownToast()?.id ?? -1);

    expect(request.gaveUp()).toBe(true);
  });

  it('shows it again when a retried request fails again', () => {
    fail();
    toasts.act(shownToast()?.id ?? -1);
    TestBed.tick();

    fail();

    expect(shownToast()).toBeDefined();
  });
});
