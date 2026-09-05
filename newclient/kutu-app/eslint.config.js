const angularPlugin = require("@angular-eslint/eslint-plugin");
const angularTemplatePlugin = require("@angular-eslint/eslint-plugin-template");
const angularTemplateParser = require("@angular-eslint/template-parser");
const tsParser = require("@typescript-eslint/parser");

function collectRecommendedRules(plugin, prefix) {
  return Object.fromEntries(
    Object.entries(plugin.rules)
      .filter(([, rule]) => rule && rule.meta && rule.meta.docs && rule.meta.docs.recommended === "recommended")
      .map(([name]) => [`${prefix}/${name}`, "error"]),
  );
}
/*
function pickRules(plugin, prefix, names) {
  return Object.fromEntries(names.map((name) => [`${prefix}/${name}`, "error"]));
}

const angularTargetRules = pickRules(angularPlugin, "@angular-eslint", [
  "prefer-on-push-component-change-detection",
  "prefer-inject",
  "no-empty-lifecycle-method",
]);

const angularTemplateTargetRules = pickRules(angularTemplatePlugin, "@angular-eslint/template", [
  "prefer-control-flow",
  "eqeqeq",
]);
*/
module.exports = [
  {
    ignores: ["projects/**/*", "dist/**/*", "node_modules/**/*", "www/**/*", ".angular/**/*"],
  },
  {
    files: ["**/*.ts"],
    languageOptions: {
      parser: tsParser,
      parserOptions: {
        project: ["tsconfig.json", "e2e/tsconfig.json"],
        createDefaultProgram: true,
        ecmaVersion: "latest",
        sourceType: "module",
      },
    },
    plugins: {
      "@angular-eslint": angularPlugin,
      "@angular-eslint/template": angularTemplatePlugin,
    },
    processor: angularTemplatePlugin.processors["extract-inline-html"],
    rules: {
      //...angularTargetRules,
      "@angular-eslint/component-class-suffix": [
        "error",
        {
          suffixes: ["Page", "Component"],
        },
      ],
      "@angular-eslint/component-selector": [
        "error",
        {
          type: "element",
          prefix: ["app", "result", "riege", "scorelist", "startlist", "clublist"],
          style: "kebab-case",
        },
      ],
      "@angular-eslint/directive-selector": [
        "error",
        {
          type: "attribute",
          prefix: "app",
          style: "camelCase",
        },
      ],
    },
  },
  {
    files: ["**/*.html"],
    languageOptions: {
      parser: angularTemplateParser,
    },
    plugins: {
      "@angular-eslint/template": angularTemplatePlugin,
    },
    rules: {
      //...angularTemplateTargetRules,
    },
  },
];