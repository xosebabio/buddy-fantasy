import { HttpErrorResponse } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { defer, finalize, Observable, of, Subject, switchMap, take, throwError } from 'rxjs';

/**
 * Requests that failed with a `5xx` or without connection, held until the user retries them or gives
 * up (FE-08). The shell shows a toast with retry while `pending` is true; one answer covers every
 * request held at that moment.
 */
@Injectable({ providedIn: 'root' })
export class FailedRequests {
  private readonly answers = new Subject<boolean>();
  private readonly held = signal(0);

  readonly pending = computed(() => this.held() > 0);

  /** Emits when the user retries; errors with the original error when they give up. */
  waitForRetry(error: HttpErrorResponse): Observable<void> {
    return defer(() => {
      this.held.update((count) => count + 1);
      return this.answers.pipe(
        take(1),
        switchMap((retry) => (retry ? of(undefined) : throwError(() => error))),
        finalize(() => {
          this.held.update((count) => count - 1);
        }),
      );
    });
  }

  retry(): void {
    this.answers.next(true);
  }

  dismiss(): void {
    this.answers.next(false);
  }
}
