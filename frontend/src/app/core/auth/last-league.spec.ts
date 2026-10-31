import { TestBed } from '@angular/core/testing';

import { LastLeague } from './last-league';

describe('LastLeague', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('remembers the last league saved', () => {
    const lastLeague = TestBed.inject(LastLeague);

    lastLeague.save('l1');
    lastLeague.save('l2');

    expect(lastLeague.get()).toBe('l2');
  });

  it('has no league before any is saved', () => {
    expect(TestBed.inject(LastLeague).get()).toBeNull();
  });
});
