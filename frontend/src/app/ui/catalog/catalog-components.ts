import { CdkAccordion } from '@angular/cdk/accordion';
import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { IsActiveMatchOptions, RouterLink } from '@angular/router';
import {
  LucideBan,
  LucideCheck,
  LucideClock,
  LucideDynamicIcon,
  LucideHouse,
  LucideIcon,
  LucideLoaderCircle,
  LucidePlus,
  LucideShare2,
  LucideShirt,
  LucideStar,
  LucideTrash,
  LucideTrophy,
  LucideUsers,
  LucideUserX,
  LucideX,
} from '@lucide/angular';

import { Avatar } from '../avatar/avatar';
import { Badge } from '../badge/badge';
import { Button } from '../button/button';
import { IconButton } from '../button/icon-button';
import { Card } from '../card/card';
import { Countdown } from '../countdown/countdown';
import { Disclosure } from '../disclosure/disclosure';
import { EmptyState } from '../empty-state/empty-state';
import { Field, FieldInput, FieldSelect } from '../field/field';
import { Figure } from '../figure/figure';
import { List, ListItem } from '../list/list';
import { LoadError } from '../load-error/load-error';
import { NavBar, NavItem, SideNav } from '../nav/nav';
import { Notice } from '../notice/notice';
import { Segmented, type SegmentedOption } from '../segmented/segmented';
import { Skeleton } from '../skeleton/skeleton';
import { Stepper } from '../stepper/stepper';
import { Tab, TabLink, TabNav, TabPanel, Tabs } from '../tabs/tabs';
import { TopBar } from '../top-bar/top-bar';
import { Wordmark } from '../top-bar/wordmark';

interface NavDemo {
  id: string;
  label: string;
  icon: LucideIcon;
}

/** The components of ui/ in all their states, shown once per theme by the catalog page. */
@Component({
  selector: 'bf-catalog-components',
  imports: [
    Avatar,
    Badge,
    Button,
    Card,
    CdkAccordion,
    Countdown,
    Disclosure,
    EmptyState,
    Field,
    FieldInput,
    FieldSelect,
    Figure,
    IconButton,
    List,
    ListItem,
    LoadError,
    LucideBan,
    LucideCheck,
    LucideClock,
    LucideDynamicIcon,
    LucideLoaderCircle,
    LucidePlus,
    LucideShare2,
    LucideTrash,
    LucideUserX,
    LucideX,
    NavBar,
    NavItem,
    Notice,
    RouterLink,
    Segmented,
    SideNav,
    Skeleton,
    Stepper,
    Tab,
    TabLink,
    TabNav,
    TabPanel,
    Tabs,
    TopBar,
    Wordmark,
  ],
  templateUrl: './catalog-components.html',
  styleUrl: './catalog-components.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CatalogComponents {
  protected readonly members = [
    { name: 'Xosé Babío', role: 'Admin' },
    { name: 'Brais Otero', role: 'Manager' },
    { name: 'Iago', role: 'Manager' },
  ];

  protected readonly themes: readonly SegmentedOption<string>[] = [
    { value: 'system', label: 'Automático' },
    { value: 'light', label: 'Claro' },
    { value: 'dark', label: 'Oscuro' },
  ];
  protected readonly theme = signal('system');

  protected readonly tab = signal('managers');

  /** The tabs of a route, linked to /_ui with a query parameter each, like the navigation. */
  protected readonly tabLinks = [
    { id: 'general', label: 'General' },
    { id: 'matchday', label: 'Por jornada' },
  ];

  protected readonly mark = signal<number | null>(7);
  protected readonly blankMark = signal<number | null>(null);

  /** Deadlines from when the catalog opens: far, under 3 h (amber) and in the last minute. */
  private readonly opened = Date.now();
  protected readonly deadlines = {
    far: this.at(28 * 60 + 5),
    near: this.at(2 * 60 + 40),
    last: this.at(0.5),
  };

  /** A green pixel, for an avatar with a picture without going to the network. */
  protected readonly picture =
    'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGPIPrEQAAN2AdU6wMdjAAAAAElFTkSuQmCC';

  /** The destinations of FE-30, linked to /_ui with a query parameter each. */
  protected readonly navItems: NavDemo[] = [
    { id: 'matchday', label: 'Jornada', icon: LucideHouse },
    { id: 'lineup', label: 'Alineación', icon: LucideShirt },
    { id: 'rate', label: 'Valorar', icon: LucideStar },
    { id: 'standings', label: 'Clasificación', icon: LucideTrophy },
    { id: 'league', label: 'Liga', icon: LucideUsers },
  ];

  protected readonly navActive: IsActiveMatchOptions = {
    paths: 'exact',
    queryParams: 'exact',
    fragment: 'ignored',
    matrixParams: 'ignored',
  };

  /** The first destination is the page without parameters, so that one is active on arrival. */
  protected navParams(item: NavDemo): Record<string, string> {
    return item === this.navItems[0] ? {} : { nav: item.id };
  }

  protected tabParams(id: string): Record<string, string> {
    return id === this.tabLinks[0]?.id ? {} : { tab: id };
  }

  private at(minutes: number): string {
    return new Date(this.opened + minutes * 60_000).toISOString();
  }
}
