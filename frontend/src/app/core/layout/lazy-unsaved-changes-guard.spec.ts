import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { firstValueFrom, isObservable, of } from 'rxjs';

import { Confirm } from '../../ui/confirm/confirm';
import { lazyUnsavedChangesGuard } from './lazy-unsaved-changes-guard';

function leave(unsaved: boolean): Promise<unknown> {
  const state = TestBed.inject(Router).routerState.snapshot;
  const result = TestBed.runInInjectionContext(() =>
    lazyUnsavedChangesGuard({ unsaved: signal(unsaved) }, state.root, state, state),
  );
  if (!isObservable(result)) {
    throw new Error('The guard should answer with an observable');
  }
  return firstValueFrom(result);
}

describe('lazyUnsavedChangesGuard', () => {
  it('lets the user leave a page with nothing to save, without asking', async () => {
    const ask = vi.fn(() => of(false));
    TestBed.configureTestingModule({ providers: [{ provide: Confirm, useValue: { ask } }] });

    await expect(leave(false)).resolves.toBe(true);

    expect(ask).not.toHaveBeenCalled();
  });

  it('asks to confirm when there are unsaved changes and follows the answer', async () => {
    const ask = vi.fn(() => of(false));
    TestBed.configureTestingModule({ providers: [{ provide: Confirm, useValue: { ask } }] });

    await expect(leave(true)).resolves.toBe(false);

    expect(ask).toHaveBeenCalledOnce();
  });
});
