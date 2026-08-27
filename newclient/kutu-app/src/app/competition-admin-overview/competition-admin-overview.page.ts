import { Component, OnDestroy, inject, signal, ViewChild, ElementRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AlertController, ToastController, NavController, ModalController } from '@ionic/angular';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { downloadBlob, formatDisplayDate } from '../utils';
import { firstValueFrom } from 'rxjs';
import { OverviewLinks } from '../backend-types';
import { AdminAccessLinkModalComponent } from './admin-access-link-modal.component';
import {StandardLinkModalComponent} from "./standard-link-modal.component";

@Component({
  templateUrl: 'competition-admin-overview.page.html',
  standalone: false
})
export class CompetitionAdminOverviewPage implements OnDestroy {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  uuid = '';
  titel = signal('');
  datum = signal('');
  secret = '';
  logoUrl = signal<string | null>(null);
  overviewLinks = signal<OverviewLinks | null>(null);
  loading = signal(true);
  isDownloading = signal(false);
  isUploading = signal(false);
  private route = inject(ActivatedRoute);
  private secretService = inject(SecretService);
  private backend = inject(AdminBackendService);
  private alertCtrl = inject(AlertController);
  private toastCtrl = inject(ToastController);
  private nav = inject(NavController);
  private modalCtrl = inject(ModalController);

  ngOnDestroy() {
    const logo = this.logoUrl();
    if (logo) URL.revokeObjectURL(logo);
  }

  ionViewWillEnter() {
    this.uuid = this.route.snapshot.paramMap.get('uuid') || '';
    const stored = this.secretService.getSecret(this.uuid);
    if (!stored) {
      this.nav.navigateRoot('/admin/competitions');
      return;
    }
    this.titel.set(stored.titel);
    this.datum.set(stored.datum);
    this.secret = stored.secret;
    this.loading.set(true);
    this.loadDetails();
  }

  formatDate(d: string): string {
    return formatDisplayDate(d);
  }

  private loadDetails() {
    firstValueFrom(this.backend.getCompetitionDetails(this.uuid, this.secret)).then(data => {
      this.titel.set(data.titel);
      this.datum.set(data.datum);
      this.secretService.updateStoredSecretTitelDatum(this.uuid, data.titel, data.datum);
      this.loading.set(false);
    }).catch(() => {
      this.loading.set(false);
    });

    this.backend.getCompetitionLogo(this.uuid, this.secret).subscribe({
      next: blob => {
        this.logoUrl.set(URL.createObjectURL(blob));
      },
      error: () => { /* no logo */ }
    });

    this.backend.getOverviewLinks(this.uuid, this.secret).subscribe({
      next: links => {
        this.overviewLinks.set(links);
      },
      error: () => { /* ignore */ }
    });
  }

  async copyUrl(url: string) {
    try {
      await navigator.clipboard.writeText(url);
      const toast = await this.toastCtrl.create({
        message: 'Link kopiert', duration: 1500, color: 'success', position: 'bottom'
      });
      await toast.present();
    } catch {
      const toast = await this.toastCtrl.create({
        message: 'Konnte Link nicht kopieren', duration: 2000, color: 'danger'
      });
      await toast.present();
    }
  }

  async openRegistratinoLinkModal() {
    if (!this.overviewLinks()?.registrationUrl) return;
    const modal = await this.modalCtrl.create({
      component: StandardLinkModalComponent,
      componentProps: {
        title: 'Link zur Online Vereinsanmeldung',
        description: 'Scanne den QR-Code mit einem anderen Gerät oder öffne den Link, um dort Online Vereinsanmeldung zu öffnen.',
        link: this.overviewLinks()!.registrationUrl,
        qrUrl: this.overviewLinks()!.registrationQr
      }
    });
    await modal.present();
  }

  async openStartListLinkModal() {
    if (!this.overviewLinks()?.startListUrl) return;
    const modal = await this.modalCtrl.create({
      component: StandardLinkModalComponent,
      componentProps: {
        title: 'Link zur Online Startliste',
        description: 'Scanne den QR-Code mit einem anderen Gerät oder öffne den Link, um dort Online Startliste zu öffnen.',
        link: this.overviewLinks()!.startListUrl,
        qrUrl: this.overviewLinks()!.startListQr
      }
    });
    await modal.present();
  }

  async openLiveViewLinkModal() {
    if (!this.overviewLinks()?.liveResultsUrl) return;
    const modal = await this.modalCtrl.create({
      component: StandardLinkModalComponent,
      componentProps: {
        title: 'Link zu den Live-Ergebnissen',
        description: 'Scanne den QR-Code mit einem anderen Gerät oder öffne den Link, um die Live-Ergebnisse zu sehen.',
        link: this.overviewLinks()!.liveResultsUrl,
        qrUrl: this.overviewLinks()!.liveResultsQr
      }
    });
    await modal.present();
  }

  async openAdminAccess() {
    if (!this.overviewLinks()?.adminAccessUrl) return;
    const modal = await this.modalCtrl.create({
      component: AdminAccessLinkModalComponent,
      cssClass: 'admin-access-link-modal',
      componentProps: {
        link: this.overviewLinks()!.adminAccessUrl,
        qrUrl: this.overviewLinks()!.adminAccessQr,
        uuid: this.uuid,
        secret: this.secret
      }
    });
    await modal.present();
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

  openScoreCalc() {
    this.nav.navigateRoot('/admin/scorecalc/' + this.uuid);
  }

  async downloadZip() {
    this.isDownloading.set(true);
    try {
      const blob = await firstValueFrom(this.backend.downloadCompetitionZip(this.uuid, this.secret));
      downloadBlob(blob, this.titel() + '.zip');
    } catch (e) {
      const toast = await this.toastCtrl.create({
        message: 'Download fehlgeschlagen: ' + (e as any).message,
        duration: 3000,
        color: 'danger'
      });
      await toast.present();
    } finally {
      this.isDownloading.set(false);
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
      message: `Soll "${this.titel()}" (${this.formatDate(this.datum())}) mit der Datei "${file.name}" überschrieben werden? Alle vorhandenen Daten werden überschrieben!`,
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
    this.isUploading.set(true);
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
    } finally {
      this.isUploading.set(false);
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
      message: `Soll "${this.titel()}" (${this.formatDate(this.datum())}) wirklich gelöscht werden?
                Alle Daten werden unwiderruflich gelöscht.`,
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
