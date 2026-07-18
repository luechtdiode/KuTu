import {Component, inject, ChangeDetectorRef, OnDestroy} from '@angular/core';
import { AlertController, ToastController } from '@ionic/angular';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { StoredSecret } from '../backend-types';
import {firstValueFrom, forkJoin} from 'rxjs';


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
  templateUrl: 'security.page.html',
  standalone: false
})
export class SecurityPage implements OnDestroy {
  secrets: StoredSecret[] = [];
  competitions: CompetitionListItem[] = [];
  private cdr = inject(ChangeDetectorRef);
  private secretService = inject(SecretService);
  private backend = inject(AdminBackendService);
  private alertCtrl = inject(AlertController);
  private toastCtrl = inject(ToastController);

  ngOnDestroy() {
    for (const c of this.competitions) {
      if (c.logoUrl) URL.revokeObjectURL(c.logoUrl);
    }
  }

  ionViewWillEnter() {
    this.secrets = this.secretService.getSecrets();
    this.competitions = this.secrets.map(s => ({
      uuid: s.uuid,
      titel: s.titel,
      datum: s.datum,
      secret: s.secret,
      loading: true,
      error: false
    }));
    this.refreshFromServer();
    this.cdr.detectChanges();
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
