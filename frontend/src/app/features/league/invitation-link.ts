import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DOCUMENT,
  inject,
  input,
} from '@angular/core';
import { LucideCopy, LucideShare2 } from '@lucide/angular';

import { Button } from '../../ui/button/button';
import { Field, FieldInput } from '../../ui/field/field';
import { Toasts } from '../../ui/toast/toasts';

/**
 * The invitation link of a league (RF-06), to pass to the team: with the share sheet of the device
 * where there is one (`navigator.share`) or else copied to the clipboard. It is also in a read-only
 * field, to copy it by hand.
 */
@Component({
  selector: 'bf-invitation-link',
  imports: [Button, Field, FieldInput, LucideCopy, LucideShare2],
  template: `
    <bf-field label="Enlace de invitación" i18n-label="@@league.invitationLink.label">
      <input bfInput type="url" readonly [value]="url()" (focus)="select($event)" />
    </bf-field>
    <button bfButton type="button" variant="primary" block (click)="share()">
      @if (canShare) {
        <svg lucideShare2 [size]="20"></svg>
        <span i18n="@@league.invitationLink.share">Compartir enlace</span>
      } @else {
        <svg lucideCopy [size]="20"></svg>
        <span i18n="@@league.invitationLink.copy">Copiar enlace</span>
      }
    </button>
  `,
  styleUrl: './invitation-link.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InvitationLink {
  readonly token = input.required<string>();
  readonly leagueName = input.required<string>();

  private readonly document = inject(DOCUMENT);
  private readonly navigator = this.document.defaultView?.navigator;
  private readonly toasts = inject(Toasts);

  protected readonly url = computed(
    () => `${this.document.location.origin}/invite/${encodeURIComponent(this.token())}`,
  );
  protected readonly canShare = this.navigator !== undefined && 'share' in this.navigator;

  protected select(event: FocusEvent): void {
    if (event.target instanceof HTMLInputElement) {
      event.target.select();
    }
  }

  protected share(): void {
    const url = this.url();
    if (this.canShare) {
      // Cancelling the share sheet rejects the promise: nothing to tell the user.
      void this.navigator
        ?.share({
          title: $localize`:@@league.invitationLink.shareTitle:Únete a ${this.leagueName()}:league:`,
          text: $localize`:@@league.invitationLink.shareText:Juega en ${this.leagueName()}:league: con nosotros en BuddyFantasy.`,
          url,
        })
        .catch(() => undefined);
      return;
    }
    void this.navigator?.clipboard.writeText(url).then(
      () =>
        this.toasts.show({ message: $localize`:@@league.invitationLink.copied:Enlace copiado` }),
      () =>
        this.toasts.show({
          message: $localize`:@@league.invitationLink.copyFailed:No se pudo copiar el enlace. Cópialo a mano.`,
          tone: 'danger',
        }),
    );
  }
}
