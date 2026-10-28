import { ChangeDetectionStrategy, Component } from '@angular/core';
import { IsActiveMatchOptions, RouterLink } from '@angular/router';
import {
  LucideCheck,
  LucideClock,
  LucideDynamicIcon,
  LucideHouse,
  LucideIcon,
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
import { EmptyState } from '../empty-state/empty-state';
import { Field, FieldInput } from '../field/field';
import { List, ListItem } from '../list/list';
import { NavBar, NavItem, SideNav } from '../nav/nav';
import { Skeleton } from '../skeleton/skeleton';
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
    EmptyState,
    Field,
    FieldInput,
    IconButton,
    List,
    ListItem,
    LucideCheck,
    LucideClock,
    LucideDynamicIcon,
    LucidePlus,
    LucideShare2,
    LucideTrash,
    LucideUserX,
    LucideX,
    NavBar,
    NavItem,
    RouterLink,
    SideNav,
    Skeleton,
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
}
