import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  DOCUMENT,
  inject,
  signal,
  TemplateRef,
  viewChild,
} from '@angular/core';
import {
  LucideCheck,
  LucideChevronDown,
  LucideClock,
  LucideHouse,
  LucidePlus,
  LucideShirt,
  LucideStar,
  LucideTrophy,
  LucideUsers,
} from '@lucide/angular';

import { Button } from '../button/button';
import { Confirm } from '../confirm/confirm';
import { List, ListItem } from '../list/list';
import { Sheet, SheetClose, SheetTitle } from '../sheet/sheet';
import { Toaster } from '../toast/toaster';
import { Toasts } from '../toast/toasts';
import { CatalogComponents } from './catalog-components';

type PageTheme = 'system' | 'light' | 'dark';

@Component({
  selector: 'bf-catalog-page',
  imports: [
    Button,
    CatalogComponents,
    List,
    ListItem,
    LucideCheck,
    LucideChevronDown,
    LucideClock,
    LucideHouse,
    LucidePlus,
    LucideShirt,
    LucideStar,
    LucideTrophy,
    LucideUsers,
    SheetClose,
    SheetTitle,
    Toaster,
  ],
  templateUrl: './catalog-page.html',
  styleUrl: './catalog-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CatalogPage {
  private readonly root = inject(DOCUMENT).documentElement;
  private readonly sheet = inject(Sheet);
  private readonly confirm = inject(Confirm);
  private readonly toasts = inject(Toasts);
  private readonly switcher = viewChild.required<TemplateRef<unknown>>('switcher');

  protected readonly themes = [
    { id: 'light', name: 'Claro' },
    { id: 'dark', name: 'Oscuro' },
  ];

  /**
   * The theme of the whole page, for what the CDK draws outside the columns (sheets, dialogs and
   * toasts). It changes `<html>` without saving the choice, and leaves it as it was on exit.
   */
  protected readonly pageThemes: { id: PageTheme; name: string }[] = [
    { id: 'system', name: 'Sistema' },
    { id: 'light', name: 'Claro' },
    { id: 'dark', name: 'Oscuro' },
  ];
  protected readonly pageTheme = signal<PageTheme>('system');

  protected readonly leagues = [
    { name: 'Os Domingueiros', role: 'Admin', current: true },
    { name: 'Veteranos do Sar', role: 'Manager', current: false },
    { name: 'Fútbol 7 Martes Noche', role: 'Manager', current: false },
  ];
  protected readonly answer = signal<string | null>(null);

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

  constructor() {
    const initial = this.root.getAttribute('data-theme');
    if (initial === 'light' || initial === 'dark') {
      this.pageTheme.set(initial);
    }
    inject(DestroyRef).onDestroy(() => {
      this.applyTheme(initial);
    });
  }

  protected setPageTheme(theme: PageTheme): void {
    this.pageTheme.set(theme);
    this.applyTheme(theme === 'system' ? null : theme);
  }

  protected openSheet(): void {
    this.sheet.open<string>(this.switcher()).closed.subscribe((league) => {
      this.answer.set(league === undefined ? 'Hoja cerrada sin elegir' : `Elegida: ${league}`);
    });
  }

  protected openConfirm(): void {
    this.confirm
      .ask({
        title: '¿Expulsar a Brais?',
        message: 'Dejará de ver la liga. Podrá volver si le pasas otra vez la invitación.',
        confirm: 'Expulsar',
        tone: 'danger',
      })
      .subscribe((confirmed) => {
        this.answer.set(confirmed ? 'Confirmado' : 'Cancelado');
      });
  }

  protected showToast(): void {
    this.toasts.show({ message: 'Liga creada' });
  }

  protected showErrorToast(): void {
    this.toasts.show({
      message: 'No se pudo conectar con el servidor.',
      tone: 'danger',
      action: {
        label: 'Reintentar',
        run: () => {
          this.answer.set('Reintentado');
        },
      },
    });
  }

  private applyTheme(theme: string | null): void {
    if (theme === null) {
      this.root.removeAttribute('data-theme');
    } else {
      this.root.setAttribute('data-theme', theme);
    }
  }
}
