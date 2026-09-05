import { Component, Input, inject } from '@angular/core';
import { ModalController } from '@ionic/angular';

@Component({
  templateUrl: 'standard-link-modal.component.html',
  standalone: false
})
export class StandardLinkModalComponent {
  @Input() link = '';
  @Input() qrUrl = '';
  @Input() title = '';
  @Input() description = '';

  private modalCtrl = inject(ModalController);

  dismiss() {
    this.modalCtrl.dismiss(null);
  }

  copyLink() {
    navigator.clipboard.writeText(this.link);
  }
}
