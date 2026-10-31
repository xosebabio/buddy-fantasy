import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { firstValueFrom, isObservable } from 'rxjs';

import { type HasUnsavedChanges, unsavedChangesGuard, warnBeforeUnload } from './unsaved-changes';

function page(unsaved: boolean): HasUnsavedChanges {
  return { unsaved: signal(unsaved) };
}

function leave(from: HasUnsavedChanges): Promise<unknown> {
  const state = TestBed.inject(Router).routerState.snapshot;
  const result = TestBed.runInInjectionContext(() =>
    unsavedChangesGuard(from, state.root, state, state),
  );
  if (!isObservable(result)) {
    throw new Error('The guard should answer with an observable');
  }
  return firstValueFrom(result);
}

describe('unsavedChangesGuard', () => {
  it('lets the user leave a page with nothing to save, without asking', async () => {
    await expect(leave(page(false))).resolves.toBe(true);

    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
  });

  it('asks first if there are unsaved changes, and leaves if the user confirms', async () => {
    const left = leave(page(true));

    expect(await screen.findByRole('alertdialog', { name: '¿Salir sin guardar?' })).toBeVisible();
    expect(screen.getByText('Tienes cambios sin guardar. Si sales, los perderás.')).toBeVisible();
    await userEvent.click(screen.getByRole('button', { name: 'Salir sin guardar' }));

    await expect(left).resolves.toBe(true);
  });

  it('stays on the page if the user cancels', async () => {
    const left = leave(page(true));

    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar' }));

    await expect(left).resolves.toBe(false);
  });
});

describe('warnBeforeUnload', () => {
  const unsaved = signal(false);

  @Component({ selector: 'bf-test-page', template: '' })
  class Page {
    constructor() {
      warnBeforeUnload(unsaved);
    }
  }

  function closeTab(): Event {
    const event = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(event);
    return event;
  }

  it('asks the browser to confirm closing the tab only with unsaved changes', async () => {
    await render(Page);

    unsaved.set(false);
    expect(closeTab().defaultPrevented).toBe(false);

    unsaved.set(true);
    expect(closeTab().defaultPrevented).toBe(true);
  });

  it('stops warning when the page is destroyed', async () => {
    const { fixture } = await render(Page);
    unsaved.set(true);

    fixture.destroy();

    expect(closeTab().defaultPrevented).toBe(false);
  });
});
