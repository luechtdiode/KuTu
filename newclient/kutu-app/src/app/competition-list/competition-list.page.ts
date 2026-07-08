import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { AlertController, ToastController, NavController } from '@ionic/angular';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { StoredSecret } from '../backend-types';
import { downloadBlob } from '../utils';
import { firstValueFrom } from 'rxjs';

@Component({
  templateUrl: 'competition-list.page.html',
  standalone: false
})
export class CompetitionListPage {
  secrets: StoredSecret[] = [];
  private cdr = inject(ChangeDetectorRef);
  private secretService = inject(SecretService);
  private backend = inject(AdminBackendService);
  private alertCtrl = inject(AlertController);
  private toastCtrl = inject(ToastController);
  private nav = inject(NavController);

  ionViewWillEnter() {
    this.secrets = this.secretService.getSecrets();
    this.cdr.detectChanges();
  }

  async downloadZip(s: StoredSecret) {
    try {
      const blob = await firstValueFrom(this.backend.downloadCompetitionZip(s.uuid, s.secret));
      downloadBlob(blob, s.titel + '.zip');
    } catch (e) {
      const toast = await this.toastCtrl.create({
        message: 'Download fehlgeschlagen: ' + (e as any).message,
        duration: 3000,
        color: 'danger'
      });
      await toast.present();
    }
  }

  async confirmDelete(s: StoredSecret) {
    const alert = await this.alertCtrl.create({
      header: 'Wettkampf löschen',
      message: `Soll "${s.titel}" (${s.datum}) wirklich gelöscht werden?<br><br><strong>Alle Daten werden unwiderruflich gelöscht.</strong>`,
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Löschen',
          role: 'destructive',
          handler: () => this.deleteCompetition(s)
        }
      ]
    });
    await alert.present();
  }

  openRiegen(s: StoredSecret) {
    this.nav.navigateRoot('/admin/riege-einteilung/' + s.uuid);
  }

  openRegistrations(s: StoredSecret) {
    this.nav.navigateRoot('/admin/registrations/' + s.uuid);
  }

  private async deleteCompetition(s: StoredSecret) {
    try {
      await firstValueFrom(this.backend.deleteCompetition(s.uuid, s.secret));
    } catch (e) {
      // Continue even if server delete fails
    }
    this.secretService.removeSecret(s.uuid);
    this.secrets = this.secretService.getSecrets();
    this.cdr.detectChanges();
    const toast = await this.toastCtrl.create({
      message: 'Wettkampf gelöscht.',
      duration: 2000,
      color: 'success'
    });
    await toast.present();
  }
}
