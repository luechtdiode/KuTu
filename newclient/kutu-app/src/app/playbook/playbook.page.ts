import { Component, inject, ChangeDetectorRef, OnDestroy, OnInit } from '@angular/core';
import { ToastController, NavController } from '@ionic/angular';
import { ActivatedRoute } from '@angular/router';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { BackendService } from '../services/backend.service';
import { AdminWebsocketService, DurchgangResetted } from '../services/admin-websocket.service';
import { PlaybookState, PlaybookDurchgang, PlaybookStation, PlaybookStep, Geraet } from '../backend-types';
import { Subscription } from 'rxjs';

interface PlaybookGroup {
  title: string;
  rows: PlaybookDurchgang[];
}

@Component({
  templateUrl: 'playbook.page.html',
  styleUrls: ['playbook.page.scss'],
  standalone: false
})
export class PlaybookPage implements OnInit, OnDestroy {
  uuid = '';
  secret = '';
  wettkampfTitle = '';
  wettkampfDatum = '';
  logoUrl = '';

  playbook: PlaybookState | null = null;
  groups: PlaybookGroup[] = [];
  disziplinen: { id: number; name: string }[] = [];
  loading = false;
  expandedGroups = new Set<string>();

  private ws: AdminWebsocketService | null = null;
  private subscriptions: Subscription[] = [];

  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private secretService = inject(SecretService);
  private backend = inject(AdminBackendService);
  private bs = inject(BackendService);
  private toastCtrl = inject(ToastController);
  private navCtrl = inject(NavController);

  ngOnInit() {
    this.uuid = this.route.snapshot.paramMap.get('uuid') || '';
    const stored = this.secretService.getSecret(this.uuid);
    if (stored) {
      this.secret = stored.secret;
      this.wettkampfTitle = stored.titel;
      this.wettkampfDatum = stored.datum.substring(0, 10);
    }
    this.backend.getCompetitionDetails(this.uuid, this.secret).subscribe(details => {
      this.wettkampfTitle = details.titel;
      this.wettkampfDatum = details.datum.substring(0, 10);
    });
    this.backend.getCompetitionLogo(this.uuid, this.secret).subscribe({
      next: blob => {
        this.logoUrl = URL.createObjectURL(blob);
        this.cdr.detectChanges();
      },
      error: () => {}
    });
    this.loadPlaybook();
    this.initWebSocket();
  }

  ngOnDestroy() {
    this.ws?.disconnectWS(true);
    this.subscriptions.forEach(s => s.unsubscribe());
    if (this.logoUrl) URL.revokeObjectURL(this.logoUrl);
  }

