// @ts-check
const eslint = require('@eslint/js');
const { defineConfig, globalIgnores } = require('eslint/config');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');
const boundaries = require('eslint-plugin-boundaries');
const prettier = require('eslint-config-prettier');

module.exports = defineConfig([
  globalIgnores(['dist/', '.angular/', 'coverage/']),
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.strictTypeChecked,
      tseslint.configs.stylisticTypeChecked,
      angular.configs.tsRecommended,
      prettier,
    ],
    languageOptions: {
      parserOptions: {
        projectService: true,
        tsconfigRootDir: __dirname,
      },
    },
    processor: angular.processInlineTemplates,
    rules: {
      '@angular-eslint/directive-selector': [
        'error',
        {
          type: 'attribute',
          prefix: 'bf',
          style: 'camelCase',
        },
      ],
      '@angular-eslint/component-selector': [
        'error',
        {
          type: 'element',
          prefix: 'bf',
          style: 'kebab-case',
        },
      ],
      // Components, directives and pipes are classes that may only carry their decorator.
      '@typescript-eslint/no-extraneous-class': ['error', { allowWithDecorator: true }],
      // FE-22: no `any`, no `!`, no `as` (except `as const`) and exhaustive switches over unions.
      '@typescript-eslint/no-explicit-any': 'error',
      '@typescript-eslint/no-non-null-assertion': 'error',
      '@typescript-eslint/consistent-type-assertions': ['error', { assertionStyle: 'never' }],
      '@typescript-eslint/switch-exhaustiveness-check': [
        'error',
        { considerDefaultExhaustiveForUnions: false },
      ],
    },
  },
  {
    // FE-02: layer boundaries, like Modulith in the backend.
    files: ['src/**/*.ts'],
    plugins: { boundaries },
    settings: {
      'import/resolver': {
        typescript: { project: 'tsconfig.json' },
      },
      'boundaries/elements': [
        { type: 'core-api', pattern: 'src/app/core/api', partialMatch: false },
        { type: 'core', pattern: 'src/app/core', partialMatch: false },
        { type: 'ui', pattern: 'src/app/ui', partialMatch: false },
        { type: 'shared', pattern: 'src/app/shared', partialMatch: false },
        {
          type: 'feature',
          pattern: 'src/app/features/*',
          capture: ['feature'],
          partialMatch: false,
        },
        // Root files of the app: bootstrap, config and routes.
        { type: 'app', pattern: 'src', partialMatch: false },
      ],
    },
    rules: {
      'boundaries/no-unknown-dependencies': 'error',
      'boundaries/dependencies': [
        'error',
        {
          default: 'disallow',
          policies: [
            { allow: { to: { module: { origin: ['external', 'core'] } } } },
            { from: { element: { type: 'app' } }, allow: { to: { element: { type: '*' } } } },
            {
              from: { element: { type: 'core' } },
              allow: {
                to: { element: { types: { anyOf: ['core', 'core-api', 'ui', 'shared'] } } },
              },
            },
            {
              from: { element: { type: 'core-api' } },
              allow: { to: { element: { type: 'core-api' } } },
            },
            { from: { element: { type: 'ui' } }, allow: { to: { element: { type: 'ui' } } } },
            {
              from: { element: { type: 'shared' } },
              allow: { to: { element: { types: { anyOf: ['shared', 'ui', 'core-api'] } } } },
            },
            {
              from: { element: { type: 'feature' } },
              allow: {
                to: { element: { types: { anyOf: ['core', 'core-api', 'ui', 'shared'] } } },
              },
            },
            {
              // A feature never imports another one: it navigates to it by route.
              from: { element: { type: 'feature' } },
              allow: {
                to: {
                  element: {
                    type: 'feature',
                    captured: { feature: '{{ from.element.captured.feature }}' },
                  },
                },
              },
            },
          ],
        },
      ],
    },
  },
  {
    files: ['**/*.html'],
    extends: [angular.configs.templateRecommended, angular.configs.templateAccessibility],
    rules: {
      // RNF-15: every text is translatable and carries an explicit id (@@...).
      '@angular-eslint/template/i18n': ['error', { checkId: true }],
    },
  },
  {
    // The page title is set at runtime, translated, with the Title service.
    files: ['src/index.html'],
    rules: { '@angular-eslint/template/i18n': 'off' },
  },
  {
    // /_ui is a development tool, left out of the production build and of the translations.
    files: ['src/app/ui/catalog/**/*.html'],
    rules: { '@angular-eslint/template/i18n': 'off' },
  },
]);
