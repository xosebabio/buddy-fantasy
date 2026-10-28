import { Dialog, DialogRef } from '@angular/cdk/dialog';
import { createGlobalPositionStrategy } from '@angular/cdk/overlay';
import { ComponentType } from '@angular/cdk/portal';
import {
  Directive,
  inject,
  Injectable,
  InjectionToken,
  Injector,
  input,
  TemplateRef,
} from '@angular/core';

export interface SheetConfig<D> {
  /** What the content receives with `inject(DIALOG_DATA)`, or as `$implicit` in a template. */
  data?: D;
  /** `alertdialog` when it asks for a decision, as the confirmation dialog does. */
  role?: 'dialog' | 'alertdialog';
  /** What takes the focus when it opens: the first focusable element, or this CSS selector. */
  autoFocus?: string;
  /** The id of the element that describes it, in its content. */
  ariaDescribedBy?: string;
}

/** The id of the sheet's title, which the `bfSheetTitle` directive puts on it. */
const SHEET_TITLE_ID = new InjectionToken<string>('SHEET_TITLE_ID');

let nextId = 0;

/**
 * Opens a component or a template in a bottom sheet (FE-18), which on mobile replaces dialogs: modal,
 * with the focus trapped inside and returned on close, and closed by the scrim, Escape or a
 * navigation. Its content has a title with `bfSheetTitle`, which names it, and closes it with
 * `inject(DialogRef).close(result)` or `bfSheetClose`. Its styles are in ui/styles/_overlay.scss.
 */
@Injectable({ providedIn: 'root' })
export class Sheet {
  private readonly dialog = inject(Dialog);
  private readonly injector = inject(Injector);

  open<R = unknown, D = unknown, C = unknown>(
    content: ComponentType<C> | TemplateRef<C>,
    config: SheetConfig<D> = {},
  ): DialogRef<R, C> {
    const titleId = `bf-sheet-title-${String(nextId++)}`;
    return this.dialog.open<R, D, C>(content, {
      data: config.data,
      role: config.role ?? 'dialog',
      ariaModal: true,
      ariaLabelledBy: titleId,
      ariaDescribedBy: config.ariaDescribedBy,
      autoFocus: config.autoFocus ?? 'first-tabbable',
      restoreFocus: true,
      panelClass: 'bf-sheet',
      backdropClass: 'bf-scrim',
      width: '100%',
      positionStrategy: createGlobalPositionStrategy(this.injector).centerHorizontally().bottom(),
      providers: [{ provide: SHEET_TITLE_ID, useValue: titleId }],
    });
  }
}

/** The title of a sheet's content: it names the sheet for screen readers. */
@Directive({
  selector: '[bfSheetTitle]',
  host: { class: 'bf-sheet-title', '[id]': 'id' },
})
export class SheetTitle {
  protected readonly id = inject(SHEET_TITLE_ID);
}

/** Closes the sheet it is in when clicked, with its value as the result. */
@Directive({
  selector: 'button[bfSheetClose]',
  host: { '(click)': 'ref.close(bfSheetClose())' },
})
export class SheetClose {
  readonly bfSheetClose = input<unknown>(undefined);

  protected readonly ref = inject(DialogRef);
}
