import { FocusableOption, FocusKeyManager } from '@angular/cdk/a11y';
import {
  booleanAttribute,
  ChangeDetectionStrategy,
  Component,
  computed,
  contentChildren,
  DestroyRef,
  Directive,
  ElementRef,
  inject,
  Injector,
  input,
  model,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLinkActive } from '@angular/router';

let nextId = 0;

/**
 * Tabs inside a page (FE-18), such as _Managers_ and _Jugadores_: the `button[bfTab]` go in a
 * `tablist` named by `label`, and each `[bfTabPanel]` shows its content while its tab is the
 * `selected` one. The arrow keys, Home and End move between the tabs and select them (the CDK's
 * `FocusKeyManager`), and only the selected tab is in the tab order. They go on the left; tabs that
 * change the route are `bf-tab-nav` instead.
 */
@Component({
  selector: 'bf-tabs',
  template: `
    <div class="list" role="tablist" [attr.aria-label]="label()">
      <ng-content select="[bfTab]" />
    </div>
    <ng-content />
  `,
  styleUrl: './tabs.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.bleed]': 'bleed()' },
})
export class Tabs {
  readonly label = input.required<string>();
  readonly selected = model.required<string>();
  /** Cancels the side margin of the page, like `bfList`, so that the line reaches the edges. */
  readonly bleed = input(false, { transform: booleanAttribute });

  private readonly id = `bf-tabs-${String(nextId++)}`;
  private readonly tabs = contentChildren(Tab);
  private readonly keyManager = new FocusKeyManager(this.tabs, inject(Injector))
    .withHorizontalOrientation('ltr')
    .withVerticalOrientation(false)
    .withHomeAndEnd()
    .withWrap();

  constructor() {
    // The arrow keys select the tab they move to.
    this.keyManager.change.pipe(takeUntilDestroyed()).subscribe(() => {
      const tab = this.keyManager.activeItem;
      if (tab !== null) {
        this.selected.set(tab.value());
      }
    });
    inject(DestroyRef).onDestroy(() => {
      this.keyManager.destroy();
    });
  }

  tabId(value: string): string {
    return `${this.id}-tab-${value}`;
  }

  panelId(value: string): string {
    return `${this.id}-panel-${value}`;
  }

  select(tab: Tab): void {
    this.selected.set(tab.value());
    this.keyManager.updateActiveItem(tab);
  }

  focused(tab: Tab): void {
    this.keyManager.updateActiveItem(tab);
  }

  onKeydown(event: KeyboardEvent): void {
    this.keyManager.onKeydown(event);
  }
}

/** A tab of `bf-tabs`; `bfTab` is the value that selects it, and its panel's. */
@Component({
  selector: 'button[bfTab]',
  template: '<ng-content />',
  styleUrl: './tab.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    type: 'button',
    role: 'tab',
    '[id]': 'tabs.tabId(value())',
    '[attr.aria-controls]': 'tabs.panelId(value())',
    '[attr.aria-selected]': 'active()',
    '[attr.tabindex]': 'active() ? 0 : -1',
    '[class.active]': 'active()',
    '(click)': 'tabs.select(this)',
    '(focus)': 'tabs.focused(this)',
    '(keydown)': 'tabs.onKeydown($event)',
  },
})
export class Tab implements FocusableOption {
  readonly value = input.required<string>({ alias: 'bfTab' });

  protected readonly tabs = inject(Tabs);
  protected readonly active = computed(() => this.tabs.selected() === this.value());
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

  focus(): void {
    this.element.focus();
  }
}

/**
 * The content of a tab of `bf-tabs`, inside it: hidden while its tab, the one with the same value,
 * is not selected. The page can leave it out from `lg`, where both contents fit side by side.
 */
@Directive({
  selector: '[bfTabPanel]',
  host: {
    role: 'tabpanel',
    tabindex: '0',
    '[id]': 'tabs.panelId(value())',
    '[attr.aria-labelledby]': 'tabs.tabId(value())',
    '[hidden]': 'tabs.selected() !== value()',
  },
})
export class TabPanel {
  readonly value = input.required<string>({ alias: 'bfTabPanel' });

  protected readonly tabs = inject(Tabs);
}

/**
 * Tabs that change the route (FE-18), such as _General_ and _Por jornada_: a navigation named by
 * `label` whose `a[bfTabLink]` share the width.
 */
@Component({
  selector: 'bf-tab-nav',
  template: '<nav class="list" [attr.aria-label]="label()"><ng-content /></nav>',
  styleUrl: './tabs.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.bleed]': 'bleed()' },
})
export class TabNav {
  readonly label = input.required<string>();
  readonly bleed = input(false, { transform: booleanAttribute });
}

/**
 * A tab of `bf-tab-nav`: a `routerLink`, active with `aria-current="page"` while its route is. Pass
 * `[routerLinkActiveOptions]="{ exact: true }"` to the one whose route contains the others'.
 */
@Component({
  selector: 'a[bfTabLink]',
  template: '<ng-content />',
  styleUrl: './tab.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [{ directive: RouterLinkActive, inputs: ['routerLinkActiveOptions'] }],
  host: {
    class: 'fill',
    '[class.active]': 'active()',
    '[attr.aria-current]': 'active() ? "page" : null',
  },
})
export class TabLink {
  protected readonly active = signal(false);

  constructor() {
    inject(RouterLinkActive)
      .isActiveChange.pipe(takeUntilDestroyed())
      .subscribe((active) => {
        this.active.set(active);
      });
  }
}
