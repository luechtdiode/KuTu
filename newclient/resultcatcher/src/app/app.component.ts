import { Component } from '@angular/core';

import { Platform, AlertController, NavController } from '@ionic/angular';
import { SplashScreen } from '@ionic-native/splash-screen/ngx';
import { StatusBar } from '@ionic-native/status-bar/ngx';
import { BackendService } from './services/backend.service';
import { HomePage } from './home/home.page';
import { StationPage } from './station/station.page';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html'
})
export class AppComponent {
  public appPages: Array<{title: string, url: string, icon: string}>;

  constructor(public platform: Platform, public statusBar: StatusBar, public splashScreen: SplashScreen,
              private navController: NavController,
              public backendService: BackendService, private alertCtrl: AlertController) {

    this.appPages = [
      { title: 'Home', url: '/home', icon: 'home' },
      { title: 'Resultate', url: '/station', icon: 'list' },
      { title: 'Letzte Resultate', url: 'last-results', icon: 'radio' },
      { title: 'Top Resultate', url: 'top-results', icon: 'medal' }
    ];

    this.initializeApp();
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
              { title: 'Letzte Resultate', url: '/last-results', icon: 'radio' }
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
              handler: data => {
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

  openPage(url: string) {
    this.navController.navigateRoot(url);
  }
}
