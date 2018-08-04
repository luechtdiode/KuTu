import { Component, ViewChild } from '@angular/core';
import { Nav, Platform, AlertController } from 'ionic-angular';
import { StatusBar } from '@ionic-native/status-bar';
import { SplashScreen } from '@ionic-native/splash-screen';

import { HomePage } from '../pages/home/home';
import { StationPage } from '../pages/station/station';
import { BackendService } from './backend.service';
import { LastTopResultsPage } from '../pages/last-top-results/last-top-results';
import { LastResultsPage } from '../pages/last-results/last-results';

@Component({
  templateUrl: 'app.html'
})
export class MyApp {
  @ViewChild(Nav) nav: Nav;

  rootPage: any = HomePage;

  pages: Array<{title: string, component: any}>;

  constructor(public platform: Platform, public statusBar: StatusBar, public splashScreen: SplashScreen,
     public backendService: BackendService, private alertCtrl: AlertController) {
    this.initializeApp();

    // used for an example of ngFor and navigation
    this.pages = [
      { title: 'Home', component: HomePage },
      { title: 'Resultate', component: StationPage },
      { title: 'Alle Resultate', component: LastResultsPage },
      { title: 'Top Resultate', component: LastTopResultsPage }/*,
      { title: 'Settings', component: SettingsPage }*/
    ];

  }

  initializeApp() {
    this.platform.ready().then(() => {
      // Okay, so the platform is ready and our plugins are available.
      // Here you can do any higher level native things you might need.
      this.statusBar.styleDefault();
      if(window.location.href.indexOf('?') > 0) {
        try {
          const initializeWith = atob(window.location.href.split('?')[1]);
          
          localStorage.setItem("external_load", initializeWith); 
          window.history.replaceState({}, document.title, window.location.href.split('?')[0]);
          
        } catch(e) {
          console.log(e);
        }
      }
      this.splashScreen.hide();

      this.backendService.showMessage.subscribe(message => {
        let alert = this.alertCtrl.create({
          title: 'Achtung',
          subTitle: message.msg || 'Die gewünschte Aktion ist aktuell nicht möglich.',
          buttons: ['OK']
        });
        alert.present();
      });
    });
  }

  openPage(page) {
    // Reset the content nav to have just this page
    // we wouldn't want the back button to show in this scenario
    this.nav.setRoot(page.component);
  }
}
