import { Component, inject } from '@angular/core';
import { Platform, NavController } from '@ionic/angular';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html',
  standalone: false
})
export class AppComponent {
  platform = inject(Platform);
  navController = inject(NavController);

  appPages = [
    { title: 'Wettkämpfe', url: '/competitions', icon: 'trophy' },
    { title: 'Neuer Wettkampf', url: '/competitions/create', icon: 'add-circle' },
    { title: 'Sicherheit', url: '/security', icon: 'shield-checkmark' }
  ];

  constructor() {
    this.initializeApp();
  }

  initializeApp() {
    this.platform.ready().then(() => {
      if (window.location.pathname === '/' || window.location.pathname === '') {
        this.navController.navigateRoot('/competitions');
      }
    });
  }
}
