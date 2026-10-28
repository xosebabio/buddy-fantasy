import type { paths } from './schema';

/** A path of the API as the spec writes it, such as `/api/leagues/{id}`. */
export type ApiPath = keyof paths;

/** Names of the `{param}` placeholders of a path. */
type PathParamNames<P extends string> = P extends `${string}{${infer Name}}${infer Rest}`
  ? Name | PathParamNames<Rest>
  : never;

/** The values of a path's placeholders, or no argument when it has none. */
type PathParams<P extends string> = [PathParamNames<P>] extends [never]
  ? []
  : [params: Readonly<Record<PathParamNames<P>, string>>];

/**
 * The URL of an endpoint from its path in the spec (FE-07): a renamed endpoint or parameter breaks
 * the build instead of failing at runtime.
 */
export function apiPath<P extends ApiPath>(path: P, ...params: PathParams<P>): string;
export function apiPath(path: string, params: Readonly<Record<string, string>> = {}): string {
  return path.replace(/\{(\w+)\}/g, (_placeholder, name: string) => {
    const value = params[name];
    if (value === undefined) {
      throw new Error(`Missing the ${name} parameter of ${path}`);
    }
    return encodeURIComponent(value);
  });
}
