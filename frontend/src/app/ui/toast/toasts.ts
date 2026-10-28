import { LiveAnnouncer } from '@angular/cdk/a11y';
import { inject, Injectable, signal } from '@angular/core';

/** `danger` for what went wrong; `neutral` for the rest, such as «Liga creada». */
export type ToastTone = 'neutral' | 'danger';

export interface ToastOptions {
  message: string;
  tone?: ToastTone;
  /** A button that runs `run` and closes the toast, such as «Reintentar». */
  action?: { label: string; run: () => void };
  /** Called when the user closes the toast, or when it goes by itself. */
  dismissed?: () => void;
}

export interface Toast extends ToastOptions {
  readonly id: number;
  readonly tone: ToastTone;
}

/** Long enough to read a sentence; a toast with an action waits for the user (WCAG 2.2.1). */
const DURATION_MS = 5000;

/**
 * The toasts on screen (FE-18), which `bf-toaster` shows. Each one is also announced to screen
 * readers, `danger` ones at once.
 */
@Injectable({ providedIn: 'root' })
export class Toasts {
  private readonly announcer = inject(LiveAnnouncer);
  private readonly shown = signal<readonly Toast[]>([]);
  private nextId = 0;

  readonly toasts = this.shown.asReadonly();

  show(options: ToastOptions): number {
    const toast: Toast = { ...options, tone: options.tone ?? 'neutral', id: this.nextId++ };
    this.shown.update((toasts) => [...toasts, toast]);
    void this.announcer.announce(toast.message, toast.tone === 'danger' ? 'assertive' : 'polite');
    if (toast.action === undefined) {
      setTimeout(() => {
        this.dismiss(toast.id);
      }, DURATION_MS);
    }
    return toast.id;
  }

  /** Runs the toast's action and closes it, without calling `dismissed`. */
  act(id: number): void {
    this.remove(id)?.action?.run();
  }

  dismiss(id: number): void {
    this.remove(id)?.dismissed?.();
  }

  private remove(id: number): Toast | undefined {
    const toast = this.shown().find((shown) => shown.id === id);
    if (toast !== undefined) {
      this.shown.update((toasts) => toasts.filter((shown) => shown.id !== id));
    }
    return toast;
  }
}
