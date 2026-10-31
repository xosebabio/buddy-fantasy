import { HttpErrorResponse } from '@angular/common/http';

import type { components } from '../api/schema';
import { isErrorCode } from './error-texts';

export type Problem = components['schemas']['Problem'];

/** What a page uses of a `ProblemDetail`: `detail` is for debugging and never shown. */
export type ProblemFields = Pick<Problem, 'status' | 'code' | 'violations'>;

/**
 * The `ProblemDetail` of a failed request (FE-08), checked at runtime because the body may come from
 * elsewhere (a proxy page, a network error); `undefined` if it is not one. A code that the spec of
 * this build does not know is left out, so the page shows its generic text.
 */
export function toProblem(error: unknown): ProblemFields | undefined {
  if (!(error instanceof HttpErrorResponse)) {
    return undefined;
  }
  const body: unknown = error.error;
  if (typeof body !== 'object' || body === null || !('status' in body)) {
    return undefined;
  }
  if (typeof body.status !== 'number') {
    return undefined;
  }
  return {
    status: body.status,
    code: 'code' in body && isErrorCode(body.code) ? body.code : null,
    violations: 'violations' in body && isStringArray(body.violations) ? body.violations : null,
  };
}

function isStringArray(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((item) => typeof item === 'string');
}

/**
 * Whether the request failed because a required field was missing (FE-15): a `400` whose
 * `violations` name it, such as `name: must not be blank`.
 */
export function violates(problem: ProblemFields | undefined, field: string): boolean {
  return problem?.violations?.some((violation) => violation.startsWith(`${field}:`)) ?? false;
}
