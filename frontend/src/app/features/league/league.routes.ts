import { Routes } from '@angular/router';

import { authGuard } from '../../core/auth/guards';

/** The pages outside a league, in the shell. `leagues/new` goes before `leagues/:leagueId`. */
export const leaguesRoutes: Routes = [
  {
    path: 'leagues',
    canActivate: [authGuard],
    loadComponent: () => import('./leagues-page').then((m) => m.LeaguesPage),
    title: $localize`:@@league.leagues.pageTitle:Tus ligas`,
  },
  {
    path: 'leagues/new',
    canActivate: [authGuard],
    loadComponent: () => import('./create-league-page').then((m) => m.CreateLeaguePage),
    title: $localize`:@@league.create.pageTitle:Crear liga`,
  },
  {
    path: 'invite/:token',
    canActivate: [authGuard],
    loadComponent: () => import('./invitation-page').then((m) => m.InvitationPage),
    title: $localize`:@@league.invitation.pageTitle:Invitación`,
  },
];

/** The pages of a league, under `leagues/:leagueId`. */
export const leagueRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./league-home-page').then((m) => m.LeagueHomePage),
    title: $localize`:@@league.home.pageTitle:Jornada`,
  },
  {
    path: 'league',
    loadComponent: () => import('./league-index-page').then((m) => m.LeagueIndexPage),
    title: $localize`:@@league.index.pageTitle:Liga`,
  },
  {
    path: 'rules',
    loadComponent: () => import('./rules-page').then((m) => m.RulesPage),
    title: $localize`:@@league.rules.pageTitle:Reglas`,
  },
  {
    path: 'members',
    loadComponent: () => import('./members-page').then((m) => m.MembersPage),
    title: $localize`:@@league.members.pageTitle:Miembros`,
  },
];

/** The admin pages of a league, under `leagues/:leagueId/admin`. */
export const leagueAdminRoutes: Routes = [
  {
    path: 'members',
    loadComponent: () => import('./admin/admin-members-page').then((m) => m.AdminMembersPage),
    title: $localize`:@@league.admin.members.pageTitle:Miembros e invitación`,
  },
];
