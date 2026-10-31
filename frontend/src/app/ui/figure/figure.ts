import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** `medium` for the figures of a summary (paso 4), `large` for a position (paso 5). */
export type FigureSize = 'medium' | 'large';

/**
 * A figure with its label below (FE-18), such as `96,2 / 100 M` _Presupuesto_: the content is the
 * figure, in condensed with tabular numbers, and `[bfFigureUnit]` its unit or context, smaller and
 * muted. `danger` paints the figure in `danger-text` when it is wrong, such as a budget gone over;
 * the page says why next to it.
 */
@Component({
  selector: 'bf-figure',
  template: `
    <span class="value">
      <ng-content />
      <span class="unit"><ng-content select="[bfFigureUnit]" /></span>
    </span>
    &ngsp;<span class="label">{{ label() }}</span>
  `,
  styleUrl: './figure.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': 'size()', '[class.danger]': 'tone() === "danger"' },
})
export class Figure {
  readonly label = input.required<string>();
  readonly size = input<FigureSize>('medium');
  readonly tone = input<'neutral' | 'danger'>('neutral');
}
