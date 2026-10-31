import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LucideCircleAlert, LucideInfo, LucideTriangleAlert } from '@lucide/angular';

/** `info` for what is worth knowing, `warning` for what will go wrong if left so, `danger` for errors. */
export type NoticeTone = 'info' | 'warning' | 'danger';

/**
 * A notice in the flow of the page (FE-18), such as _Te pasas 1,5 M del presupuesto_: an icon and the
 * text, with a bar of the tone's color on the side. Each tone has its icon; another one, such as the
 * loader of _Calculando los resultados_, goes as content (`<svg lucideLoaderCircle></svg>`). It is
 * not a live region: the page adds `role="status"` or `role="alert"` if it has to be announced.
 */
@Component({
  selector: 'bf-notice',
  imports: [LucideCircleAlert, LucideInfo, LucideTriangleAlert],
  template: `
    <span class="icon">
      <ng-content select="svg">
        @switch (tone()) {
          @case ('info') {
            <svg lucideInfo [size]="20"></svg>
          }
          @case ('warning') {
            <svg lucideTriangleAlert [size]="20"></svg>
          }
          @case ('danger') {
            <svg lucideCircleAlert [size]="20"></svg>
          }
        }
      </ng-content>
    </span>
    <div class="message"><ng-content /></div>
  `,
  styleUrl: './notice.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': 'tone()' },
})
export class Notice {
  readonly tone = input<NoticeTone>('info');
}
