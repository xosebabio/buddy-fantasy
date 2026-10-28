import { LiveAnnouncer } from '@angular/cdk/a11y';
import { TestBed } from '@angular/core/testing';
import { render, screen, within } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { Toaster } from './toaster';
import { Toasts } from './toasts';

describe('Toaster', () => {
  let announce: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    announce = vi.fn().mockResolvedValue(undefined);
    TestBed.configureTestingModule({
      providers: [{ provide: LiveAnnouncer, useValue: { announce } }],
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows nothing without toasts', async () => {
    await render(Toaster);

    expect(screen.queryByRole('list')).not.toBeInTheDocument();
  });

  it('shows a toast and announces it politely', async () => {
    const { detectChanges } = await render(Toaster);

    TestBed.inject(Toasts).show({ message: 'Liga creada' });
    detectChanges();

    const list = screen.getByRole('list', { name: 'Avisos' });
    expect(within(list).getByText('Liga creada')).toBeInTheDocument();
    expect(announce).toHaveBeenCalledWith('Liga creada', 'polite');
  });

  it('announces an error at once', async () => {
    await render(Toaster);

    TestBed.inject(Toasts).show({ message: 'No se pudo conectar.', tone: 'danger' });

    expect(announce).toHaveBeenCalledWith('No se pudo conectar.', 'assertive');
  });

  it('goes by itself after a while and tells who opened it', async () => {
    vi.useFakeTimers();
    const dismissed = vi.fn();
    const { detectChanges } = await render(Toaster);
    TestBed.inject(Toasts).show({ message: 'Liga creada', dismissed });

    vi.advanceTimersByTime(5000);
    detectChanges();

    expect(screen.queryByText('Liga creada')).not.toBeInTheDocument();
    expect(dismissed).toHaveBeenCalledOnce();
  });

  it('closes with its button', async () => {
    const dismissed = vi.fn();
    const { detectChanges } = await render(Toaster);
    TestBed.inject(Toasts).show({ message: 'Liga creada', dismissed });
    detectChanges();

    await userEvent.click(screen.getByRole('button', { name: 'Cerrar aviso' }));

    expect(screen.queryByText('Liga creada')).not.toBeInTheDocument();
    expect(dismissed).toHaveBeenCalledOnce();
  });

  it('runs its action, closes and does not go by itself', async () => {
    vi.useFakeTimers();
    const run = vi.fn();
    const dismissed = vi.fn();
    const { detectChanges } = await render(Toaster);
    TestBed.inject(Toasts).show({
      message: 'No se pudo conectar.',
      tone: 'danger',
      action: { label: 'Reintentar', run },
      dismissed,
    });
    vi.advanceTimersByTime(60_000);
    detectChanges();

    screen.getByRole('button', { name: 'Reintentar' }).click();
    detectChanges();

    expect(run).toHaveBeenCalledOnce();
    expect(dismissed).not.toHaveBeenCalled();
    expect(screen.queryByText('No se pudo conectar.')).not.toBeInTheDocument();
  });
});
