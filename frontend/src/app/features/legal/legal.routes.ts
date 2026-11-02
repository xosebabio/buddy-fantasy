import { Routes } from '@angular/router';

/**
 * The legal pages (RNF-10), outside the shell and without a guard: anyone can read them, with or
 * without a session. They are fixed texts, so they have no API calls either.
 */
export const legalRoutes: Routes = [
  {
    path: 'privacy',
    loadComponent: () => import('./privacy-page').then((m) => m.PrivacyPage),
    title: $localize`:@@legal.privacy.pageTitle:Política de privacidad`,
  },
  {
    path: 'terms',
    loadComponent: () => import('./terms-page').then((m) => m.TermsPage),
    title: $localize`:@@legal.terms.pageTitle:Condiciones de servicio`,
  },
];
