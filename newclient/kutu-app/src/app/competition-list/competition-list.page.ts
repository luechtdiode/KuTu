import { Component, OnDestroy, inject, signal, ViewChild, ElementRef } from '@angular/core';
import { NavController, AlertController, ToastController } from '@ionic/angular';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { formatDisplayDate } from '../utils';
import { firstValueFrom, forkJoin } from 'rxjs';
import { unzipSync } from 'fflate';

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
  @ViewChild('zipFileInput') zipFileInput!: ElementRef<HTMLInputElement>;
  competitions = signal<CompetitionListItem[]>([]);
  isUploading = signal(false);
  private secretService = inject(SecretService);
  private backend = inject(AdminBackendService);
  private nav = inject(NavController);
  private alertCtrl = inject(AlertController);
  private toastCtrl = inject(ToastController);

  ngOnDestroy() {
    for (const c of this.competitions()) {
      if (c.logoUrl) URL.revokeObjectURL(c.logoUrl);
    }
  }

  ionViewWillEnter() {
    const secrets = this.secretService.getSecrets();
    this.competitions.set(secrets.map(s => ({
      uuid: s.uuid,
      titel: s.titel,
      datum: s.datum,
      secret: s.secret,
      loading: true,
      error: false
    })));
    this.refreshFromServer();
  }

  formatDate(d: string): string {
    return formatDisplayDate(d);
  }

  openOverview(c: CompetitionListItem) {
    this.nav.navigateRoot('/admin/competitions/' + c.uuid);
  }

  triggerZipImport() {
    this.zipFileInput.nativeElement.click();
  }

  async onZipFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    input.value = '';

    this.isUploading.set(true);
    try {
      const arrayBuffer = await file.arrayBuffer();
      const files = unzipSync(new Uint8Array(arrayBuffer));

      const csvEntry = files['wettkampf.csv'];
      if (!csvEntry) {
        const toast = await this.toastCtrl.create({
          message: 'Keine Wettkampfdaten in der ZIP-Datei gefunden.',
          duration: 3000,
          color: 'danger'
        });
        await toast.present();
        return;
      }

      const csvContent = new TextDecoder().decode(csvEntry);
      const lines = csvContent.split('\n').filter(l => l.trim());
      if (lines.length < 2) {
        const toast = await this.toastCtrl.create({
          message: 'Wettkampfdatei ist leer oder ungültig.',
          duration: 3000,
          color: 'danger'
        });
        await toast.present();
        return;
      }

      const headerLine = lines[0].replace(/^\uFEFF/, '');
      const headers = headerLine.split('","').map(h => h.replace(/^"|"$/g, ''));
      const values = lines[1].split(',');

      const uuidIdx = headers.indexOf('uuid');
      const titelIdx = headers.indexOf('titel');
      const datumIdx = headers.indexOf('datum');

      if (uuidIdx === -1) {
        const toast = await this.toastCtrl.create({
          message: 'Ungültiges Wettkampf-Format in der ZIP-Datei.',
          duration: 3000,
          color: 'danger'
        });
        await toast.present();
        return;
      }

      const uuid = values[uuidIdx]?.replace(/^"|"$/g, '') || '';
      const titel = titelIdx >= 0 ? (values[titelIdx]?.replace(/^"|"$/g, '') || 'Unbekannt') : 'Unbekannt';
      const datum = datumIdx >= 0 ? (values[datumIdx]?.replace(/^"|"$/g, '') || '') : '';

      await this.createNewCompetition(files, uuid, titel, datum, file);

    } catch (e) {
      const toast = await this.toastCtrl.create({
        message: 'Fehler beim Lesen der ZIP-Datei: ' + ((e as Error).message || e),
        duration: 3000,
        color: 'danger'
      });
      await toast.present();
    } finally {
      this.isUploading.set(false);
    }
  }

  private findOriginSecret(files: Record<string, Uint8Array>): string | null {
    const origin = location.hostname;
    const atEntry = Object.keys(files).find(name => name === `.at.${origin}`);
    return atEntry ? new TextDecoder().decode(files[atEntry]) : null;
  }

  private async createNewCompetition(files: Record<string, Uint8Array>, uuid: string, titel: string, datum: string, file: File) {
    try {
      const response = await firstValueFrom(this.backend.uploadCompetitionZipPost(uuid, file));
      const newSecret = response.headers.get('x-access-token');
      if (newSecret) {
        this.secretService.saveSecret({ uuid, titel, datum, secret: newSecret });
      }

      const toast = await this.toastCtrl.create({
        message: `Wettkampf "${titel}" angelegt! Bestätige dein Email-Postfach zur Verifizierung (1h Timeout).`,
        duration: 4000,
        color: 'success'
      });
      await toast.present();
      this.ionViewWillEnter();

    } catch (e) {
      const status = (e as any).status;
      if (status === 409) {
        const body = (e as any).error || '';
        if (body.includes('kann nicht mehrfach')) {
          const secret = this.findOriginSecret(files);
          if (!secret) {
            const origin = location.hostname;
            const toast = await this.toastCtrl.create({
              message: `Kein Schlüssel für diesen Server (".at.${origin}") in der ZIP-Datei gefunden. Der Wettkampf muss zuerst von diesem Server aus exportiert werden.`,
              duration: 4000,
              color: 'danger'
            });
            await toast.present();
            return;
          }
          this.secretService.saveSecret({ uuid, titel, datum, secret });
          const alert = await this.alertCtrl.create({
            header: 'Wettkampf existiert bereits',
            message: `Der Wettkampf "${titel}" ist bereits auf dem Server vorhanden.
            Sollen die Daten aus dem Backup wiederhergestellt werden?
            ACHTUNG: Alle vorhandenen Daten werden überschrieben!`,
            buttons: [
              { text: 'Nein', role: 'cancel' },
              {
                text: 'Ja, wiederherstellen',
                role: 'destructive',
                handler: () => this.restoreZipData(uuid, secret, file, titel, datum)
              }
            ]
          });
          await alert.present();
        } else {
          const toast = await this.toastCtrl.create({
            message: body || 'Wettkampf konnte nicht angelegt werden.',
            duration: 4000,
            color: 'danger'
          });
          await toast.present();
        }
      } else {
        const toast = await this.toastCtrl.create({
          message: 'Upload fehlgeschlagen: ' + ((e as any).error || (e as any).message),
          duration: 3000,
          color: 'danger'
        });
        await toast.present();
      }
    }
  }

  private async restoreZipData(uuid: string, secret: string, file: File, titel: string, datum: string) {
    try {
      await firstValueFrom(this.backend.uploadCompetitionZip(uuid, secret, file));
      this.secretService.saveSecret({ uuid, titel, datum, secret });

      const toast = await this.toastCtrl.create({
        message: `Daten für "${titel}" erfolgreich wiederhergestellt.`,
        duration: 2000,
        color: 'success'
      });
      await toast.present();
      this.ionViewWillEnter();
    } catch (e) {
      const status = (e as any).status;
      if (status === 401) {
        this.secretService.removeSecret(uuid);
      }
      const toast = await this.toastCtrl.create({
        message: 'Upload fehlgeschlagen: ' + ((e as any).error || (e as any).message),
        duration: 3000,
        color: 'danger'
      });
      await toast.present();
    }
  }

  private refreshFromServer() {
    const requests = this.competitions().map(c =>
      this.backend.getCompetitionDetails(c.uuid, c.secret)
    );
    if (requests.length === 0) {
      return;
    }
    forkJoin(requests.map(r => firstValueFrom(r).catch(() => null))).subscribe(async results => {
      const toRemove: string[] = [];
      for (let i = 0; i < results.length; i++) {
        const c = this.competitions()[i];
        const data = results[i];
        if (!data) {
          const expired = await this.isExpired(c.secret);
          if (expired) {
            toRemove.push(c.uuid);
            continue;
          }
        }
        this.competitions.update(list => {
          const copy = [...list];
          copy[i] = data
            ? { ...copy[i], titel: data.titel, datum: data.datum, error: false, loading: false }
            : { ...copy[i], error: true, loading: false };
          return copy;
        });
        this.loadLogo(i);
      }
      if (toRemove.length > 0) {
        for (const uuid of toRemove) {
          this.secretService.removeSecret(uuid);
        }
        this.competitions.update(list => list.filter(c => !toRemove.includes(c.uuid)));
        this.showRemovedMessage(toRemove.length);
      }
    });
  }

  private async isExpired(secret: string): Promise<boolean> {
    try {
      await firstValueFrom(this.backend.isTokenExpired(secret));
      return false;
    } catch {
      return true;
    }
  }

  private async showRemovedMessage(count: number) {
    const toast = await this.toastCtrl.create({
      message: count === 1
        ? 'Die Berechtigung für einen Wettkampf ist abgelaufen. Der Wettkampf wurde entfernt.'
        : `Die Berechtigungen für ${count} Wettkämpfe sind abgelaufen. Die Wettkämpfe wurden entfernt.`,
      duration: 3000,
      color: 'danger'
    });
    await toast.present();
  }

  private loadLogo(index: number) {
    const c = this.competitions()[index];
    this.backend.getCompetitionLogo(c.uuid, c.secret).subscribe({
      next: blob => {
        const logoUrl = URL.createObjectURL(blob);
        this.competitions.update(list => {
          const copy = [...list];
          copy[index] = { ...copy[index], logoUrl };
          return copy;
        });
      },
      error: () => { /* no logo */ }
    });
  }
}
