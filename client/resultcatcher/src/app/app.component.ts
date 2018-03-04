import { Component, ViewChild } from '@angular/core';
import { Nav, Platform } from 'ionic-angular';
import { StatusBar } from '@ionic-native/status-bar';
import { SplashScreen } from '@ionic-native/splash-screen';

import { HomePage } from '../pages/home/home';
import { RiegeListPage } from '../pages/list/riege-list';
import { SettingsPage } from '../pages/settings/settings';
import { StationPage } from '../pages/station/station';

@Component({
  templateUrl: 'app.html'
})
export class MyApp {
  @ViewChild(Nav) nav: Nav;

  rootPage: any = HomePage;

  pages: Array<{title: string, component: any}>;

  constructor(public platform: Platform, public statusBar: StatusBar, public splashScreen: SplashScreen) {
    this.initializeApp();

    // used for an example of ngFor and navigation
    this.pages = [
      { title: 'Home', component: HomePage },
      { title: 'Resultate', component: StationPage }/*,
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
    });
  }

  openPage(page) {
    // Reset the content nav to have just this page
    // we wouldn't want the back button to show in this scenario
    this.nav.setRoot(page.component);
  }
}
