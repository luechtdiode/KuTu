import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { AlertController, ToastController } from '@ionic/angular';
import { ActivatedRoute } from '@angular/router';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { ClubRegistration, SyncAction, Verein } from '../backend-types';
import { firstValueFrom } from 'rxjs';

@Component({
  templateUrl: 'admin-registrations.page.html',
  standalone: false
})
export class AdminRegistrationsPage {
  uuid = '';
  secret = '';
  wettkampfTitle = '';
  registrations: ClubRegistration[] = [];
  syncActions: SyncAction[] = [];
  loading = false;

  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private secretService = inject(SecretService);
  private backend = inject(AdminBackendService);
  private alertCtrl = inject(AlertController);
  private toastCtrl = inject(ToastController);

  async ionViewWillEnter() {
    this.uuid = this.route.snapshot.paramMap.get('uuid') || '';
    const stored = this.secretService.getSecret(this.uuid);
    if (stored) {
      this.secret = stored.secret;
      this.wettkampfTitle = stored.titel;
    }
    await this.loadData();
  }

  private async loadData() {
    this.loading = true;
    try {
      const [registrations, syncActions] = await Promise.all([
        firstValueFrom(this.backend.getRegistrations(this.uuid, this.secret)),
        firstValueFrom(this.backend.getSyncActions(this.uuid)).catch(() => [] as SyncAction[])
      ]);
      this.registrations = registrations;
      this.syncActions = syncActions;
    } catch {
      this.registrations = [];
      this.syncActions = [];
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  isApproved(reg: ClubRegistration): boolean {
    return reg.vereinId != null && reg.vereinId > 0;
  }

  async approveRegistration(reg: ClubRegistration) {
    const alert = await this.alertCtrl.create({
      header: 'Verein annehmen',
      message: `Möchtest du die Anmeldung von "${reg.vereinname}" annehmen?`,
      inputs: [
        { name: 'vereinname', type: 'text', value: reg.vereinname, placeholder: 'Vereinsname' },
        { name: 'verband', type: 'text', value: reg.verband || '', placeholder: 'Verband' }
      ],
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Annehmen',
          handler: async (data) => {
            try {
              const verein: Verein = { id: 0, name: data.vereinname, verband: data.verband || '' };
              await firstValueFrom(this.backend.approveRegistration(this.uuid, reg.id, verein, this.secret));
              await this.loadData();
              const toast = await this.toastCtrl.create({ message: `${data.vereinname} angenommen`, duration: 2000, color: 'success' });
              await toast.present();
            } catch (e: any) {
              const toast = await this.toastCtrl.create({ message: 'Fehler: ' + (e.message || e), duration: 3000, color: 'danger' });
              await toast.present();
            }
          }
        }
      ]
    });
    await alert.present();
  }

  async deleteRegistration(reg: ClubRegistration) {
    const alert = await this.alertCtrl.create({
      header: 'Anmeldung löschen',
      message: `Soll die Anmeldung von "${reg.vereinname}" gelöscht werden?`,
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Löschen', role: 'destructive',
          handler: async () => {
            try {
              await firstValueFrom(this.backend.deleteRegistration(this.uuid, reg.id, this.secret));
              await this.loadData();
              const toast = await this.toastCtrl.create({ message: 'Anmeldung gelöscht', duration: 2000, color: 'success' });
              await toast.present();
            } catch {
              const toast = await this.toastCtrl.create({ message: 'Fehler beim Löschen', duration: 3000, color: 'danger' });
              await toast.present();
            }
          }
        }
      ]
    });
    await alert.present();
  }

  getStatusBadge(reg: ClubRegistration): string {
    return this.isApproved(reg) ? 'Angenommen' : 'Neu';
  }

  getStatusColor(reg: ClubRegistration): string {
    return this.isApproved(reg) ? 'success' : 'warning';
  }
}
