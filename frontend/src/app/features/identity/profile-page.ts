import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { LucideLogOut } from '@lucide/angular';

import { SessionStore } from '../../core/auth/session-store';
import { Online } from '../../core/http/online';
import { Theme, type ThemePreference } from '../../core/theme/theme';
import { Avatar } from '../../ui/avatar/avatar';
import { Button } from '../../ui/button/button';
import { Segmented, type SegmentedOption } from '../../ui/segmented/segmented';

const THEMES: readonly SegmentedOption<ThemePreference>[] = [
  { value: 'system', label: $localize`:@@identity.profile.theme.system:Automático` },
  { value: 'light', label: $localize`:@@identity.profile.theme.light:Claro` },
  { value: 'dark', label: $localize`:@@identity.profile.theme.dark:Oscuro` },
];

/**
 * The user's profile (RF-02): the name and picture of their Google account, which others see; the
 * theme of the app (FE-26); and the logout (RF-04).
 */
@Component({
  selector: 'bf-profile-page',
  imports: [Avatar, Button, LucideLogOut, Segmented],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePage {
  protected readonly session = inject(SessionStore);
  protected readonly theme = inject(Theme);
  protected readonly online = inject(Online);
  protected readonly themes = THEMES;
  protected readonly loggingOut = signal(false);

  protected logout(): void {
    this.loggingOut.set(true);
    this.session.logout().subscribe({
      error: () => {
        this.loggingOut.set(false);
      },
    });
  }
}
