import { Component, inject } from '@angular/core';
import { ModalController } from '@ionic/angular';
import {Terms} from "../backend-types";

@Component({
  templateUrl: 'terms-modal.component.html',
  standalone: false
})
export class TermsModalComponent {
  private modalCtrl = inject(ModalController);

  terms!: Terms;

  dismiss(accepted: boolean) {
    this.modalCtrl.dismiss(accepted);
  }
}
