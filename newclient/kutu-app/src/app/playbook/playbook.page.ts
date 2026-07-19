import { Component, inject, ChangeDetectorRef, OnDestroy, OnInit } from '@angular/core';
import { ToastController, NavController, ModalController, AlertController } from '@ionic/angular';
import { ActivatedRoute } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { BackendService } from '../services/backend.service';
import { AdminWebsocketService, DurchgangResetted } from '../services/admin-websocket.service';
import { PlaybookState, PlaybookDurchgang, PlaybookStation, PlaybookStep, Geraet, JudgeLink } from '../backend-types';
import { JudgeLinkModalComponent } from './judge-link-modal.component';
import { Subscription } from 'rxjs';
import { backendUrl } from '../utils';

interface PlaybookGroup {
  title: string;
  rows: PlaybookDurchgang[];
}

interface StepperHalt {
  halt: number;
  totalAthletes: number;
  completedAthletes: number;
  pct: number;
  status: 'pending' | 'einturnen' | 'active' | 'done';
}

interface StepperDg {
  name: string;
  displayName: string;
  status: 'pending' | 'running' | 'finished';
  halts: StepperHalt[];
  planEinturnen: string;
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
  stepperDgs: StepperDg[] = [];
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
  private modalCtrl = inject(ModalController);
  private alertCtrl = inject(AlertController);
  private http = inject(HttpClient);

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

  ionViewWillEnter() {
    if (this.playbook) {
      this.loadPlaybook();
    }
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

    this.buildStepper();
  }

  private buildStepper() {
    const allFinished = this.groups.length > 0 && this.groups.every(g => g.rows.every(r => r.isFinished));
    const visibleGroups = allFinished ? this.groups : this.groups.filter(g => g.rows.some(r => !r.isFinished));

    this.stepperDgs = visibleGroups.map(g => {
      const rows = g.rows;
      const ungrouped = rows.length === 1 && rows[0].name === rows[0].title;
      const displayName = ungrouped ? rows[0].name : g.title;

      let status: StepperDg['status'] = 'pending';
      if (rows.every(r => r.isFinished)) status = 'finished';
      else if (rows.some(r => r.isRunning)) status = 'running';

      const haltMap = new Map<number, { totalAthletes: number; completedAthletes: number }>();
      for (const row of rows) {
        for (const station of row.stations) {
          for (const step of station.steps) {
            const existing = haltMap.get(step.halt) || { totalAthletes: 0, completedAthletes: 0 };
            haltMap.set(step.halt, {
              totalAthletes: existing.totalAthletes + step.totalAthletes,
              completedAthletes: existing.completedAthletes + step.completedAthletes
            });
          }
        }
      }

      const haltEntries = Array.from(haltMap.entries()).sort((a, b) => a[0] - b[0]);
      const halts: StepperHalt[] = [];
      const haltsFinished = new Set<number>();

      for (const [halt, stats] of haltEntries) {
        const pct = stats.totalAthletes > 0 ? Math.round(100 * stats.completedAthletes / stats.totalAthletes) : 0;
        let hStatus: StepperHalt['status'] = 'pending';
        if (status === 'running') {
          if (pct === 100) {
            hStatus = 'done';
            haltsFinished.add(halt);
          } else if (pct > 0) {
            hStatus = 'active';
          } else {
            const prevDone = halt === 0 || haltsFinished.has(halt - 1);
            hStatus = prevDone ? 'einturnen' : 'pending';
          }
        }
        halts.push({ halt, totalAthletes: stats.totalAthletes, completedAthletes: stats.completedAthletes, pct, status: hStatus });
      }

      return { name: g.title, displayName, status, halts, planEinturnen: rows[0]?.planEinturnen || '', rows };
    });
  }

  stepperDgClick(dg: StepperDg) {
    if (dg.status === 'pending') {
      for (const row of dg.rows.filter(r => !r.isRunning && !r.isFinished)) {
        this.startDurchgang(row.name);
      }
    }
  }

