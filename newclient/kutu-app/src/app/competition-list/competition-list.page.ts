import { Component, OnDestroy, inject, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { AlertController, ToastController, NavController } from '@ionic/angular';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { downloadBlob, formatDisplayDate } from '../utils';
import { firstValueFrom, forkJoin } from 'rxjs';

interface CompetitionListItem {
  uuid: string;
  titel: string;
  datum: string;
  secret: string;
  loading: boolean;
  error: boolean;
  logoUrl?: string;
}

@Component({
  templateUrl: 'competition-list.page.html',
  standalone: false
})
export class CompetitionListPage implements OnDestroy {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  competitions: CompetitionListItem[] = [];
  private selectedCompetition: CompetitionListItem | null = null;
  private cdr = inject(ChangeDetectorRef);
  private secretService = inject(SecretService);
  private backend = inject(AdminBackendService);
  private alertCtrl = inject(AlertController);
  private toastCtrl = inject(ToastController);
  private nav = inject(NavController);

  ngOnDestroy() {
    for (const c of this.competitions) {
      if (c.logoUrl) URL.revokeObjectURL(c.logoUrl);
    }
  }

  ionViewWillEnter() {
    const secrets = this.secretService.getSecrets();
    this.competitions = secrets.map(s => ({
      uuid: s.uuid,
      titel: s.titel,
      datum: s.datum,
      secret: s.secret,
      loading: true,
      error: false
    }));
    this.refreshFromServer();
  }

  formatDate(d: string): string {
    return formatDisplayDate(d);
  }

  private refreshFromServer() {
    const requests = this.competitions.map(c =>
      this.backend.getCompetitionDetails(c.uuid, c.secret)
    );
    if (requests.length === 0) {
      this.cdr.detectChanges();
      return;
    }
    forkJoin(requests.map(r => firstValueFrom(r).catch(() => null))).subscribe(results => {
      for (let i = 0; i < results.length; i++) {
        const data = results[i];
        if (data) {
          this.competitions[i].titel = data.titel;
          this.competitions[i].datum = data.datum;
          this.competitions[i].error = false;
        } else {
          this.competitions[i].error = true;
        }
        this.competitions[i].loading = false;
        this.loadLogo(i);
      }
      this.cdr.detectChanges();
    });
  }

  private loadLogo(index: number) {
    const c = this.competitions[index];
    this.backend.getCompetitionLogo(c.uuid, c.secret).subscribe({
      next: blob => {
        c.logoUrl = URL.createObjectURL(blob);
        this.cdr.detectChanges();
      },
      error: () => { /* no logo */ }
    });
  }

  async downloadZip(c: CompetitionListItem) {
    try {
      const blob = await firstValueFrom(this.backend.downloadCompetitionZip(c.uuid, c.secret));
      downloadBlob(blob, c.titel + '.zip');
    } catch (e) {
      const toast = await this.toastCtrl.create({
        message: 'Download fehlgeschlagen: ' + (e as any).message,
        duration: 3000,
        color: 'danger'
      });
      await toast.present();
    }
  }

  triggerUpload(c: CompetitionListItem) {
    this.selectedCompetition = c;
    this.fileInput.nativeElement.click();
  }

  async onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length || !this.selectedCompetition) return;

    const file = input.files[0];
    const c = this.selectedCompetition;
    input.value = '';

    const alert = await this.alertCtrl.create({
      header: 'Wettkampf hochladen',
      message: `Soll "${c.titel}" (${this.formatDate(c.datum)}) mit der Datei "${file.name}" überschrieben werden? Alle vorhandenen Daten werden überschrieben!`,
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Hochladen',
          role: 'destructive',
          handler: () => this.uploadZip(c, file)
        }
      ]
    });
    await alert.present();
  }

  private async uploadZip(c: CompetitionListItem, file: File) {
    try {
      await firstValueFrom(this.backend.uploadCompetitionZip(c.uuid, c.secret, file));
      const toast = await this.toastCtrl.create({
        message: 'Wettkampf erfolgreich hochgeladen.',
        duration: 2000,
        color: 'success'
      });
      await toast.present();
      this.refreshFromServer();
    } catch (e) {
      const toast = await this.toastCtrl.create({
        message: 'Upload fehlgeschlagen: ' + ((e as any).error || (e as any).message),
        duration: 3000,
        color: 'danger'
      });
      await toast.present();
    }
  }

  async confirmDelete(s: CompetitionListItem) {
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

  openEdit(s: CompetitionListItem) {
    this.nav.navigateRoot('/admin/competitions/' + s.uuid + '/edit');
  }

  openRiegen(s: CompetitionListItem) {
    this.nav.navigateRoot('/admin/riege-einteilung/' + s.uuid);
  }

  openRegistrations(s: CompetitionListItem) {
    this.nav.navigateRoot('/admin/registrations/' + s.uuid);
  }

  openPlaybook(s: CompetitionListItem) {
    this.nav.navigateRoot('/admin/playbook/' + s.uuid);
  }

  openRankings(s: CompetitionListItem) {
    this.nav.navigateRoot('/admin/rankings/' + s.uuid);
  }

  private async deleteCompetition(s: CompetitionListItem) {
    try {
      await firstValueFrom(this.backend.deleteCompetition(s.uuid, s.secret));
    } catch (e) {
      // Continue even if server delete fails
    }
    this.secretService.removeSecret(s.uuid);
    this.competitions = this.competitions.filter(c => c.uuid !== s.uuid);
    this.cdr.detectChanges();
    const toast = await this.toastCtrl.create({
      message: 'Wettkampf gelöscht.',
      duration: 2000,
      color: 'success'
    });
    await toast.present();
  }
}
