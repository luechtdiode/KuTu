import { Component, inject } from '@angular/core';
import { Platform, NavController, ModalController } from '@ionic/angular';
import { TermsModalComponent } from './create-competition/terms-modal.component';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html',
  standalone: false
})
export class AppComponent {
  platform = inject(Platform);
  navController = inject(NavController);
  modalCtrl = inject(ModalController);

  appPages = [
    { title: 'Wettkämpfe', url: '/competitions', icon: 'trophy' },
    { title: 'Neuer Wettkampf', url: '/competitions/create', icon: 'add-circle' },
    { title: 'Sicherheit', url: '/security', icon: 'shield-checkmark' }
  ];

  async showTerms() {
    const modal = await this.modalCtrl.create({
      component: TermsModalComponent
    });
    await modal.present();
  }

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
