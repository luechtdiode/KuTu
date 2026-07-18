import { Component, Input, inject } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Geraet } from '../backend-types';

@Component({
  templateUrl: 'riege-edit-modal.component.html',
  standalone: false
})
export class RiegeEditModalComponent {
  @Input() name = '';
  @Input() durchgang = '';
  @Input() startId = 0;
  @Input() kind = 0;
  @Input() disziplinen: Geraet[] = [];

  editName = '';
  editDurchgang = '';
  selectedStartId = 0;

  private modalCtrl = inject(ModalController);

  get startGeraet(): string {
    return this.disziplinen.find(d => d.id === this.selectedStartId)?.name;
  }

  ngOnInit() {
    this.editName = this.name;
    this.editDurchgang = this.durchgang;
    this.selectedStartId = this.startId;
  }

  dismiss() {
    this.modalCtrl.dismiss(null);
  }

  confirm() {
    const name = this.editName.trim();
    const durchgang = this.editDurchgang.trim();
    if (!name || !durchgang) return;
    this.modalCtrl.dismiss({ name: name, durchgang: durchgang, delete: false, startId: this.selectedStartId, kind: this.kind });
  }

  delete() {
    this.modalCtrl.dismiss({ name: this.editName, durchgang: this.editDurchgang, delete: true, kind: this.kind });
  }
}
