import { Component, Input, inject } from '@angular/core';
import { ModalController } from '@ionic/angular';

@Component({
  templateUrl: 'judge-link-modal.component.html',
  standalone: false
})
export class JudgeLinkModalComponent {
  @Input() link = '';
  @Input() qrUrl = '';

  private modalCtrl = inject(ModalController);

  dismiss() {
    this.modalCtrl.dismiss(null);
  }

  copyLink() {
    navigator.clipboard.writeText(this.link);
  }
}
