import { ChangeDetectionStrategy, Component } from '@angular/core';
import {
  LucideChevronDown,
  LucideClock,
  LucideHouse,
  LucidePlus,
  LucideShirt,
  LucideStar,
  LucideTrophy,
  LucideUsers,
} from '@lucide/angular';

@Component({
  selector: 'bf-catalog-page',
  imports: [
    LucideChevronDown,
    LucideClock,
    LucideHouse,
    LucidePlus,
    LucideShirt,
    LucideStar,
    LucideTrophy,
    LucideUsers,
  ],
  templateUrl: './catalog-page.html',
  styleUrl: './catalog-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CatalogPage {
  protected readonly themes = [
    { id: 'light', name: 'Claro' },
    { id: 'dark', name: 'Oscuro' },
  ];

  /** The `--bf-color-*` tokens of ui/styles/_tokens.scss, without the prefix. */
  protected readonly colors = [
    { name: 'Superficies', tokens: ['bg', 'surface', 'border', 'border-strong', 'scrim'] },
    { name: 'Texto', tokens: ['text', 'text-muted', 'on-color'] },
    { name: 'Marca', tokens: ['brand', 'brand-text', 'brand-subtle'] },
    { name: 'Acento y foco', tokens: ['accent', 'accent-text', 'accent-subtle', 'focus'] },
    {
      name: 'Estados',
      tokens: ['danger', 'danger-text', 'warning', 'warning-text', 'warning-subtle'],
    },
    {
      name: 'Posiciones',
      tokens: [
        'position-goalkeeper',
        'position-defender',
        'position-midfielder',
        'position-forward',
      ],
    },
    {
      name: 'Campo',
      tokens: ['pitch', 'pitch-stripe', 'pitch-line', 'on-pitch', 'on-pitch-muted'],
    },
  ];

  protected readonly typeSteps = [8, 7, 6, 5, 4, 3, 2, 1];
}
