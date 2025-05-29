import { Injectable, Inject, DOCUMENT } from '@angular/core';

import * as Color from 'color';

@Injectable({
  providedIn: 'root'
})
export class ThemeSwitcherService {

  constructor(
    @Inject(DOCUMENT) private document: Document,
  ) {
    const cssText = localStorage.getItem('theme');
    if (cssText) {
      this.setGlobalCSS(cssText);
    } else {
      this.setTheme({});
    }
  }

  // Override all global variables with a new theme
  setTheme(theme) {
    const cssText = CSSTextGenerator(theme);
    this.setGlobalCSS(cssText);
    localStorage.setItem('theme', cssText);
  }

  // Define a single CSS variable
  setVariable(name, value) {
    this.document.documentElement.style.setProperty(name, value);
  }

  private setGlobalCSS(css: string) {
    this.document.documentElement.style.cssText = css;
  }

  get storedTheme() {
    return localStorage.getItem('theme');
  }
}

const defaults = {
  primary: '#3880ff',
  secondary: '#0cd1e8',
  tertiary: '#7044ff',
  success: '#10dc60',
  warning: '#ff7b00',
  danger: '#f04141',
  dark: '#222428',
  medium: '#989aa2',
  light: '#fcfdff'
};

function generateSteppedColors(background, text) {
  if (background === void 0) { background = '#ffffff'; }
  if (text === void 0) { text = '#000000'; }
  const color = new Color(background);
  let steps = '';
  for (let i = 5; i < 100; i = i + 5) {
      const step = i + '0';
      const amount = i / 100.0;
      const stepColor: string = color.mix(Color(text), amount).hex();
      steps += `  --ion-color-step-${step}: ${stepColor};`;
      if (i < 95) {
          steps += '\n';
      }
  }
  return steps;
}

function CSSTextGenerator(colors) {
  colors = { ...defaults, ...colors };

  const {
    primary,
    secondary,
    tertiary,
    success,
    warning,
    danger,
    dark,
    medium,
    light
  } = colors;

  const shadeRatio = 0.2;
  const tintRatio = 0.2;

  return `
    --ion-color-base: ${light};
    --ion-color-contrast: ${dark};
    --ion-background-color: ${light};
    --ion-card-background-color: ${accentuate(light, 0.4)};
    --ion-card-shadow-color1: ${accentuate(dark, 2.0).alpha(0.2)};
    --ion-card-shadow-color2: ${accentuate(dark, 2.0).alpha(0.14)};
    --ion-card-shadow-color3: ${accentuate(dark, 2.0).alpha(0.12)};
    --ion-card-color: ${accentuate(dark, 2.0)};
    --ion-text-color: ${dark};
    --ion-toolbar-background-color: ${Color(light).lighten(tintRatio)};
    --ion-toolbar-text-color: ${contrast(dark, 0.2)};
    --ion-item-background-color: ${contrast(light, 0.1)};
    --ion-item-background-activated: ${contrast(light, 0.3)};
    --ion-item-text-color: ${contrast(dark, 0.1)};
    --ion-item-border-color: ${Color(medium).lighten(tintRatio)};
    --ion-overlay-background-color: ${accentuate(light, 0.1)};
    --ion-color-primary: ${primary};
    --ion-color-primary-rgb: ${rgb(primary)};
    --ion-color-primary-contrast: ${contrast(primary)};
    --ion-color-primary-contrast-rgb: ${rgb(contrast(primary))};
    --ion-color-primary-shade:  ${Color(primary).darken(shadeRatio)};
    --ion-color-primary-tint:  ${Color(primary).lighten(tintRatio)};
    --ion-color-secondary: ${secondary};
    --ion-color-secondary-rgb: ${rgb(secondary)};
    --ion-color-secondary-contrast: ${contrast(secondary)};
    --ion-color-secondary-contrast-rgb: ${rgb(contrast(secondary))};
    --ion-color-secondary-shade:  ${Color(secondary).darken(shadeRatio)};
    --ion-color-secondary-tint: ${Color(secondary).lighten(tintRatio)};
    --ion-color-tertiary:  ${tertiary};
    --ion-color-tertiary-rgb: ${rgb(tertiary)};
    --ion-color-tertiary-contrast: ${contrast(tertiary)};
    --ion-color-tertiary-contrast-rgb: ${rgb(contrast(tertiary))};
    --ion-color-tertiary-shade: ${Color(tertiary).darken(shadeRatio)};
    --ion-color-tertiary-tint:  ${Color(tertiary).lighten(tintRatio)};
    --ion-color-success: ${success};
    --ion-color-success-rgb: ${rgb(success)};
    --ion-color-success-contrast: ${contrast(success)};
    --ion-color-success-contrast-rgb: ${rgb(contrast(success))};
    --ion-color-success-shade: ${Color(success).darken(shadeRatio)};
    --ion-color-success-tint: ${Color(success).lighten(tintRatio)};
    --ion-color-warning: ${warning};
    --ion-color-warning-rgb: ${rgb(warning)};
    --ion-color-warning-contrast: ${contrast(warning)};
    --ion-color-warning-contrast-rgb: ${rgb(contrast(warning))};
    --ion-color-warning-shade: ${Color(warning).darken(shadeRatio)};
    --ion-color-warning-tint: ${Color(warning).lighten(tintRatio)};
    --ion-color-danger: ${danger};
    --ion-color-danger-rgb: ${rgb(danger)};
    --ion-color-danger-contrast: ${contrast(danger)};
    --ion-color-danger-contrast-rgb: ${rgb(contrast(danger))};
    --ion-color-danger-shade: ${Color(danger).darken(shadeRatio)};
    --ion-color-danger-tint: ${Color(danger).lighten(tintRatio)};
    --ion-color-dark: ${dark};
    --ion-color-dark-rgb: ${rgb(dark)};
    --ion-color-dark-contrast: ${contrast(dark)};
    --ion-color-dark-contrast-rgb: ${rgb(contrast(dark))};
    --ion-color-dark-shade: ${Color(dark).darken(shadeRatio)};
    --ion-color-dark-tint: ${Color(dark).lighten(tintRatio)};
    --ion-color-medium: ${medium};
    --ion-color-medium-rgb: ${rgb(medium)};
    --ion-color-medium-contrast: ${contrast(medium)};
    --ion-color-medium-contrast-rgb: ${rgb(contrast(medium))};
    --ion-color-medium-shade: ${Color(medium).darken(shadeRatio)};
    --ion-color-medium-tint: ${Color(medium).lighten(tintRatio)};
    --ion-color-light: ${light};
    --ion-color-light-rgb: ${rgb(light)};
    --ion-color-light-contrast: ${contrast(light)};
    --ion-color-light-contrast-rgb: ${rgb(contrast(light))};
    --ion-color-light-shade: ${Color(light).darken(shadeRatio)};
    --ion-color-light-tint: ${Color(light).lighten(tintRatio)};`
    + generateSteppedColors(dark, light);
}

function contrast(color, ratio = 0.8) {
  const c = Color(color);
  return c.isDark() ? c.lighten(ratio) : c.darken(ratio);
}
function accentuate(color, ratio = 0.8) {
  const c = Color(color);
  return c.isDark() ? c.darken(ratio) : c.lighten(ratio);
}
function rgb(color) {
  const c = Color(color);
  return `${c.red()}, ${c.green()}, ${c.blue()}`;
}
