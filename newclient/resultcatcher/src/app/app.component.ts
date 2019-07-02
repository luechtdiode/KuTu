import { Component } from '@angular/core';

import { Platform, AlertController, NavController } from '@ionic/angular';
import { SplashScreen } from '@ionic-native/splash-screen/ngx';
import { StatusBar } from '@ionic-native/status-bar/ngx';
import { BackendService } from './services/backend.service';
import { ThemeSwitcherService } from './services/theme-switcher.service';
import { ThemeSwitcher2Service } from './services/theme-switcher2.service';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html'
})
export class AppComponent {

  constructor(public platform: Platform, public statusBar: StatusBar, public splashScreen: SplashScreen,
              private navController: NavController, private route: ActivatedRoute, private router: Router,
              public themeSwitcher: ThemeSwitcherService, public themeSwitcher2: ThemeSwitcher2Service,
              public backendService: BackendService, private alertCtrl: AlertController) {

    this.appPages = [
      { title: 'Home', url: '/home', icon: 'home' },
      { title: 'Resultate', url: '/station', icon: 'list' },
      { title: 'Letzte Resultate', url: 'last-results', icon: 'radio' },
      { title: 'Top Resultate', url: 'top-results', icon: 'medal' },
      { title: 'Athlet/-In suchen', url: 'search-athlet', icon: 'search' }
    ];

    this.initializeApp();
  }
  public appPages: Array<{title: string, url: string, icon: string}>;

  themes = {
    /*autumn: {
      primary: '#F78154',
      secondary: '#4D9078',
      tertiary: '#B4436C',
      light: '#FDE8DF',
      medium: '#FCD0A2',
      dark: '#B89876'
    },*/
    Blau: {
      primary: '#ffa238',
      secondary: '#a19137',
      tertiary: '#421804',
      success: '#0eb651',
      warning: '#ff7b00',
      danger: '#f04141',
      dark: '#fffdf5',
      medium: '#606ea7',
      light: '#03163d'
    },
    Sport: {
      primary: '#ffa238',
      secondary: '#7dc0ff',
      tertiary: '#421804',
      success: '#0eb651',
      warning: '#ff7b00',
      danger: '#f04141',
      dark: '#03163d',
      medium: '#606ea7',
      light: '#fffdf5'
    },
    Dunkel: {
      primary: '#8DBB82',
      secondary: '#FCFF6C',
      tertiary: '#FE5F55',
      warning: '#ffce00',
      medium: '#BCC2C7',
      dark: '#DADFE1', // #F7F7FF
      light: '#363232' // #1c1b1a
    },
    Neon: {
      primary: '#39BFBD',
      secondary: '#4CE0B3',
      tertiary: '#FF5E79',
      warning: '#ff7b00',
      light: '#F4EDF2',
      medium: '#B682A5',
      dark: '#34162A'
    }
  };

  get themeKeys() {
    return Object.keys(this.themes);
  }

  clearPosParam() {
    this.router.navigate(
      ['.'],
      { relativeTo: this.route, queryParams: {} }
    );
  }

  initializeApp() {
    this.platform.ready().then(() => {
      // Okay, so the platform is ready and our plugins are available.
      // Here you can do any higher level native things you might need.
      this.statusBar.styleDefault();

      if (window.location.href.indexOf('?') > 0) {
        try {
          const initializeWithEncoded = window.location.href.split('?')[1];
          const initializeWith = atob(initializeWithEncoded);
          if (initializeWithEncoded.startsWith('all')) {
            this.appPages = [
              { title: 'Alle Resultate', url: '/all-results', icon: 'radio' }
            ];
            this.navController.navigateRoot('/last-results');
          } else if (initializeWithEncoded.startsWith('top')) {
            this.appPages = [
              { title: 'Top Resultate', url: '/top-results', icon: 'medal' }
            ];
            this.navController.navigateRoot('/top-results');
          } else if (initializeWith.startsWith('last')) {
            this.appPages = [
              { title: 'Aktuelle Resultate', url: '/last-results', icon: 'radio' }
            ];
            this.navController.navigateRoot('/last-results');
            this.backendService.initWithQuery(initializeWith.substring(5));
            // localStorage.setItem("external_load", initializeWith.substring(5));
          } else if (initializeWith.startsWith('top')) {
            this.appPages = [
              { title: 'Top Resultate', url: '/top-results', icon: 'medal' }
            ];
            this.navController.navigateRoot('/top-results');

            this.backendService.initWithQuery(initializeWith.substring(4));
            // localStorage.setItem("external_load", initializeWith.substring(4));
          } else {
            window.history.replaceState({}, document.title, window.location.href.split('?')[0]);
            this.clearPosParam();
            console.log('initializing with ' + initializeWith);
            localStorage.setItem('external_load', initializeWith);
            if (initializeWith.startsWith('c=') && initializeWith.indexOf('&st=') > -1 && initializeWith.indexOf('&g=') > -1) {
              this.appPages = [
                { title: 'Home', url: '/home', icon: 'home' },
                { title: 'Resultate', url: '/station', icon: 'list' },
              ];
              this.navController.navigateRoot('/station');
            }
          }

        } catch (e) {
          console.log(e);
        }
      } else if (localStorage.getItem('current_station')) {
        const cs = localStorage.getItem('current_station');
        if (cs.startsWith('c=') && cs.indexOf('&st=') && cs.indexOf('&g=')) {
          this.appPages = [
            { title: 'Home', url: '/home', icon: 'home' },
            { title: 'Resultate', url: '/station', icon: 'list' },
          ];
          this.navController.navigateRoot('/station');
        }
      }
      this.splashScreen.hide();

      this.backendService.askForUsername.subscribe(service => {
        const alert = this.alertCtrl.create({
          header: 'Settings',
          message: service.currentUserName ? 'Dein Benutzername' : 'Du bist das erste Mal hier. Bitte gib einen Benutzernamen an',
          inputs: [
            {
              name: 'username',
              placeholder: 'Benutzername',
              value: service.currentUserName
            }
          ],
          buttons: [
            {
              text: 'Abbrechen',
              role: 'cancel',
              handler: () => {
                console.log('Cancel clicked');
              }
            },
            {
              text: 'Speichern',
              handler: data => {
                if (data.username && data.username.trim().length > 1) {
                  service.currentUserName = data.username.trim();
                } else {
                  // invalid name
                  return false;
                }
              }
            }
          ]
        });
        alert.then(a => a.present());
      });
      this.backendService.showMessage.subscribe(message => {
        let msg = message.msg;
        if (!msg || msg.trim().length === 0) {
          msg = 'Die gewünschte Aktion ist aktuell nicht möglich.';
        }
        const alert = this.alertCtrl.create({
          header: 'Achtung',
          message: msg,
          buttons: ['OK']
        });
        alert.then(a => a.present());
      });
    });
  }

  askUserName() {
    this.backendService.askForUsername.next(this.backendService);
  }

  cycleTheme() {
    this.themeSwitcher.cycleTheme();
  }

  changeTheme(name: string) {
    this.themeSwitcher2.setTheme(this.themes[name]);
  }
  openPage(url: string) {
    this.navController.navigateRoot(url);
  }
}
