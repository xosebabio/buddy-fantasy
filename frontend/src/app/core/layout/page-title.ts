import { inject, Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { type RouterStateSnapshot, TitleStrategy } from '@angular/router';

const APP_NAME = 'Buddy Fantasy';

/**
 * The tab of the browser names the page (WCAG 2.4.2): the `title` of the deepest route that has one,
 * followed by the app's name, or just the name.
 */
@Injectable()
export class PageTitle extends TitleStrategy {
  private readonly title = inject(Title);

  override updateTitle(snapshot: RouterStateSnapshot): void {
    const page = this.buildTitle(snapshot);
    this.title.setTitle(page === undefined ? APP_NAME : `${page} – ${APP_NAME}`);
  }
}
