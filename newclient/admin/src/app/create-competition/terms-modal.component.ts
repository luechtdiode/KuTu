import { Component, inject } from '@angular/core';
import { ModalController } from '@ionic/angular';

@Component({
  templateUrl: 'terms-modal.component.html',
  standalone: false
})
export class TermsModalComponent {
  private modalCtrl = inject(ModalController);

  dismiss(accepted: boolean) {
    this.modalCtrl.dismiss(accepted);
  }
}
