// @ts-check
const eslint = require("@eslint/js");
const tseslint = require("typescript-eslint");
const angular = require("angular-eslint");
const prettier = require("eslint-config-prettier");
const importPlugin = require("eslint-plugin-import");
const jestPlugin = require("eslint-plugin-jest");

module.exports = tseslint.config(
  {
    ignores: ["**/dist/**", "**/node_modules/**", ".angular/**", "coverage/**", "jest.config.ts"]
  },
  {
    files: ["**/*.ts"],
    extends: [
      eslint.configs.recommended,
      ...tseslint.configs.recommendedTypeChecked,
      ...angular.configs.tsRecommended,
      importPlugin.flatConfigs.recommended,
      prettier
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.eslint.json'],
        tsconfigRootDir: __dirname,
        ecmaVersion: 'latest',
        sourceType: 'module'
      }
    },
    settings: {
      "import/resolver": {
        typescript: {
          alwaysTryTypes: true,
          project: './tsconfig.json'
        }
      }
    },
    processor: angular.processInlineTemplates,
    rules: {
      // Angular selectors
      "@angular-eslint/directive-selector": [
        "error",
        {
          type: "attribute",
          prefix: "app",
          style: "camelCase",
        },
      ],
      "@angular-eslint/component-selector": [
        "error",
        {
          type: "element",
          prefix: "app",
          style: "kebab-case",
        },
      ],

      // TypeScript rules - relaxed for pragmatism
      "@typescript-eslint/no-unused-vars": ["warn", {"argsIgnorePattern": "^_", "varsIgnorePattern": "^_"}],
      "@typescript-eslint/no-explicit-any": "error",
      "@typescript-eslint/prefer-nullish-coalescing": "off", // Too strict for simple cases
      "@typescript-eslint/prefer-optional-chain": "warn",

      // Import rules
      "import/no-unresolved": "off", // TypeScript handles this better
      "import/namespace": "off", // Slow and redundant with TypeScript
      "import/default": "off", // Slow and redundant with TypeScript
      "import/no-named-as-default": "off", // False positives with Angular
      "import/no-named-as-default-member": "off", // False positives
      "import/order": ["warn", {
        "newlines-between": "always",
        "groups": [["builtin", "external"], "internal", ["parent", "sibling", "index"]],
        "alphabetize": {"order": "asc", "caseInsensitive": true}
      }]
    },
  },
  {
    files: ["**/*.html"],
    extends: [
      ...angular.configs.templateRecommended,
      ...angular.configs.templateAccessibility,
    ],
    rules: {
      "@angular-eslint/template/no-negated-async": "off"
    },
  },
  {
    files: ["**/*.spec.ts"],
    extends: [jestPlugin.configs["flat/recommended"]],
  },
  {
    files: ["**/*.routes.ts", "**/app.routes.ts"],
    rules: {
      // Angular Router's resolve/guards typing is weak - causes false positives
      "@typescript-eslint/no-unsafe-assignment": "off"
    }
  }
);
