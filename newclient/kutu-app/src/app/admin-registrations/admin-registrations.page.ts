import { Component, inject, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { AlertController, ToastController } from '@ionic/angular';
import { ActivatedRoute, Router } from '@angular/router';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { ClubRegistration, SyncAction, SyncActionKey, Verein, RiegeItem } from '../backend-types';
import { firstValueFrom } from 'rxjs';

@Component({
  templateUrl: 'admin-registrations.page.html',
  standalone: false
})
export class AdminRegistrationsPage implements OnDestroy {
  uuid = '';
  secret = '';
  wettkampfTitle = '';
  logoUrl = '';
  registrations: ClubRegistration[] = [];
  syncActions: SyncAction[] = [];
  selectedSyncIndices = new Set<number>();
  applying = false;
  loading = false;
  unassignedRiegenCount = 0;

  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
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

  ngOnDestroy() {
    if (this.logoUrl) URL.revokeObjectURL(this.logoUrl);
  }

  async loadData() {
    this.loading = true;
    try {
      const [registrations, syncActions, riegen] = await Promise.all([
        firstValueFrom(this.backend.getRegistrations(this.uuid, this.secret)),
        firstValueFrom(this.backend.getSyncActions(this.uuid, this.secret)).catch(() => [] as SyncAction[]),
        firstValueFrom(this.backend.getRiegen(this.uuid, this.secret)).catch(() => [] as RiegeItem[])
      ]);
      this.registrations = registrations;
      this.syncActions = syncActions;
      this.unassignedRiegenCount = riegen.filter(r => !r.durchgang).length;
    } catch {
      this.registrations = [];
      this.syncActions = [];
      this.unassignedRiegenCount = 0;
    } finally {
      this.loading = false;
      this.selectedSyncIndices.clear();
      this.cdr.detectChanges();
    }
    this.loadLogo();
  }

  private loadLogo() {
    if (this.logoUrl) URL.revokeObjectURL(this.logoUrl);
    this.logoUrl = '';
    this.backend.getCompetitionLogo(this.uuid, this.secret).subscribe({
      next: blob => {
        this.logoUrl = URL.createObjectURL(blob);
        this.cdr.detectChanges();
      },
      error: () => {}
    });
  }

  toggleSyncAction(index: number) {
    if (this.selectedSyncIndices.has(index)) {
      this.selectedSyncIndices.delete(index);
    } else {
      this.selectedSyncIndices.add(index);
    }
  }

  toggleAllSyncAction() {
    if (this.selectedSyncIndices.size < this.syncActions.length) {
      for(const index of [...this.syncActions.map((v,i)=>i)]) {
        if (!this.selectedSyncIndices.has(index)) {
          this.selectedSyncIndices.add(index);
        }
      }
    } else {
      this.selectedSyncIndices.clear();
    }
  }

  isAllSelected(): boolean {
    return this.selectedSyncIndices.size > 0 && this.selectedSyncIndices.size === this.syncActions.length;
  }

  isSelected(index: number): boolean {
    return this.selectedSyncIndices.has(index);
  }

  get selectedCount(): number {
    return this.selectedSyncIndices.size;
  }

  private actionToKey(action: SyncAction): SyncActionKey {
    return {
      registrationId: action.data.registrationId,
      actionType: action.data.type,
      caption: action.caption,
      athletId: action.data.athletId,
      oldVereinId: action.data.oldVereinId
    };
  }

  async applySelected() {
    if (this.selectedSyncIndices.size === 0) return;
    this.applying = true;
    const keys = Array.from(this.selectedSyncIndices)
      .map(i => this.syncActions[i])
      .filter(a => a?.data)
      .map(a => this.actionToKey(a));
    try {
      const result = await firstValueFrom(this.backend.applySyncActions(this.uuid, keys, this.secret));
      const toast = await this.toastCtrl.create({
        message: `${result.processed} Aktionen verarbeitet.${result.messages.length > 0 ? ' ' + result.messages.join(', ') : ''}`,
        duration: 3000,
        color: result.processed > 0 ? 'success' : 'warning'
      });
      await toast.present();
      await this.loadData();
    } catch (e: any) {
      const toast = await this.toastCtrl.create({ message: 'Fehler: ' + (e.message || e), duration: 3000, color: 'danger' });
      await toast.present();
    } finally {
      this.applying = false;
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

  goToRiegenEinteilung() {
    this.router.navigate(['/admin/riege-einteilung', this.uuid]);
  }
}
