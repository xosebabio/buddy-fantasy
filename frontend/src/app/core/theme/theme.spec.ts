import { TestBed } from '@angular/core/testing';

import { Theme } from './theme';

describe('Theme', () => {
  const root = document.documentElement;

  beforeEach(() => {
    localStorage.clear();
    root.removeAttribute('data-theme');
  });

  it('follows the system when nothing was chosen', () => {
    expect(TestBed.inject(Theme).preference()).toBe('system');
  });

  it('starts with the stored choice', () => {
    localStorage.setItem('bf.theme', 'dark');

    expect(TestBed.inject(Theme).preference()).toBe('dark');
  });

  it('ignores a stored value that is not a theme', () => {
    localStorage.setItem('bf.theme', 'sepia');

    expect(TestBed.inject(Theme).preference()).toBe('system');
  });

  it('applies and stores a fixed theme', () => {
    const theme = TestBed.inject(Theme);

    theme.set('dark');

    expect(theme.preference()).toBe('dark');
    expect(root).toHaveAttribute('data-theme', 'dark');
    expect(localStorage.getItem('bf.theme')).toBe('dark');
  });

  it('goes back to the system theme and forgets the choice', () => {
    const theme = TestBed.inject(Theme);
    theme.set('light');

    theme.set('system');

    expect(root).not.toHaveAttribute('data-theme');
    expect(localStorage.getItem('bf.theme')).toBeNull();
  });
});
