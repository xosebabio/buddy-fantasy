// Writes src/app/ui/styles/_palette.scss: the base colors of FE-25 and their 50–900 scales, generated
// in OKLCH (decision 6 of plan 11). Run it with `npm run palette` after changing a color here; the
// contrast test (ui/styles/contrast.spec.ts) then checks the semantic tokens built on top.
import { writeFileSync } from 'node:fs';

const OUTPUT = new URL('../src/app/ui/styles/_palette.scss', import.meta.url);

/** FE-25: brand, secondary and one per position; plus the error red and the warning amber. */
const BASES = {
  green: ['#00a375', 'Brand and main action; also success and positive points.'],
  lavender: ['#7a86ee', 'Secondary: selection, links and focus.'],
  terracotta: ['#e2794e', 'Goalkeeper.'],
  blue: ['#009dc2', 'Defender.'],
  olive: ['#84941d', 'Midfielder.'],
  pink: ['#de5ea5', 'Forward.'],
  red: ['#d23b32', 'Errors and negative points.'],
  amber: ['#f2b33d', 'Deadlines about to expire.'],
};

/**
 * Lightness of each step, the same for every hue, so a step means the same contrast in all of them
 * (700 is text on white, 900 a dark background).
 */
const LIGHTNESS = {
  50: 0.96,
  100: 0.925,
  200: 0.87,
  300: 0.765,
  400: 0.7,
  500: 0.625,
  600: 0.56,
  700: 0.48,
  800: 0.4,
  900: 0.28,
};

/** Share of the base chroma at each step: the ends are softer; the gamut may cut it further. */
const CHROMA = {
  50: 0.2,
  100: 0.35,
  200: 0.55,
  300: 0.8,
  400: 0.95,
  500: 1,
  600: 1,
  700: 0.95,
  800: 0.75,
  900: 0.35,
};

/**
 * Warm neutrals for backgrounds, surfaces, borders and text, as [lightness, chroma] at one hue: 900
 * is the near black of text in the light theme and the surface of the dark one, 950 the background
 * of the dark one. 50 is light enough for the base green and lavender to reach 3:1 on it.
 */
const NEUTRAL_HUE = 85;
const NEUTRAL = {
  50: [0.975, 0.004],
  100: [0.947, 0.006],
  200: [0.907, 0.009],
  300: [0.808, 0.013],
  400: [0.717, 0.015],
  500: [0.61, 0.014],
  600: [0.495, 0.013],
  700: [0.41, 0.011],
  800: [0.306, 0.008],
  900: [0.222, 0.005],
  950: [0.182, 0.002],
};

/** Colors chosen by hand: the striped grass of the pitch, the same in both themes. */
const FIXED = {
  white: '#ffffff',
  black: '#000000',
  pitch: '#123d29',
  'pitch-stripe': '#164530',
};

function hexToRgb(hex) {
  const value = Number.parseInt(hex.slice(1), 16);
  return [(value >> 16) & 255, (value >> 8) & 255, value & 255].map((channel) => channel / 255);
}

function rgbToHex(rgb) {
  const hex = rgb.map((channel) =>
    Math.round(Math.min(1, Math.max(0, channel)) * 255)
      .toString(16)
      .padStart(2, '0'),
  );
  // Stylelint's color-hex-length asks for the short form when there is one.
  const short = hex.every((pair) => pair[0] === pair[1]);
  return `#${short ? hex.map((pair) => pair[0]).join('') : hex.join('')}`;
}

const toLinear = (c) => (c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4);
const toGamma = (c) => (c <= 0.0031308 ? 12.92 * c : 1.055 * c ** (1 / 2.4) - 0.055);

/** sRGB (0–1) to OKLCH, from https://bottosson.github.io/posts/oklab/. */
function rgbToOklch(rgb) {
  const [r, g, b] = rgb.map(toLinear);
  const l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
  const m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
  const s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
  const L = 0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s;
  const A = 1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s;
  const B = 0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s;
  return { l: L, c: Math.hypot(A, B), h: (Math.atan2(B, A) * 180) / Math.PI };
}

/** OKLCH to sRGB (0–1), unclamped: a channel outside 0–1 means the color is out of gamut. */
function oklchToRgb({ l: L, c, h }) {
  const A = c * Math.cos((h * Math.PI) / 180);
  const B = c * Math.sin((h * Math.PI) / 180);
  const l = (L + 0.3963377774 * A + 0.2158037573 * B) ** 3;
  const m = (L - 0.1055613458 * A - 0.0638541728 * B) ** 3;
  const s = (L - 0.0894841775 * A - 1.291485548 * B) ** 3;
  return [
    4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
    -0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s,
  ].map(toGamma);
}

const inGamut = (rgb) => rgb.every((channel) => channel >= -1e-4 && channel <= 1 + 1e-4);

/** The color at that lightness and hue with the most chroma up to `c` that fits in sRGB. */
function fitToGamut({ l, c, h }) {
  if (inGamut(oklchToRgb({ l, c, h }))) {
    return oklchToRgb({ l, c, h });
  }
  let low = 0;
  let high = c;
  for (let i = 0; i < 30; i++) {
    const mid = (low + high) / 2;
    if (inGamut(oklchToRgb({ l, c: mid, h }))) {
      low = mid;
    } else {
      high = mid;
    }
  }
  return oklchToRgb({ l, c: low, h });
}

function scale(base) {
  const { c, h } = rgbToOklch(hexToRgb(base));
  return Object.entries(LIGHTNESS).map(([step, l]) => [
    step,
    rgbToHex(fitToGamut({ l, c: c * CHROMA[step], h })),
  ]);
}

function neutralScale() {
  return Object.entries(NEUTRAL).map(([step, [l, c]]) => [
    step,
    rgbToHex(fitToGamut({ l, c, h: NEUTRAL_HUE })),
  ]);
}

const variables = (name, steps) => steps.map(([step, hex]) => `$${name}-${step}: ${hex};`);

const blocks = [
  [
    '// Fixed: black, white and the pitch.',
    ...Object.entries(FIXED).map(([name, hex]) => `$${name}: ${rgbToHex(hexToRgb(hex))};`),
  ],
  ['// Warm neutrals.', ...variables('neutral', neutralScale())],
  ...Object.entries(BASES).map(([name, [base, role]]) => [
    `// ${role}`,
    `$${name}: ${rgbToHex(hexToRgb(base))};`,
    ...variables(name, scale(base)),
  ]),
];

writeFileSync(
  OUTPUT,
  [
    '// Generated by scripts/palette.mjs (npm run palette): do not edit by hand.',
    '// Components never use these colors: they use the semantic tokens of _tokens.scss.',
    '',
    blocks.map((lines) => lines.join('\n')).join('\n\n'),
    '',
  ].join('\n'),
);
