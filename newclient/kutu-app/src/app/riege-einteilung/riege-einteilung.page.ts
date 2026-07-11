import { Component, inject, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { AlertController, ToastController, ModalController } from '@ionic/angular';
import { ActivatedRoute } from '@angular/router';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { RiegeItem, RiegeSuggestionRequest, DurchgangDurationItem, UpdateRiegeRequest, Geraet, MergeDurchgangRequest, GroupDurchgangRequest, UngroupDurchgangRequest } from '../backend-types';
import { firstValueFrom } from 'rxjs';
import { RiegeEditModalComponent } from '../editors/riege-edit-modal.component';

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
  durchgangNames: string[] = [];
  groups: TableGroup[] = [];
  unassignedRiegen: CellRiege[] = [];
  generateParams: RiegeSuggestionRequest = {
    maxRiegenSize: 11,
    maxParallelDg: 0,
    splitPgm: true,
    splitSexOption: '',
    separateRiegen2Durchgaenge: true
  };
  selectedDisziplinIds = new Set<number>();
  filterDurchgaenge: string[] = [];
  selectedDgs = new Set<string>();
  showGeneratePanel = false;
  loading = false;
  draggedRiege: { name: string; durchgang: string; startId: number | null } | null = null;

  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private secretService = inject(SecretService);
  private backend = inject(AdminBackendService);
  private alertCtrl = inject(AlertController);
  private toastCtrl = inject(ToastController);
  private modalCtrl = inject(ModalController);

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
      if (this.selectedDisziplinIds.size === 0) {
        this.selectedDisziplinIds = new Set(disziplinen.map(d => d.id));
      }
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
    this.durchgangNames = durchgangNames;
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

  get hasGroupedRiegen(): boolean {
    return this.groups.map(tg => tg.rows.length).filter(l => l > 1).length > 0;
  }
  async generate() {
    this.loading = true;
    try {
      const allSelected = this.selectedDisziplinIds.size === this.disziplinen.length;
      const request: RiegeSuggestionRequest = {
        ...this.generateParams,
        splitSexOption: this.generateParams.splitSexOption || undefined,
        onDisziplinIds: !allSelected ? [...this.selectedDisziplinIds] : undefined,
        filterDurchgang: this.filterDurchgaenge.length > 0 ? this.filterDurchgaenge : undefined,
      };
      await firstValueFrom(this.backend.suggestRiegen(this.uuid, request, this.secret));
      this.filterDurchgaenge = [];
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
    const modal = await this.modalCtrl.create({
      component: RiegeEditModalComponent,
      componentProps: {
        name: item.name,
        durchgang,
        startId,
        kind: item.kind,
        disziplinen: this.disziplinen,
      }
    });
    modal.onDidDismiss().then(async (result) => {
      if (!result.data) return;
      const { name, durchgang: newDg, startId: newStartId, kind } = result.data;
      if (name === item.name && newDg === durchgang && newStartId === startId) return;
      try {
        const request: UpdateRiegeRequest = { name, durchgang: newDg, startId: newStartId, kind };
        await firstValueFrom(this.backend.updateRiege(this.uuid, request, this.secret));
        await this.loadData();
      } catch {
        const toast = await this.toastCtrl.create({ message: 'Fehler beim Speichern', duration: 3000, color: 'danger' });
        await toast.present();
      }
    });
    await modal.present();
  }

  get allDgsSelected(): boolean {
    return this.durchgangNames.length > 0 && this.durchgangNames.every(n => this.selectedDgs.has(n));
  }

  toggleDg(name: string) {
    if (this.selectedDgs.has(name)) {
      this.selectedDgs.delete(name);
    } else {
      this.selectedDgs.add(name);
    }
  }

  toggleAllDgs() {
    if (this.allDgsSelected) {
      this.selectedDgs.clear();
    } else {
      this.selectedDgs = new Set(this.durchgangNames);
    }
  }

  isGroupSelected(group: TableGroup): boolean {
    return group.rows.length > 0 && group.rows.every(r => this.selectedDgs.has(r.durchgangName));
  }

  toggleGroupDgs(group: TableGroup) {
    if (this.isGroupSelected(group)) {
      for (const r of group.rows) this.selectedDgs.delete(r.durchgangName);
    } else {
      for (const r of group.rows) this.selectedDgs.add(r.durchgangName);
    }
  }

  async renameSelected() {
    if (this.selectedDgs.size !== 1) return;
    const dgName = [...this.selectedDgs][0];
    const alert = await this.alertCtrl.create({
      header: 'Durchgang umbenennen',
      inputs: [{ name: 'newName', type: 'text', value: dgName, placeholder: 'Neuer Name' }],
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Umbenennen',
          handler: async (data) => {
            const newName = data.newName?.trim();
            if (!newName || newName === dgName) return;
            this.loading = true;
            try {
              await firstValueFrom(this.backend.renameDurchgang(this.uuid, this.secret, dgName, newName));
              this.selectedDgs.clear();
              await this.loadData();
              const toast = await this.toastCtrl.create({ message: `Durchgang umbenannt zu "${newName}"`, duration: 2000, color: 'success' });
              await toast.present();
            } catch {
              const toast = await this.toastCtrl.create({ message: 'Fehler beim Umbenennen', duration: 3000, color: 'danger' });
              await toast.present();
            } finally {
              this.loading = false;
            }
          }
        }
      ]
    });
    await alert.present();
  }

  async switchGroupSelected() {
    if (this.selectedDgs.size === 0) return;
    const allTitles = [...new Set(this.groups.map(g => g.title))];
    const options = allTitles.map(t => ({ label: t, type: 'radio' as const, name: 'title', value: t }));
    const alert = await this.alertCtrl.create({
      header: 'Gruppe wechseln',
      subHeader: `${this.selectedDgs.size} Durchgang/Durchgänge`,
      inputs: options,
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Wechseln',
          handler: async (data) => {
            const selectedTitle = data;
            if (!selectedTitle) return;
            this.loading = true;
            try {
              const request: GroupDurchgangRequest = { durchgangNames: [...this.selectedDgs], groupTitle: selectedTitle };
              await firstValueFrom(this.backend.moveDurchgangToGroup(this.uuid, this.secret, request));
              this.selectedDgs.clear();
              await this.loadData();
              const toast = await this.toastCtrl.create({ message: `Gruppe geändert zu "${selectedTitle}"`, duration: 2000, color: 'success' });
              await toast.present();
            } catch {
              const toast = await this.toastCtrl.create({ message: 'Fehler beim Ändern der Gruppe', duration: 3000, color: 'danger' });
              await toast.present();
            } finally {
              this.loading = false;
            }
          }
        }
      ]
    });
    await alert.present();
  }

  async mergeSelected() {
    if (this.selectedDgs.size < 2) return;
    const defaultName = [...this.selectedDgs][0];
    const alert = await this.alertCtrl.create({
      header: 'Durchgänge zusammenführen',
      subHeader: `${this.selectedDgs.size} Durchgänge werden umbenannt zu:`,
      inputs: [{ name: 'newName', type: 'text', value: defaultName, placeholder: 'Neuer Durchgang-Name' }],
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Zusammenführen',
          handler: async (data) => {
            const targetName = data.newName?.trim();
            if (!targetName) return;
            this.loading = true;
            try {
              const request: MergeDurchgangRequest = { durchgangNames: [...this.selectedDgs], targetName };
              await firstValueFrom(this.backend.mergeDurchgang(this.uuid, this.secret, request));
              this.selectedDgs.clear();
              await this.loadData();
              const toast = await this.toastCtrl.create({ message: `Zusammengeführt zu "${targetName}"`, duration: 2000, color: 'success' });
              await toast.present();
            } catch {
              const toast = await this.toastCtrl.create({ message: 'Fehler beim Zusammenführen', duration: 3000, color: 'danger' });
              await toast.present();
            } finally {
              this.loading = false;
            }
          }
        }
      ]
    });
    await alert.present();
  }

  get hasGroupedSelection(): boolean {
    const allRows = this.groups.reduce<TableRow[]>((acc, g) => acc.concat(g.rows), []);
    return [...this.selectedDgs].some(dgName => {
      const row = allRows.find(r => r.durchgangName === dgName);
      return row && row.durchgangName !== row.durchgangTitle;
    });
  }

  async ungroupSelected() {
    if (this.selectedDgs.size === 0) return;
    this.loading = true;
    try {
      const request: UngroupDurchgangRequest = { durchgangNames: [...this.selectedDgs] };
      await firstValueFrom(this.backend.ungroupDurchgang(this.uuid, this.secret, request));
      this.selectedDgs.clear();
      await this.loadData();
      const toast = await this.toastCtrl.create({ message: 'Gruppe aufgelöst', duration: 2000, color: 'success' });
      await toast.present();
    } catch {
      const toast = await this.toastCtrl.create({ message: 'Fehler beim Auflösen der Gruppe', duration: 3000, color: 'danger' });
      await toast.present();
    } finally {
      this.loading = false;
    }
  }

  async aggregateSelected() {
    if (this.selectedDgs.size < 2) return;
    const defaultTitle = this.suggestGroupTitle();
    const alert = await this.alertCtrl.create({
      header: 'Gruppe bilden',
      subHeader: `${this.selectedDgs.size} Durchgänge zusammenfassen`,
      inputs: [{ name: 'groupTitle', type: 'text', value: defaultTitle, placeholder: 'Gruppen-Name' }],
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Bilden',
          handler: async (data) => {
            const groupTitle = data.groupTitle?.trim();
            if (!groupTitle) return;
            this.loading = true;
            try {
              const request: GroupDurchgangRequest = { durchgangNames: [...this.selectedDgs], groupTitle };
              await firstValueFrom(this.backend.aggregateDurchgaenge(this.uuid, this.secret, request));
              this.selectedDgs.clear();
              await this.loadData();
              const toast = await this.toastCtrl.create({ message: `Gruppe "${groupTitle}" erstellt`, duration: 2000, color: 'success' });
              await toast.present();
            } catch {
              const toast = await this.toastCtrl.create({ message: 'Fehler beim Bilden der Gruppe', duration: 3000, color: 'danger' });
              await toast.present();
            } finally {
              this.loading = false;
            }
          }
        }
      ]
    });
    await alert.present();
  }

  private splitDurchgangName(name: string): string {
    const riege2Match = name.match(/^(.*)\((\.+)\)$/);
    if (riege2Match) return `${riege2Match[1].trim()} ${riege2Match[2]}`;
    const nameMatch = name.match(/^(.*)\((\d+)\)$/);
    if (nameMatch) return nameMatch[1].trim();
    return name.trim();
  }

  private categoriesOf(name: string): Set<string> {
    const baseName = this.splitDurchgangName(name);
    return baseName.replace(/-Tu/g, '').replace(/-Ti/g, '')
      .split('&').map(s => s.trim()).filter(s => s.length > 0)
      .reduce((set, s) => { set.add(s); return set; }, new Set<string>());
  }

  private suggestGroupTitle(): string {
    const allRows = this.groups.reduce<TableRow[]>((acc, g) => acc.concat(g.rows), []);
    const selectedRows = [...this.selectedDgs].map(dg => allRows.find(r => r.durchgangName === dg)).filter(Boolean) as TableRow[];

    const categories = selectedRows.reduce<Set<string>>((set, r) => {
      for (const c of this.categoriesOf(r.durchgangName)) set.add(c);
      return set;
    }, new Set<string>());

    const categoryLabel = [...categories].sort().map(s => s.replace(/[()]/g, '')).join(', ');

    const abteilungPattern = /^Abteilung (\d+)/;
    const usedNumbers = this.groups
      .map(g => g.title.match(abteilungPattern))
      .filter(Boolean)
      .map(m => parseInt(m![1], 10));
    const nextNum = usedNumbers.length > 0 ? Math.max(...usedNumbers) + 1 : 1;

    if (categoryLabel) return `Abteilung ${nextNum} (${categoryLabel})`;
    return `Abteilung ${nextNum}`;
  }

  async regenerateSelected() {
    if (this.selectedDgs.size === 0) return;
    this.filterDurchgaenge = [...this.selectedDgs];
    this.selectedDisziplinIds = new Set(
      this.disziplinen.filter(d => {
        for (const dgName of this.selectedDgs) {
          const row = this.groups.reduce<TableRow[]>((acc, g) => acc.concat(g.rows), []).find(r => r.durchgangName === dgName);
          if (row && row.cells[d.id]?.length > 0) return true;
        }
        return false;
      }).map(d => d.id)
    );
    this.showGeneratePanel = true;
    this.cdr.detectChanges();
  }

  toggleDisziplin(id: number, event: any) {
    if (event.detail.checked) {
      this.selectedDisziplinIds.add(id);
    } else {
      this.selectedDisziplinIds.delete(id);
    }
  }

  selectAllDisziplinen() {
    this.selectedDisziplinIds = new Set(this.disziplinen.map(d => d.id));
  }

  selectNoneDisziplinen() {
    this.selectedDisziplinIds = new Set();
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
