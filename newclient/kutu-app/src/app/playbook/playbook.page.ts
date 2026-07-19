import { Component, inject, ChangeDetectorRef, OnDestroy, OnInit } from '@angular/core';
import { ToastController } from '@ionic/angular';
import { ActivatedRoute } from '@angular/router';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { AdminWebsocketService, DurchgangResetted } from '../services/admin-websocket.service';
import { PlaybookState, PlaybookDurchgang, PlaybookStation, Geraet } from '../backend-types';
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

  private ws: AdminWebsocketService | null = null;
  private subscriptions: Subscription[] = [];

  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private secretService = inject(SecretService);
  private backend = inject(AdminBackendService);
  private toastCtrl = inject(ToastController);

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

  goToResults(durchgang: string, geraetId: number) {
    // TODO: Navigate to results page for this geräteriege
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
