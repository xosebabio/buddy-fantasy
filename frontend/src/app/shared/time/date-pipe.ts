import { Pipe, type PipeTransform } from '@angular/core';

import { type DateFormat, formatInstant } from '../../core/time/madrid-time';

/**
 * An instant of the API in Spanish and in Madrid time (FE-19, RNF-05):
 * `{{ match.kickoff | bfDate: 'dateTime' }}` is «domingo, 2 de noviembre, 11:00». Nothing for `null`.
 */
@Pipe({ name: 'bfDate' })
export class BfDatePipe implements PipeTransform {
  transform(
    instant: string | Date | null | undefined,
    dateFormat: DateFormat = 'dateTime',
  ): string {
    return instant === null || instant === undefined ? '' : formatInstant(instant, dateFormat);
  }
}
