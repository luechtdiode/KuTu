import { Component, inject, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { AlertController, ToastController } from '@ionic/angular';
import { ActivatedRoute } from '@angular/router';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { RiegeItem, RiegeSuggestionRequest, DurchgangDurationItem, UpdateRiegeRequest, Geraet, StoredSecret } from '../backend-types';
import { firstValueFrom } from 'rxjs';

interface CellRiege {
  name: string;
  kind: number;
  athletCount: number;
}

interface TableRow {
  durchgangName: string;
  durchgangTitle: string;
  duration: DurchgangDurationItem | null;
  cells: { [disziplinId: number]: CellRiege[] };
}

interface TableGroup {
  title: string;
  rows: TableRow[];
}

@Component({
  templateUrl: 'riege-einteilung.page.html',
  standalone: false
})
export class RiegeEinteilungPage implements OnDestroy {
  uuid = '';
  secret = '';
  wettkampfTitle = '';
  logoUrl = '';

  disziplinen: Geraet[] = [];
  groups: TableGroup[] = [];
  unassignedRiegen: CellRiege[] = [];
  generateParams: RiegeSuggestionRequest = {
    maxRiegenSize: 11,
    maxParallelDg: 0,
    splitPgm: true,
    splitSexOption: '',
    separateRiegen2Durchgaenge: true
  };
  showGeneratePanel = false;
  loading = false;
  draggedRiege: { name: string; durchgang: string; startId: number | null } | null = null;

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

  ngOnDestroy() {
    if (this.logoUrl) URL.revokeObjectURL(this.logoUrl);
  }

  async loadData() {
    this.loading = true;
    try {
      const [riegen, disziplinen, durations] = await Promise.all([
        firstValueFrom(this.backend.getRiegen(this.uuid, this.secret)),
        firstValueFrom(this.backend.getDisziplinen(this.uuid)),
        firstValueFrom(this.backend.getRiegeDuration(this.uuid, this.secret)).catch(() => [] as DurchgangDurationItem[])
      ]);
      this.disziplinen = disziplinen;
      this.buildTable(riegen, durations);
    } catch {
      this.groups = [];
      this.disziplinen = [];
    } finally {
      this.loading = false;
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

  private buildTable(riegen: RiegeItem[], durations: DurchgangDurationItem[]) {
    this.unassignedRiegen = riegen
      .filter(r => !r.durchgang)
      .map(r => ({ name: r.name, kind: r.kind, athletCount: r.athletCount }));

    const durchgangNames = [...new Set(riegen.map(r => r.durchgang).filter(Boolean))] as string[];
    const durMap = new Map(durations.map(d => [d.name, d]));

    const rows = durchgangNames.map(dgName => {
      const cells: { [disziplinId: number]: CellRiege[] } = {};
      for (const d of this.disziplinen) {
        cells[d.id] = [];
      }
      const dgRiegen = riegen.filter(r => r.durchgang === dgName);
      for (const r of dgRiegen) {
        const sid = r.startId ?? -1;
        if (!cells[sid]) cells[sid] = [];
        cells[sid].push({ name: r.name, kind: r.kind, athletCount: r.athletCount });
      }
      return {
        durchgangName: dgName,
        durchgangTitle: durMap.get(dgName)?.title ?? dgName,
        duration: durMap.get(dgName) ?? null,
        cells
      };
    });

    const groupMap = new Map<string, TableRow[]>();
    for (const row of rows) {
      const t = row.durchgangTitle;
      if (!groupMap.has(t)) groupMap.set(t, []);
      groupMap.get(t)!.push(row);
    }

    this.groups = [...groupMap.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([title, groupRows]) => ({
        title,
        rows: groupRows.sort((a, b) => a.durchgangName.localeCompare(b.durchgangName))
      }));
  }

  async generate() {
    this.loading = true;
    try {
      const request: RiegeSuggestionRequest = {
        ...this.generateParams,
        splitSexOption: this.generateParams.splitSexOption || undefined,
        onDisziplinIds: this.generateParams.onDisziplinIds?.length ? this.generateParams.onDisziplinIds : undefined,
      };
      await firstValueFrom(this.backend.suggestRiegen(this.uuid, request, this.secret));
      await this.loadData();
      const toast = await this.toastCtrl.create({ message: 'Riegeneinteilung generiert', duration: 2000, color: 'success' });
      await toast.present();
    } catch (e) {
      const toast = await this.toastCtrl.create({ message: 'Fehler: ' + ((e as any).message || e), duration: 4000, color: 'danger' });
      await toast.present();
    } finally {
      this.loading = false;
    }
  }

  async resetRiegen() {
    const alert = await this.alertCtrl.create({
      header: 'Riegen zurücksetzen',
      message: 'Alle bestehenden Riegen, Durchgänge und Startzeiten werden gelöscht. Fortfahren?',
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Zurücksetzen', role: 'destructive',
          handler: async () => {
            try {
              await firstValueFrom(this.backend.resetRiegen(this.uuid, this.secret));
              await this.loadData();
              const toast = await this.toastCtrl.create({ message: 'Riegen zurückgesetzt', duration: 2000, color: 'success' });
              await toast.present();
            } catch {
              const toast = await this.toastCtrl.create({ message: 'Fehler beim Zurücksetzen', duration: 3000, color: 'danger' });
              await toast.present();
            }
          }
        }
      ]
    });
    await alert.present();
  }

  onDragStart(event: DragEvent, riegeName: string, durchgang: string, startId: number | null) {
    this.draggedRiege = { name: riegeName, durchgang, startId };
    event.dataTransfer?.setData('text/plain', JSON.stringify(this.draggedRiege));
    event.dataTransfer!.effectAllowed = 'move';
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.dataTransfer!.dropEffect = 'move';
    (event.currentTarget as HTMLElement)?.classList.add('drag-over');
  }

  onDragLeave(event: DragEvent) {
    const target = event.currentTarget as HTMLElement;
    target.classList.remove('drag-over');
  }

  onDrop(event: DragEvent, targetDurchgang: string, targetStartId: number) {
    event.preventDefault();
    (event.currentTarget as HTMLElement)?.classList.remove('drag-over');
    if (!this.draggedRiege) return;
    const { name, durchgang: srcDg, startId: srcSid } = this.draggedRiege;
    this.draggedRiege = null;
    if (srcDg === targetDurchgang && srcSid === targetStartId) return;
    const request: UpdateRiegeRequest = {
      name,
      durchgang: targetDurchgang,
      startId: targetStartId,
      kind: 0
    };
    this.backend.updateRiege(this.uuid, request, this.secret).subscribe({
      next: () => this.loadData(),
      error: async () => {
        const toast = await this.toastCtrl.create({ message: `Fehler beim Verschieben von ${name}`, duration: 3000, color: 'danger' });
        await toast.present();
      }
    });
  }

  async editRiege(item: CellRiege, durchgang: string, startId: number) {
    const alert = await this.alertCtrl.create({
      header: 'Riege bearbeiten',
      inputs: [
        { name: 'name', type: 'text', value: item.name, placeholder: 'Name' },
      ],
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Speichern',
          handler: async (data) => {
            if (data.name === item.name) return;
            try {
              const request: UpdateRiegeRequest =  { startId: startId, name: data.name, kind: item.kind, durchgang: durchgang };
              await firstValueFrom(this.backend.updateRiege(this.uuid, request, this.secret));
              await this.loadData();
            } catch {
              const toast = await this.toastCtrl.create({ message: 'Fehler beim Speichern', duration: 3000, color: 'danger' });
              await toast.present();
            }
          }
        }
      ]
    });
    await alert.present();
  }

  formatMillis(ms: number): string {
    if (ms <= 0) return '-';
    const totalSec = Math.floor(ms / 1000);
    const min = Math.floor(totalSec / 60);
    const sec = totalSec % 60;
    return `${min}:${sec.toString().padStart(2, '0')}`;
  }

  totalAthleten(): number {
    return this.groups.reduce((sum, g) => sum + g.rows.reduce((s, r) => s + Object.values(r.cells).reduce((s2, c) => s2 + c.reduce((a, ri) => a + ri.athletCount, 0), 0), 0), 0);
  }
}
