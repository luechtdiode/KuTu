import { Component, OnDestroy, inject, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AlertController, ToastController, NavController } from '@ionic/angular';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { downloadBlob, formatDisplayDate } from '../utils';
import { firstValueFrom } from 'rxjs';

@Component({
  templateUrl: 'competition-admin-overview.page.html',
  standalone: false
})
export class CompetitionAdminOverviewPage implements OnDestroy {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  uuid = '';
  titel = '';
  datum = '';
  secret = '';
  logoUrl: string | null = null;
  loading = true;
  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private secretService = inject(SecretService);
  private backend = inject(AdminBackendService);
  private alertCtrl = inject(AlertController);
  private toastCtrl = inject(ToastController);
  private nav = inject(NavController);

  ngOnDestroy() {
    if (this.logoUrl) URL.revokeObjectURL(this.logoUrl);
  }

  ionViewWillEnter() {
    this.uuid = this.route.snapshot.paramMap.get('uuid') || '';
    const stored = this.secretService.getSecret(this.uuid);
    if (!stored) {
      this.nav.navigateRoot('/admin/competitions');
      return;
    }
    this.titel = stored.titel;
    this.datum = stored.datum;
    this.secret = stored.secret;
    this.loading = true;
    this.loadDetails();
  }

  formatDate(d: string): string {
    return formatDisplayDate(d);
  }

  private loadDetails() {
    firstValueFrom(this.backend.getCompetitionDetails(this.uuid, this.secret)).then(data => {
      this.titel = data.titel;
      this.datum = data.datum;
      this.secretService.updateStoredSecretTitelDatum(this.uuid, data.titel, data.datum);
      this.loading = false;
      this.cdr.detectChanges();
    }).catch(() => {
      this.loading = false;
      this.cdr.detectChanges();
    });

    this.backend.getCompetitionLogo(this.uuid, this.secret).subscribe({
      next: blob => {
        this.logoUrl = URL.createObjectURL(blob);
        this.cdr.detectChanges();
      },
      error: () => { /* no logo */ }
    });
  }

  get registrationUrl(): string {
    const base = window.location.origin;
    const payload = btoa(`registration&c=${this.uuid}`);
    return `${base}/?${payload}`;
  }

  get liveResultsUrl(): string {
    const base = window.location.origin;
    const payload = btoa(`last&c=${this.uuid}`);
    return `${base}/?${payload}`;
  }

  openEdit() {
    this.nav.navigateRoot('/admin/competitions/' + this.uuid + '/edit');
  }

  openRegistrations() {
    this.nav.navigateRoot('/admin/registrations/' + this.uuid);
  }

  openRiegen() {
    this.nav.navigateRoot('/admin/riege-einteilung/' + this.uuid);
  }

  openPlaybook() {
    this.nav.navigateRoot('/admin/playbook/' + this.uuid);
  }

  openRankings() {
    this.nav.navigateRoot('/admin/rankings/' + this.uuid);
  }

  async downloadZip() {
    try {
      const blob = await firstValueFrom(this.backend.downloadCompetitionZip(this.uuid, this.secret));
      downloadBlob(blob, this.titel + '.zip');
    } catch (e) {
      const toast = await this.toastCtrl.create({
        message: 'Download fehlgeschlagen: ' + (e as any).message,
        duration: 3000,
        color: 'danger'
      });
      await toast.present();
    }
  }

  triggerUpload() {
    this.fileInput.nativeElement.click();
  }

  async onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;

    const file = input.files[0];
    input.value = '';

    const alert = await this.alertCtrl.create({
      header: 'Wettkampf hochladen',
      message: `Soll "${this.titel}" (${this.formatDate(this.datum)}) mit der Datei "${file.name}" überschrieben werden? Alle vorhandenen Daten werden überschrieben!`,
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Hochladen',
          role: 'destructive',
          handler: () => this.uploadZip(file)
        }
      ]
    });
    await alert.present();
  }

  private async uploadZip(file: File) {
    try {
      await firstValueFrom(this.backend.uploadCompetitionZip(this.uuid, this.secret, file));
      const toast = await this.toastCtrl.create({
        message: 'Wettkampf erfolgreich hochgeladen.',
        duration: 2000,
        color: 'success'
      });
      await toast.present();
      this.loadDetails();
    } catch (e) {
      const toast = await this.toastCtrl.create({
        message: 'Upload fehlgeschlagen: ' + ((e as any).error || (e as any).message),
        duration: 3000,
        color: 'danger'
      });
      await toast.present();
    }
  }

  copyCompetition() {
    this.nav.navigateRoot('/admin/competitions/create', {
      queryParams: { copyFrom: this.uuid }
    });
  }

  async confirmDelete() {
    const alert = await this.alertCtrl.create({
      header: 'Wettkampf löschen',
      message: `Soll "${this.titel}" (${this.formatDate(this.datum)}) wirklich gelöscht werden?<br><br><strong>Alle Daten werden unwiderruflich gelöscht.</strong>`,
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Löschen',
          role: 'destructive',
          handler: () => this.deleteCompetition()
        }
      ]
    });
    await alert.present();
  }

  private async deleteCompetition() {
    try {
      await firstValueFrom(this.backend.deleteCompetition(this.uuid, this.secret));
    } catch (e) {
      // Continue even if server delete fails
    }
    this.secretService.removeSecret(this.uuid);
    const toast = await this.toastCtrl.create({
      message: 'Wettkampf gelöscht.',
      duration: 2000,
      color: 'success'
    });
    await toast.present();
    this.nav.navigateRoot('/admin/competitions');
  }
}
