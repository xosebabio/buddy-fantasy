import { Pipe, type PipeTransform } from '@angular/core';

import { LOCALE } from '../../core/time/madrid-time';

/** `unit` «87,4 M», for a value in a row; `number` «87,4», for a figure with its unit apart. */
export type MoneyFormat = 'unit' | 'number';

const MILLIONS = new Intl.NumberFormat(LOCALE, {
  minimumFractionDigits: 1,
  maximumFractionDigits: 2,
});

/** An amount of the API, which comes in millions, in Spanish (decision 13 of plan 12). */
export function formatMoney(millions: number, moneyFormat: MoneyFormat = 'unit'): string {
  const amount = MILLIONS.format(millions);
  return moneyFormat === 'number'
    ? amount
    : $localize`:@@shared.money.millions:${amount}:amount: M`;
}

/**
 * An amount in millions (FE-19): `{{ player.currentValue | bfMoney }}` is «17,2 M», and
 * `{{ budget | bfMoney: 'number' }}` «87,4», for a `bf-figure` with the unit in `[bfFigureUnit]`.
 */
@Pipe({ name: 'bfMoney' })
export class BfMoneyPipe implements PipeTransform {
  transform(millions: number, moneyFormat: MoneyFormat = 'unit'): string {
    return formatMoney(millions, moneyFormat);
  }
}
