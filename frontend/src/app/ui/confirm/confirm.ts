import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { Button } from '../button/button';
import { Sheet, SheetTitle } from '../sheet/sheet';

export interface ConfirmOptions {
  /** The question, with the verb of the action: «¿Expulsar a Brais?». */
  title: string;
  /** What happens if the user confirms, when the title is not enough. */
  message?: string;
  /** The text of the button that confirms, with the same verb: «Expulsar». */
  confirm: string;
  /** `danger` when it cannot be undone. */
  tone?: 'default' | 'danger';
}

interface ConfirmData extends ConfirmOptions {
  messageId: string;
}

let nextId = 0;

/**
 * Asks the user to confirm an action, in a bottom sheet (FE-18): an `alertdialog` that starts with
 * the focus on «Cancelar», the safe answer. Emits `true` if they confirm and `false` if they cancel
 * or close it, and then completes.
 */
@Injectable({ providedIn: 'root' })
export class Confirm {
  private readonly sheet = inject(Sheet);

  ask(options: ConfirmOptions): Observable<boolean> {
    const messageId = `bf-confirm-message-${String(nextId++)}`;
    return this.sheet
      .open<boolean, ConfirmData>(ConfirmDialog, {
        data: { ...options, messageId },
        role: 'alertdialog',
        autoFocus: '[data-cancel]',
        ariaDescribedBy: options.message === undefined ? undefined : messageId,
      })
      .closed.pipe(map((confirmed) => confirmed === true));
  }
}

@Component({
  selector: 'bf-confirm-dialog',
  imports: [Button, SheetTitle],
  template: `
    <h2 bfSheetTitle>{{ data.title }}</h2>
    @if (data.message) {
      <p class="message" [id]="data.messageId">{{ data.message }}</p>
    }
    <div class="actions">
      <button
        bfButton
        type="button"
        block
        [variant]="data.tone === 'danger' ? 'danger' : 'primary'"
        (click)="ref.close(true)"
      >
        {{ data.confirm }}
      </button>
      <button
        bfButton
        type="button"
        block
        variant="quiet"
        data-cancel
        (click)="ref.close(false)"
        i18n="@@ui.confirm.cancel"
      >
        Cancelar
      </button>
    </div>
  `,
  styleUrl: './confirm.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
class ConfirmDialog {
  protected readonly data = inject<ConfirmData>(DIALOG_DATA);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);
}
