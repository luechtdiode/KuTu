import { Injectable, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { DomController } from '@ionic/angular';

interface Theme {
  name: string;
  styles: ThemeStyle[];
}

interface ThemeStyle {
  themeVariable: string;
  value: string;
}

@Injectable({
  providedIn: 'root'
})
export class ThemeSwitcherService {

  private themes: Theme[] = [];
  private currentTheme = 0;

  constructor(private domCtrl: DomController, @Inject(DOCUMENT) private document) {

    this.themes = [
      {
        name: 'day',
        styles: [
            { themeVariable: '--ion-border-color', value: 'var(--ion-color-light-shade)'},
            { themeVariable: '--ion-background-color', value: 'var(--ion-color-light)'},
            { themeVariable: '--ion-background-color-rgb: 6', value: 'var(--ion-color-light-rgb)'},
            { themeVariable: '--ion-text-color', value: 'var(--ion-color-dark)'},
            { themeVariable: '--ion-text-color-rgb: 25', value: 'var(--ion-color-dark-rgb)'},
            { themeVariable: '--ion-overlay-background-color', value: 'var(--ion-color-step-100)'},
            { themeVariable: '--ion-color-step-50', value: '#4a8686'},
            { themeVariable: '--ion-color-step-100', value: '#538d8d'},
            { themeVariable: '--ion-color-step-150', value: '#5d9393'},
            { themeVariable: '--ion-color-step-200', value: '#669999'},
            { themeVariable: '--ion-color-step-250', value: '#70a0a0'},
            { themeVariable: '--ion-color-step-300', value: '#79a6a6'},
            { themeVariable: '--ion-color-step-350', value: '#83acac'},
            { themeVariable: '--ion-color-step-400', value: '#8cb3b3'},
            { themeVariable: '--ion-color-step-450', value: '#96b9b9'},
            { themeVariable: '--ion-color-step-500', value: '#a0c0c0'},
            { themeVariable: '--ion-color-step-550', value: '#a9c6c6'},
            { themeVariable: '--ion-color-step-600', value: '#b3cccc'},
            { themeVariable: '--ion-color-step-650', value: '#bcd3d3'},
            { themeVariable: '--ion-color-step-700', value: '#c6d9d9'},
            { themeVariable: '--ion-color-step-750', value: '#cfdfdf'},
            { themeVariable: '--ion-color-step-800', value: '#d9e6e6'},
            { themeVariable: '--ion-color-step-850', value: '#e2ecec'},
            { themeVariable: '--ion-color-step-900', value: '#ecf2f2'},
            { themeVariable: '--ion-color-step-950', value: '#f5f9f9'},
        ]
      },
      {
        name: 'night',
        styles: [
          { themeVariable: '--ion-border-color', value: 'var(--ion-color-medium-shade)'},
          { themeVariable: '--ion-background-color', value: 'var(--ion-color-dark)'},
          { themeVariable: '--ion-background-color-rgb', value: 'var(--ion-color-dark-rgb)'},
          { themeVariable: '--ion-text-color', value: 'var(--ion-color-light)'},
          { themeVariable: '--ion-text-color-rgb', value: 'var(--ion-color-light-rgb)'},
          { themeVariable: '--ion-overlay-background-color', value: 'var(--ion-color-step-100)'},
          { themeVariable: '--ion-color-step-50', value: '#232323'},
          { themeVariable: '--ion-color-step-100', value: '#2e2e2e'},
          { themeVariable: '--ion-color-step-150', value: '#3a3a3a'},
          { themeVariable: '--ion-color-step-200', value: '#454545'},
          { themeVariable: '--ion-color-step-250', value: '#515151'},
          { themeVariable: '--ion-color-step-300', value: '#5d5d5d'},
          { themeVariable: '--ion-color-step-350', value: '#8b8b8b'},
          { themeVariable: '--ion-color-step-400', value: '#747474'},
          { themeVariable: '--ion-color-step-450', value: '#7f7f7f'},
          { themeVariable: '--ion-color-step-500', value: '#8b8b8b'},
          { themeVariable: '--ion-color-step-550', value: '#979797'},
          { themeVariable: '--ion-color-step-600', value: '#a2a2a2'},
          { themeVariable: '--ion-color-step-650', value: '#aeaeae'},
          { themeVariable: '--ion-color-step-700', value: '#b9b9b9'},
          { themeVariable: '--ion-color-step-750', value: '#c5c5c5'},
          { themeVariable: '--ion-color-step-800', value: '#d1d1d1'},
          { themeVariable: '--ion-color-step-850', value: '#dcdcdc'},
          { themeVariable: '--ion-color-step-900', value: '#e8e8e8'},
          { themeVariable: '--ion-color-step-950', value: '#f3f3f3'},
        ]
      }
    ];
  }

  cycleTheme(): void {

    if (this.themes.length > this.currentTheme + 1) {
      this.currentTheme++;
    } else {
      this.currentTheme = 0;
    }

    this.setTheme(this.themes[this.currentTheme].name);

  }

  setTheme(name): void {

    const theme = this.themes.find(t => t.name === name);

    this.domCtrl.write(() => {

      theme.styles.forEach(style => {
        document.documentElement.style.setProperty(style.themeVariable, style.value);
      });

    });

  }

}