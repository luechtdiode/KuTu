import { Component, computed, inject, OnDestroy, signal } from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {AlertController, IonItemSliding, ModalController, ToastController} from '@ionic/angular';
import { firstValueFrom, Subscription } from 'rxjs';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { WsStateService } from '../services/ws-state.service';
import { TeilnehmerEditModalComponent } from './teilnehmer-edit-modal.component';
import {Teilnehmer, TeilnehmerImportRow, TeilnehmerItem} from '../backend-types';

@Component({
  templateUrl: 'admin-teilnehmer.page.html',
  standalone: false
})
export class AdminTeilnehmerPage implements OnDestroy {
  private router = inject(Router);

  uuid = '';
  secret = '';
  items = signal<TeilnehmerItem[]>([]);
  filter = signal('');
  loading = signal(false);
  filtered = computed(() => {
    const tokens = this.filter().toLowerCase().split(/\s+/).filter(t => t.length > 0);
    if (tokens.length === 0) return this.items();
    return this.items().filter(i => {
      const hay = [i.name, i.vorname, i.verein, i.riege, i.riege2, i.teamName]
        .map(v => (v || '').toLowerCase()).join('\u0001');
      return tokens.every(t => hay.includes(t));
    });
  });

  selectedId = signal<number | null>(null);

  importFile = signal<File | null>(null);
  importRows = signal<TeilnehmerImportRow[]>([]);
  importSelected = signal<Set<string>>(new Set());

  private subscriptions: Subscription[] = [];
  private wsAcquired = false;
  private route = inject(ActivatedRoute);
  private secretService = inject(SecretService);
  private backend = inject(AdminBackendService);
  private wsState = inject(WsStateService);
  private toastCtrl = inject(ToastController);
  private modalCtrl = inject(ModalController);
  private alertCtrl = inject(AlertController);

  async ionViewWillEnter() {
    this.uuid = this.route.snapshot.paramMap.get('uuid') || '';
    const stored = this.secretService.getSecret(this.uuid);
    if (stored) this.secret = stored.secret;
    await this.loadData();
    if (!this.wsAcquired) {
      this.wsAcquired = true;
      this.subscriptions.push(
        this.wsState.athletAssignmentChanged.subscribe(() => this.loadData()),
        this.wsState.registrationSyncUpdated.subscribe(() => this.loadData())
      );
      this.wsState.acquire({kind: 'competition', competitionId: this.uuid});
    }
  }

  ngOnDestroy() {
    if (this.wsAcquired) {
      this.wsState.release({kind: 'competition', competitionId: this.uuid});
      this.wsAcquired = false;
    }
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  async loadData() {
    this.loading.set(true);
    try {
      this.items.set(await firstValueFrom(this.backend.getTeilnehmer(this.uuid, this.secret)));
    } catch {
      this.items.set([]);
    } finally {
      this.loading.set(false);
    }
  }

  onFilter(ev: any) {
    this.filter.set(ev?.detail?.value ?? '');
  }

  select(i: TeilnehmerItem) {
    this.selectedId.set(i.athletId);
  }

  async edit(i: TeilnehmerItem) {
    this.selectedId.set(i.athletId);
    const modal = await this.modalCtrl.create({
      component: TeilnehmerEditModalComponent,
      componentProps: { uuid: this.uuid, secret: this.secret, item: i }
    });
    await modal.present();
    const { data } = await modal.onDidDismiss();
    if (data) {
      this.items.set(this.items().map(x => x.athletId === data.athletId ? data : x));
      //await this.loadData();
    }
  }

  private ids(): number[] {
    return this.filter().trim() ? this.filtered().map(i => i.athletId) : [];
  }

  private download(blob: Blob, filename: string) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 10000);
  }

  async exportExcel(includeData: boolean) {
    try {
      const blob = await firstValueFrom(this.backend.exportTeilnehmerExcel(this.uuid, this.secret, includeData, this.ids()));
      this.download(blob, includeData ? 'Teilnehmerliste.xlsx' : 'Teilnehmer-Vorlage.xlsx');
    } catch (e) {
      await this.toast('Export fehlgeschlagen', 'danger');
    }
  }

  async print(kind: 'notenblatt' | 'teilnehmer' | 'vereine' | 'durchgang' | 'riegenblatt') {
    const win = window.open('', '_blank');
    try {
      const blob = await firstValueFrom(this.backend.getTeilnehmerPrint(this.uuid, kind, this.ids(), this.secret));
      const url = URL.createObjectURL(new Blob([blob], { type: 'text/html;charset=utf-8' }));
      if (win) win.location.href = url; else window.open(url, '_blank');
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch (e) {
      win?.close();
      await this.toast('Keine Daten zum Drucken', 'danger');
    }
  }

  async onImportFile(ev: Event) {
    const input = ev.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;
    input.value = '';
    if (!file) return;
    try {
      const preview = await firstValueFrom(this.backend.previewTeilnehmerImport(this.uuid, file, this.secret));
      this.importFile.set(file);
      this.importRows.set(preview.rows);
      this.importSelected.set(new Set(preview.rows.map(r => r.key)));
      if (preview.rows.length === 0) {
        this.importFile.set(null);
        await this.toast('Keine Änderungen gefunden', 'medium');
      }
    } catch (e) {
      await this.toast('Import-Vorschau fehlgeschlagen', 'danger');
    }
  }

  toggleImport(key: string) {
    const next = new Set(this.importSelected());
    if (next.has(key)) next.delete(key); else next.add(key);
    this.importSelected.set(next);
  }

  cancelImport() {
    this.importFile.set(null);
    this.importRows.set([]);
    this.importSelected.set(new Set());
  }

  async applyImport() {
    const file = this.importFile();
    if (!file) return;
    try {
      const res = await firstValueFrom(this.backend.applyTeilnehmerImport(this.uuid, file, [...this.importSelected()], this.secret));
      await this.toast(res.applied + ' Änderungen übernommen');
      this.cancelImport();
      await this.loadData();
    } catch (e) {
      await this.toast('Import fehlgeschlagen', 'danger');
    }
  }

  goToRiegenEinteilung() {
    this.router.navigate(['/admin/riege-einteilung', this.uuid]);
  }

  private async toast(message: string, color: 'success' | 'danger' | 'medium' = 'success') {
    const toast = await this.toastCtrl.create({
      message,
      duration: 2500,
      color
    });
    await toast.present();
  }

  async unassignFromCompetition(item: TeilnehmerItem) {
    const alert = await this.alertCtrl.create({
      header: 'Athlet aus Wettkampf entfernen',
      message: `${item.vorname} ${item.name} (${item.verein}) wird aus der Wettkampf-Einteilung entfernt.`,
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Entfernen',
          role: 'destructive',
          handler: () => {
            this.backend.unassignAthletFromCompetition(this.uuid, item.athletId, this.secret).subscribe({
              next: async (result) => {
                const removed = result?.removedWertungen || 0;
                await this.toast(`Athlet aus Wettkampf entfernt (${removed} Wertungen).`, 'success');
                await this.loadData();
              },
              error: async () => {
                await this.toast('Fehler beim Entfernen des Athleten.', 'danger');
              }
            });
          }
        }
      ]
    });
    await alert.present();
  }


}
