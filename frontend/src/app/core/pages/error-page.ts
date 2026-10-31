import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Button } from '../../ui/button/button';
import { EmptyState } from '../../ui/empty-state/empty-state';
import { ERROR_PAGES, type ErrorKind } from './error-page-texts';

/**
 * «Sin acceso» and «No encontrado» (FE-08), with the way back to the start. The interceptor and the
 * guards show them keeping the URL; `kind` comes from the route's data.
 */
@Component({
  selector: 'bf-error-page',
  imports: [Button, EmptyState, RouterLink],
  template: `
    <h1>{{ texts().title }}</h1>
    <bf-empty-state>
      {{ texts().message }}
      <a bfButton bfEmptyStateAction variant="primary" routerLink="/" i18n="@@core.errorPage.home">
        Ir al inicio
      </a>
    </bf-empty-state>
  `,
  styleUrl: './error-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorPage {
  readonly kind = input<ErrorKind>('not-found');

  protected readonly texts = computed(() => ERROR_PAGES[this.kind()]);
}
