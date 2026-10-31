import { Pipe, type PipeTransform } from '@angular/core';

import { LOCALE } from '../../core/time/madrid-time';

const SIGNED = new Intl.NumberFormat(LOCALE, { signDisplay: 'exceptZero' });
const UNSIGNED = new Intl.NumberFormat(LOCALE);

/**
 * Points with their sign and the typographic minus, «+14», «−3» and «0» (decision 13 of plan 12);
 * without `signed`, as the totals of the standings, «342».
 */
export function formatPoints(points: number, signed = true): string {
  return (signed ? SIGNED : UNSIGNED).format(points).replace('-', '−');
}

/** Points in text (FE-19): `{{ 14 | bfPoints }}` is «+14». Their color is `bf-points`'. */
@Pipe({ name: 'bfPoints' })
export class BfPointsPipe implements PipeTransform {
  transform(points: number, signed = true): string {
    return formatPoints(points, signed);
  }
}
