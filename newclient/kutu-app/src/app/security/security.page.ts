import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { AlertController, ToastController } from '@ionic/angular';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { StoredSecret } from '../backend-types';
import { firstValueFrom } from 'rxjs';

@Component({
  templateUrl: 'security.page.html',
  standalone: false
})
export class SecurityPage {
  secrets: StoredSecret[] = [];
  private cdr = inject(ChangeDetectorRef);
  private secretService = inject(SecretService);
  private backend = inject(AdminBackendService);
  private alertCtrl = inject(AlertController);
  private toastCtrl = inject(ToastController);

  ionViewWillEnter() {
    this.secrets = this.secretService.getSecrets();
    this.cdr.detectChanges();
  }

  async confirmDelete(s: StoredSecret) {
    const alert = await this.alertCtrl.create({
      header: 'Wettkampf löschen',
      message: `Soll der Wettkampf "${s.titel}" (${s.datum}) wirklich gelöscht werden?<br><br><strong>Alle Daten (Athleten, Wertungen, Anmeldungen) werden unwiderruflich gelöscht.</strong>`,
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
