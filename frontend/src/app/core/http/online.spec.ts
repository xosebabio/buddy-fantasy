import { TestBed } from '@angular/core/testing';

import { Online } from './online';

describe('Online', () => {
  let onLine: boolean;

  beforeEach(() => {
    onLine = true;
    vi.spyOn(navigator, 'onLine', 'get').mockImplementation(() => onLine);
  });

  function goOffline(): void {
    onLine = false;
    window.dispatchEvent(new Event('offline'));
  }

  it('starts from what the browser says', () => {
    onLine = false;

    expect(TestBed.inject(Online).online()).toBe(false);
  });

  it('follows the browser when the network goes and comes back', () => {
    const online = TestBed.inject(Online);

    goOffline();
    expect(online.online()).toBe(false);

    onLine = true;
    window.dispatchEvent(new Event('online'));
    expect(online.online()).toBe(true);
  });
});
