import { TestBed } from '@angular/core/testing';

import { ReturnUrl } from './return-url';

describe('ReturnUrl', () => {
  let returnUrl: ReturnUrl;

  beforeEach(() => {
    sessionStorage.clear();
    returnUrl = TestBed.inject(ReturnUrl);
  });

  it('gives back the saved route only once', () => {
    returnUrl.save('/invite/tok?x=1');

    expect(returnUrl.consume()).toBe('/invite/tok?x=1');
    expect(returnUrl.consume()).toBeNull();
  });

  it('has no route when none was saved', () => {
    expect(returnUrl.consume()).toBeNull();
  });

  it.each(['https://evil.example', '//evil.example', '/\\evil.example', 'leagues'])(
    'never saves %s, which is not a route of the app',
    (url) => {
      returnUrl.save(url);

      expect(returnUrl.consume()).toBeNull();
    },
  );

  it('ignores a stored value that is not a route of the app', () => {
    sessionStorage.setItem('bf.returnUrl', '//evil.example');

    expect(returnUrl.consume()).toBeNull();
  });
});
