import { Component, Input, inject } from '@angular/core';
import { ModalController } from '@ionic/angular';

@Component({
  templateUrl: 'start-offset-modal.component.html',
  standalone: false
})
export class StartOffsetModalComponent {
  @Input() title = '';
  @Input() set defaultDate(val: string) { this.editDate = val; }
  @Input() set defaultTime(val: string) { this.editTime = val; }

  editDate = '';
  editTime = '';

  private modalCtrl = inject(ModalController);

  dismiss() {
    this.modalCtrl.dismiss(null);
  }

  confirm() {
    if (!this.editDate) return;
    this.modalCtrl.dismiss({ date: this.editDate, time: this.editTime || '00:00' });
  }
}
