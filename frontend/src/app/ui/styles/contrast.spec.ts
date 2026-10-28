import palette from './_palette.scss' with { loader: 'text' };
import tokens from './_tokens.scss' with { loader: 'text' };

type Rgba = readonly [red: number, green: number, blue: number, alpha: number];
type Theme = 'light' | 'dark';

/**
 * Text and the background it goes on, by token name: WCAG 2.2 AA asks for 4.5:1 (RNF-12). They are
 * the uses of ui/README.md; a new pair of tokens in a component goes here too.
 */
const TEXT: [string, string][] = [
  ['text', 'bg'],
  ['text', 'surface'],
  ['text', 'brand-subtle'],
  ['text', 'accent-subtle'],
  ['text', 'warning-subtle'],
  ['text-muted', 'bg'],
  ['text-muted', 'surface'],
  ['brand-text', 'bg'],
  ['brand-text', 'surface'],
  ['brand-text', 'brand-subtle'],
  ['accent-text', 'bg'],
  ['accent-text', 'surface'],
  ['accent-text', 'accent-subtle'],
  ['danger-text', 'bg'],
  ['danger-text', 'surface'],
  ['warning-text', 'bg'],
  ['warning-text', 'surface'],
  ['warning-text', 'warning-subtle'],
  ['on-color', 'brand'],
  ['on-color', 'accent'],
  ['on-color', 'position-goalkeeper'],
  ['on-color', 'position-defender'],
  ['on-color', 'position-midfielder'],
  ['on-color', 'position-forward'],
  ['on-pitch', 'pitch'],
  ['on-pitch', 'pitch-stripe'],
  ['on-pitch-muted', 'pitch'],
  ['on-pitch-muted', 'pitch-stripe'],
];

/**
 * Graphics that carry information (focus, selection, the active destination, an error mark, the
 * outline of an input, the bibs on the pitch) and their background: 3:1. A bib in a list is decorative, so the position
 * colors are not checked on bg or surface (FE-25 puts them at about 3:1 on white).
 */
const GRAPHICS: [string, string][] = [
  ['focus', 'bg'],
  ['focus', 'surface'],
  ['accent', 'bg'],
  ['accent', 'surface'],
  ['accent', 'pitch'],
  ['accent', 'pitch-stripe'],
  ['brand', 'bg'],
  ['brand', 'surface'],
  ['border-strong', 'bg'],
  ['border-strong', 'surface'],
  ['danger', 'bg'],
  ['danger', 'surface'],
  ['position-goalkeeper', 'pitch'],
  ['position-defender', 'pitch'],
  ['position-midfielder', 'pitch'],
  ['position-forward', 'pitch'],
  ['position-goalkeeper', 'pitch-stripe'],
  ['position-defender', 'pitch-stripe'],
  ['position-midfielder', 'pitch-stripe'],
  ['position-forward', 'pitch-stripe'],
];

const PALETTE = new Map(
  [...palette.matchAll(/^\$([\w-]+): (#[0-9a-f]{3}|#[0-9a-f]{6});$/gm)].map(
    ([, name = '', hex = '']) => [name, hex],
  ),
);

const THEMES: Record<Theme, Map<string, Rgba>> = {
  light: themeColors('light'),
  dark: themeColors('dark'),
};

describe('Theme colors', () => {
  it('reads the palette and both themes', () => {
    expect(PALETTE.size).toBeGreaterThan(100);
    expect(THEMES.light.size).toBeGreaterThan(20);
  });

  it('defines the same color tokens in both themes', () => {
    expect([...THEMES.dark.keys()].sort()).toEqual([...THEMES.light.keys()].sort());
  });

  describe.each<Theme>(['light', 'dark'])('in the %s theme', (theme) => {
    it.each(TEXT)('%s on %s reaches 4.5:1', (text, background) => {
      expect(contrast(theme, text, background)).toBeGreaterThanOrEqual(4.5);
    });

    it.each(GRAPHICS)('%s on %s reaches 3:1', (graphic, background) => {
      expect(contrast(theme, graphic, background)).toBeGreaterThanOrEqual(3);
    });
  });
});

/** The `--bf-color-*` tokens of a theme's mixin in _tokens.scss, resolved against the palette. */
function themeColors(theme: Theme): Map<string, Rgba> {
  const body = new RegExp(`@mixin ${theme} \\{([\\s\\S]*?)\\n\\}`).exec(tokens)?.[1];
  if (body === undefined) {
    throw new Error(`_tokens.scss has no mixin ${theme}`);
  }
  return new Map(
    [...body.matchAll(/--bf-color-([\w-]+): #\{(.+)\};/g)].map(([, name = '', value = '']) => [
      name,
      resolve(value),
    ]),
  );
}

/** `p.$name` or `color.change(p.$name, $alpha: 0.5)`, the only forms _tokens.scss uses. */
function resolve(value: string): Rgba {
  const match = /^(?:p\.\$([\w-]+)|color\.change\(p\.\$([\w-]+), \$alpha: ([\d.]+)\))$/.exec(value);
  const name = match?.[1] ?? match?.[2];
  const hex = name === undefined ? undefined : PALETTE.get(name);
  if (match === null || hex === undefined) {
    throw new Error(`Cannot resolve the color ${value}`);
  }
  const digits = hex.slice(1);
  const rgb = Number.parseInt(digits.length === 3 ? digits.replace(/\w/g, '$&$&') : digits, 16);
  return [
    ((rgb >> 16) & 255) / 255,
    ((rgb >> 8) & 255) / 255,
    (rgb & 255) / 255,
    Number(match[3] ?? 1),
  ];
}

/** WCAG contrast ratio; a translucent foreground is composited over its opaque background. */
function contrast(theme: Theme, foreground: string, background: string): number {
  const fg = THEMES[theme].get(foreground);
  const bg = THEMES[theme].get(background);
  if (fg === undefined || bg?.[3] !== 1) {
    throw new Error(`No opaque pair ${foreground} on ${background} in the ${theme} theme`);
  }
  const alpha = fg[3];
  const blended: Rgba = [
    fg[0] * alpha + bg[0] * (1 - alpha),
    fg[1] * alpha + bg[1] * (1 - alpha),
    fg[2] * alpha + bg[2] * (1 - alpha),
    1,
  ];
  const [lighter, darker] = [luminance(blended), luminance(bg)].sort((a, b) => b - a);
  return ((lighter ?? 0) + 0.05) / ((darker ?? 0) + 0.05);
}

function luminance([red, green, blue]: Rgba): number {
  const linear = (channel: number) =>
    channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4;
  return 0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue);
}
