import { Routes } from '@angular/router';

import { authGuard, guestGuard } from '../../core/auth/guards';

/** The login, outside the shell: it has the whole screen. */
export const loginRoutes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./login-page').then((m) => m.LoginPage),
    title: $localize`:@@identity.login.pageTitle:Entrar`,
  },
];

/** The pages of the user, in the shell outside a league. */
export const identityRoutes: Routes = [
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () => import('./profile-page').then((m) => m.ProfilePage),
    title: $localize`:@@identity.profile.pageTitle:Tu perfil`,
  },
];
