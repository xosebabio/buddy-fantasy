import { HttpErrorResponse } from '@angular/common/http';

import { toProblem } from './problem';

function errorWith(body: unknown, status = 409): HttpErrorResponse {
  return new HttpErrorResponse({ error: body, status });
}

describe('toProblem', () => {
  it('reads the status, code and violations of a ProblemDetail', () => {
    const problem = toProblem(
      errorWith({
        type: 'about:blank',
        title: 'Conflict',
        status: 409,
        detail: 'The league always keeps its admin',
        code: 'ADMIN_CANNOT_LEAVE',
      }),
    );

    expect(problem).toEqual({ status: 409, code: 'ADMIN_CANNOT_LEAVE', violations: null });
  });

  it('reads the violations of a validation error', () => {
    const problem = toProblem(errorWith({ status: 400, violations: ['name: must not be blank'] }));

    expect(problem).toEqual({
      status: 400,
      code: null,
      violations: ['name: must not be blank'],
    });
  });

  it('leaves out a code that this build does not know', () => {
    expect(toProblem(errorWith({ status: 409, code: 'NEW_CODE' }))?.code).toBeNull();
  });

  it('leaves out violations that are not a list of texts', () => {
    expect(toProblem(errorWith({ status: 400, violations: [1, 2] }))?.violations).toBeNull();
  });

  it.each([
    ['a text body', '<html>Bad Gateway</html>'],
    ['an empty body', null],
    ['a body without status', { code: 'SIZE' }],
    ['a body with a status that is not a number', { status: '409' }],
  ])('is undefined for %s', (_case, body) => {
    expect(toProblem(errorWith(body))).toBeUndefined();
  });

  it('is undefined for an error that is not an HTTP one', () => {
    expect(toProblem(new Error('boom'))).toBeUndefined();
  });
});
