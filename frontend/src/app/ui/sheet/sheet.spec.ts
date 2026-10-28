import { DialogRef } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, inject, TemplateRef, viewChild } from '@angular/core';
import { fireEvent, render, screen, waitFor } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { Sheet, SheetClose, SheetTitle } from './sheet';

@Component({
  selector: 'bf-test-host',
  imports: [SheetTitle, SheetClose],
  template: `
    <button type="button" (click)="open()">Cambiar de liga</button>
    <ng-template #content let-leagues>
      <h2 bfSheetTitle>Cambiar de liga</h2>
      @for (league of leagues; track league) {
        <button type="button" [bfSheetClose]="league">{{ league }}</button>
      }
    </ng-template>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
class TestHost {
  private readonly sheet = inject(Sheet);
  private readonly content = viewChild.required<TemplateRef<unknown>>('content');

  ref?: DialogRef<string>;

  open(): void {
    this.ref = this.sheet.open<string, string[]>(this.content(), {
      data: ['Os Domingueiros', 'Veteranos do Sar'],
    });
  }
}

describe('Sheet', () => {
  // jsdom has no layout, so the CDK finds nothing visible to focus and focuses the dialog itself.
  it('opens a modal dialog named by its title, with the focus inside', async () => {
    await render(TestHost);

    await userEvent.click(screen.getByRole('button', { name: 'Cambiar de liga' }));

    const sheet = await screen.findByRole('dialog', { name: 'Cambiar de liga' });
    expect(sheet).toHaveAttribute('aria-modal', 'true');
    expect(sheet.closest('.bf-sheet')).not.toBeNull();
    await waitFor(() => {
      expect(sheet.contains(document.activeElement)).toBe(true);
    });
    expect(screen.getByRole('button', { name: 'Os Domingueiros' })).toBeInTheDocument();
  });

  it('closes with the result of the button and returns the focus', async () => {
    const { fixture } = await render(TestHost);
    const opener = screen.getByRole('button', { name: 'Cambiar de liga' });
    await userEvent.click(opener);
    const closed = vi.fn();
    fixture.componentInstance.ref?.closed.subscribe(closed);

    await userEvent.click(await screen.findByRole('button', { name: 'Veteranos do Sar' }));

    expect(closed).toHaveBeenCalledWith('Veteranos do Sar');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    await waitFor(() => {
      expect(opener).toHaveFocus();
    });
  });

  it('closes with Escape and no result', async () => {
    const { fixture } = await render(TestHost);
    await userEvent.click(screen.getByRole('button', { name: 'Cambiar de liga' }));
    await screen.findByRole('dialog');
    const closed = vi.fn();
    fixture.componentInstance.ref?.closed.subscribe(closed);

    // The CDK reads keyCode, which jsdom leaves at 0 in the events of user-event.
    fireEvent.keyDown(document.body, { key: 'Escape', keyCode: 27 });

    expect(closed).toHaveBeenCalledWith(undefined);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
