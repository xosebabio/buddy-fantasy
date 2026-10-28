import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'bf-catalog-page',
  templateUrl: './catalog-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CatalogPage {}
