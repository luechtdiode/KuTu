import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { firstValueFrom } from 'rxjs';
import { AdminBackendService } from '../services/admin-backend.service';
import { TeamItem, TeilnehmerItem, TeilnehmerProgramm, TeilnehmerUpdate } from '../backend-types';

@Component({
  selector: 'app-teilnehmer-edit-modal',
  standalone: false,
  templateUrl: 'teilnehmer-edit-modal.component.html'
})
export class TeilnehmerEditModalComponent implements OnInit {
  @Input() uuid = '';
  @Input() secret = '';
  @Input() item!: TeilnehmerItem;

  programme = signal<TeilnehmerProgramm[]>([]);
  teams = signal<TeamItem[]>([]);
  programmId = signal(0);
  riege = signal('');
  riege2 = signal('');
  team = signal(0);
  reserve = signal(0);
  saving = signal(false);
  error = signal('');

  private modalCtrl = inject(ModalController);
  private backend = inject(AdminBackendService);

  async ngOnInit() {
    this.programmId.set(this.item.programmId);
    this.riege.set(this.item.riege || '');
    this.riege2.set(this.item.riege2 || '');
    this.team.set(this.item.team);
    this.reserve.set(this.item.reserve);
    try {
      this.programme.set(await firstValueFrom(this.backend.getTeilnehmerProgramme(this.uuid, this.secret)));
    } catch (e) {
      this.error.set('Kategorien konnten nicht geladen werden');
    }
    try {
      this.teams.set(await firstValueFrom(this.backend.getTeilnehmerTeams(this.uuid, this.item.athletId, this.secret)));
    } catch (e) {
      this.error.set('Teams konnten nicht geladen werden');
    }
  }

  dismiss() {
    this.modalCtrl.dismiss(null);
  }

  async save() {
    this.saving.set(true);
    this.error.set('');
    const i = this.item;
    const update: TeilnehmerUpdate = {};
    if (this.programmId() !== i.programmId) update.programmId = this.programmId();
    if (this.team() !== i.team) update.team = this.team();
    if (Number(this.reserve()) !== i.reserve) update.reserve = Number(this.reserve());
    if (this.riege().trim() !== (i.riege || '')) update.riege = this.riege().trim();
    if (this.riege2().trim() !== (i.riege2 || '')) update.riege2 = this.riege2().trim();
    try {
      const saved = await firstValueFrom(this.backend.updateTeilnehmer(this.uuid, i.athletId, update, this.secret));
      this.modalCtrl.dismiss(saved);
    } catch (e) {
      this.error.set('Fehler beim Speichern: ' + ((e as any).message || e));
      this.saving.set(false);
    }
  }
}
