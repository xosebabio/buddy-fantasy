import { Component } from '@angular/core';
import { render } from '@testing-library/angular';

import { onVisible } from './visible';

describe('onVisible', () => {
  let state: DocumentVisibilityState;
  let callback = vi.fn<() => void>();

  @Component({ selector: 'bf-test-page', template: '' })
  class Page {
    constructor() {
      onVisible(() => {
        callback();
      });
    }
  }

  beforeEach(() => {
    state = 'hidden';
    callback = vi.fn<() => void>();
    vi.spyOn(document, 'visibilityState', 'get').mockImplementation(() => state);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function change(to: DocumentVisibilityState): void {
    state = to;
    document.dispatchEvent(new Event('visibilitychange'));
  }

  it('calls back when the user comes back to the tab', async () => {
    await render(Page);

    change('visible');

    expect(callback).toHaveBeenCalledTimes(1);
  });

  it('does not call back when the user leaves the tab', async () => {
    await render(Page);

    change('hidden');

    expect(callback).not.toHaveBeenCalled();
  });

  it('stops listening when the page is destroyed', async () => {
    const { fixture } = await render(Page);

    fixture.destroy();
    change('visible');

    expect(callback).not.toHaveBeenCalled();
  });
});