  isCurrentDg(dg: StepperDg): boolean {
    const running = this.stepperDgs.find(d => d.status === 'running');
    if (running) return dg === running;
    return dg === this.stepperDgs.find(d => d.status === 'pending');
  }

  haltStatusLabel(halt: StepperHalt, isFirstHalt: boolean): string {
    switch (halt.status) {
      case 'einturnen': return isFirstHalt ? 'Einturnen' : 'Gerätewechsel + Einturnen';
      case 'active': return halt.pct + '%';
      case 'done': return '\u2713';
      default: return '';
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

  async resetDurchgang(name: string) {
    const alert = await this.alertCtrl.create({
      header: 'Durchgang Zeiten zurücksetzen',
      message: `Die bereits verbrauchte Zeit wird zurückgesetzt und kann nicht wieder für die Durchgangsdauer beigezogen werden. Sollen die aufgezeichneten Durchgangszeiten von "${name}" wirklich zurückgesetzt werden?`,
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Zurücksetzen', role: 'destructive',
          handler: () => {
            this.backend.resetDurchgang(this.uuid, this.secret, name).subscribe({
              next: () => this.showToast('Durchgangszeiten zurückgesetzt', 'success'),
              error: () => this.showToast('Fehler beim Zurücksetzen', 'danger')
            });
          }
        }
      ]
    });
    await alert.present();
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

  async resetGroup(group: PlaybookGroup) {
    const resettable = group.rows.filter(r => this.canReset(r));
    const names = resettable.map(r => r.name).join(', ');
    const alert = await this.alertCtrl.create({
      header: 'Alle aufgezeichneten Durchgangszeiten zurücksetzen',
      message: `Die bereits verbrauchte Zeit wird bei allen selektierten Durchgängen zurückgesetzt und kann nicht wieder für die Durchgangsdauer beigezogen werden. Sollen die aufgezeichneten Zeiten der selektierten Durchgänge "${names}" wirklich zurückgesetzt werden?`,
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Zurücksetzen', role: 'destructive',
          handler: () => {
            for (const r of resettable) {
              this.backend.resetDurchgang(this.uuid, this.secret, r.name).subscribe({
                next: () => this.showToast('Durchgangszeiten zurückgesetzt', 'success'),
                error: () => this.showToast('Fehler beim Zurücksetzen der Durchgangszeiten', 'danger')
              });
            }
          }
        }
      ]
    });
    await alert.present();
  }

  goToStation(durchgang: string, geraetId: number, halt: number) {
    const refreshAndNavigate = () => {
      this.bs.getGeraete(this.uuid, durchgang).subscribe(() => {
        this.bs.getSteps(this.uuid, durchgang, geraetId).subscribe(() => {
          this.bs.getWertungen(this.uuid, durchgang, geraetId, halt + 1);
          this.navCtrl.navigateForward('station');
        });
      });
    };

    const headers = new HttpHeaders({ 'x-access-token': this.secret });
    this.http.options(backendUrl + 'api/isTokenExpired', {
      observe: 'response',
      responseType: 'text',
      headers
    }).subscribe({
      next: (data) => {
        const newToken = data.headers.get('x-access-token');
        if (newToken) {
          localStorage.setItem('auth_token', newToken);
        }
        refreshAndNavigate();
      },
      error: () => refreshAndNavigate()
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

  async showJudgeLink() {
    this.backend.getJudgeLink(this.uuid, this.secret).subscribe({
      next: async (jl: JudgeLink) => {
        const modal = await this.modalCtrl.create({
          component: JudgeLinkModalComponent,
          componentProps: { link: jl.link, qrUrl: jl.qrImage }
        });
        await modal.present();
      },
      error: () => this.showToast('Fehler beim Generieren des Links', 'danger')
    });
  }

  goToAthletSearch() {
    this.navCtrl.navigateForward(`/search-athlet/${this.uuid}?admin=true&origin=playbook`);
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