  loadPlaybook() {
    this.loading = true;
    this.backend.getPlaybook(this.uuid, this.secret).subscribe({
      next: state => {
        this.playbook = state;
        this.buildTable(state);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private buildTable(state: PlaybookState) {
    const disziplinMap = new Map<number, string>();
    for (const dg of state.durchgaenge) {
      for (const station of dg.stations) {
        if (station.disziplinId > 0) {
          disziplinMap.set(station.disziplinId, station.disziplinName);
        }
      }
    }
    this.disziplinen = Array.from(disziplinMap.entries())
      .map(([id, name]) => ({ id, name }));

    const prevExpanded = new Set(this.expandedGroups);
    const groupMap = new Map<string, PlaybookDurchgang[]>();
    for (const dg of state.durchgaenge) {
      const title = dg.title || dg.name;
      if (!groupMap.has(title)) {
        groupMap.set(title, []);
      }
      groupMap.get(title)!.push(dg);
    }
    this.groups = Array.from(groupMap.entries())
      .map(([title, rows]) => ({ title, rows: rows.sort((a, b) => a.name.localeCompare(b.name)) }))
      .sort((a, b) => a.title.localeCompare(b.title));

    if (prevExpanded.size === 0) {
      // default: all collapsed
    } else {
      this.expandedGroups.clear();
      for (const g of this.groups) {
        if (prevExpanded.has(g.title)) {
          this.expandedGroups.add(g.title);
        }
      }
    }
  }

  toggleGroup(group: PlaybookGroup) {
    if (this.expandedGroups.has(group.title)) {
      this.expandedGroups.delete(group.title);
    } else {
      this.expandedGroups.add(group.title);
    }
  }

  getStation(dg: PlaybookDurchgang, disziplinId: number): PlaybookStation | undefined {
    return dg.stations.find(s => s.disziplinId === disziplinId);
  }

  stepPct(step: { completedAthletes: number; totalAthletes: number }): number {
    if (step.totalAthletes === 0) return 0;
    return Math.round(100 * step.completedAthletes / step.totalAthletes);
  }

  hasGroupedDgs(group: PlaybookGroup): boolean {
    return group.rows.length > 1 || group.rows[0].name !== group.title;
  }

  groupPlanStart(group: PlaybookGroup): string {
    return group.rows.find(r => r.planStart)?.planStart || '';
  }

  groupPlanFinish(group: PlaybookGroup): string {
    let last = '';
    for (const r of group.rows) {
      if (r.planFinish) last = r.planFinish;
    }
    return last;
  }

  groupDuration(group: PlaybookGroup): string {
    const starts = group.rows.filter(r => r.effectiveStart).map(r => r.effectiveStart);
    const ends = group.rows.filter(r => r.effectiveEnd).map(r => r.effectiveEnd);
    if (starts.length && ends.length) return `ab ${starts[0]} bis ${ends[ends.length - 1]}`;
    return '';
  }

  groupTotalAthletes(group: PlaybookGroup): number {
    return group.rows.reduce((sum, r) => sum + r.totalCount, 0);
  }

  groupCompletedAthletes(group: PlaybookGroup): number {
    return group.rows.reduce((sum, r) => sum + r.completedCount, 0);
  }

  groupOverallPct(group: PlaybookGroup): number {
    const total = this.groupTotalAthletes(group);
    if (total === 0) return 0;
    return Math.round(100 * this.groupCompletedAthletes(group) / total);
  }

  getGroupStation(group: PlaybookGroup, disziplinId: number): PlaybookStation | undefined {
    const stepMap = new Map<number, { totalAthletes: number; completedAthletes: number }>();
    let hasData = false;
    for (const row of group.rows) {
      const station = this.getStation(row, disziplinId);
      if (!station) continue;
      hasData = true;
      for (const step of station.steps) {
        const existing = stepMap.get(step.halt) || { totalAthletes: 0, completedAthletes: 0 };
        stepMap.set(step.halt, {
          totalAthletes: existing.totalAthletes + step.totalAthletes,
          completedAthletes: existing.completedAthletes + step.completedAthletes
        });
      }
    }
    if (!hasData) return undefined;
    const steps: PlaybookStep[] = Array.from(stepMap.entries())
      .sort((a, b) => a[0] - b[0])
      .map(([halt, stats]) => ({ halt, ...stats }));
    const totalAthletes = steps.reduce((s, st) => s + st.totalAthletes, 0);
    const completedAthletes = steps.reduce((s, st) => s + st.completedAthletes, 0);
    const overallPct = totalAthletes > 0 ? Math.round(100 * completedAthletes / totalAthletes) : 0;
    return { disziplinId, disziplinName: '', steps, overallPct };
  }

  isGroupRunning(group: PlaybookGroup): boolean {
    return group.rows.some(r => r.isRunning);
  }

  isGroupFinished(group: PlaybookGroup): boolean {
    return group.rows.every(r => r.isFinished);
  }

  canStart(dg: PlaybookDurchgang): boolean {
    return !dg.isRunning && !dg.isFinished;
  }

  canFinish(dg: PlaybookDurchgang): boolean {
    return dg.isRunning;
  }

  canReset(dg: PlaybookDurchgang): boolean {
    return dg.isRunning || dg.isFinished;
  }

  canRestart(dg: PlaybookDurchgang): boolean {
    return dg.isFinished;
  }

  canStartGroup(group: PlaybookGroup): boolean {
    return group.rows.some(r => this.canStart(r) || this.canRestart(r));
  }

  canFinishGroup(group: PlaybookGroup): boolean {
    return group.rows.some(r => this.canFinish(r));
  }

  canResetGroup(group: PlaybookGroup): boolean {
    return group.rows.some(r => this.canReset(r));
  }

  startDurchgang(name: string) {
    this.backend.startDurchgang(this.uuid, this.secret, name).subscribe({
      next: () => this.showToast('Durchgang gestartet', 'success'),
      error: () => this.showToast('Fehler beim Starten', 'danger')
    });
  }

  finishDurchgang(name: string) {
    this.backend.finishDurchgang(this.uuid, this.secret, name).subscribe({
      next: () => this.showToast('Durchgang abgeschlossen', 'success'),
      error: () => this.showToast('Fehler beim Abschliessen', 'danger')
    });
  }

  resetDurchgang(name: string) {
    this.backend.resetDurchgang(this.uuid, this.secret, name).subscribe({
      next: () => this.showToast('Durchgang zurückgesetzt', 'success'),
      error: () => this.showToast('Fehler beim Zurücksetzen', 'danger')
    });
  }

  startGroup(group: PlaybookGroup) {
    const startable = group.rows.filter(r => this.canStart(r) || this.canRestart(r));
    for (const r of startable) {
      this.startDurchgang(r.name);
    }
  }

  finishGroup(group: PlaybookGroup) {
    const finishable = group.rows.filter(r => this.canFinish(r));
    for (const r of finishable) {
      this.finishDurchgang(r.name);
    }
  }

  resetGroup(group: PlaybookGroup) {
    const resettable = group.rows.filter(r => this.canReset(r));
    for (const r of resettable) {
      this.resetDurchgang(r.name);
    }
  }

  goToStation(durchgang: string, geraetId: number, halt: number) {
    this.bs.getGeraete(this.uuid, durchgang).subscribe(() => {
      this.bs.getSteps(this.uuid, durchgang, geraetId).subscribe(() => {
        this.bs.getWertungen(this.uuid, durchgang, geraetId, halt+1);
        this.navCtrl.navigateForward('station');
      });
    });
  }

  formatDate(d: string): string {
    if (!d) return '';
    return d.split('-').reverse().join('.');
  }

  private initWebSocket() {
    this.ws = new AdminWebsocketService(this.uuid, this.secret);

    this.subscriptions.push(
      this.ws.durchgangStarted.subscribe(() => {
        this.loadPlaybook();
      })
    );

    this.subscriptions.push(
      this.ws.durchgangFinished.subscribe(() => {
        this.loadPlaybook();
      })
    );

    this.subscriptions.push(
      this.ws.durchgangResetted.subscribe(() => {
        this.loadPlaybook();
      })
    );

    this.subscriptions.push(
      this.ws.wertungUpdated.subscribe(() => {
        this.loadPlaybook();
      })
    );

    this.ws.initWebsocket();
  }

  private async showToast(message: string, color: string) {
    const toast = await this.toastCtrl.create({
      message,
      duration: 2000,
      color
    });
    await toast.present();
  }
}
