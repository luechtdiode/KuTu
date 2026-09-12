import { Component, inject, signal, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { WsStateService } from '../services/ws-state.service';
import { ClubRegistration, AthletRegistration, ProgrammRaw, SyncAction, JudgeRegistration, RegistrationSyncUpdated, TeamItem } from '../backend-types';
import { firstValueFrom, Subscription } from 'rxjs';

@Component({
  templateUrl: 'admin-club-detail.page.html',
  styleUrls: ['admin-club-detail.page.scss'],
  standalone: false
})
export class AdminClubDetailPage implements OnDestroy {
  uuid = '';
  secret = '';
  regId = 0;

  registration = signal<ClubRegistration | null>(null);
  athletRegistrations = signal<AthletRegistration[]>([]);
  programs = signal<ProgrammRaw[]>([]);
  syncActions = signal<SyncAction[]>([]);
  judgeRegistrations = signal<JudgeRegistration[]>([]);
  teams = signal<TeamItem[]>([]);
  loading = signal(false);

  private route = inject(ActivatedRoute);
  private secretService = inject(SecretService);
  private backend = inject(AdminBackendService);
  private wsState = inject(WsStateService);
  private wsAcquired = false;
  private wsSubscriptions: Subscription[] = [];

  async ionViewWillEnter() {
    this.uuid = this.route.snapshot.paramMap.get('uuid') || '';
    this.regId = Number(this.route.snapshot.paramMap.get('regId') || '0');
    const stored = this.secretService.getSecret(this.uuid);
    if (stored) {
      this.secret = stored.secret;
    }
    await this.loadData();
    this.initWebSocket();
  }

  ngOnDestroy() {
    this.wsSubscriptions.forEach(s => s.unsubscribe());
    if (this.wsAcquired) {
      this.wsState.release({kind: 'competition', competitionId: this.uuid});
      this.wsAcquired = false;
    }
  }

  private initWebSocket() {
    if (this.wsAcquired) return;
    this.wsAcquired = true;
    this.wsSubscriptions.push(
      this.wsState.registrationSyncUpdated.subscribe(() => {
        this.loadData();
      })
    );
    this.wsState.acquire({kind: 'competition', competitionId: this.uuid});
  }

  async loadData() {
    this.loading.set(true);
    try {
      const [registration, athletes, programs, syncActions, judges, teams] = await Promise.all([
        firstValueFrom(this.backend.getRegistration(this.uuid, this.regId, this.secret)),
        firstValueFrom(this.backend.getAthletRegistrations(this.uuid, this.regId, this.secret)),
        firstValueFrom(this.backend.getProgramList(this.uuid, this.secret)),
        firstValueFrom(this.backend.getSyncActions(this.uuid, this.secret)).catch(() => [] as SyncAction[]),
        firstValueFrom(this.backend.getJudgeRegistrations(this.uuid, this.regId, this.secret)).catch(() => [] as JudgeRegistration[]),
        firstValueFrom(this.backend.getTeams(this.uuid, this.regId, this.secret)).catch(() => [] as TeamItem[])
      ]);
      this.registration.set(registration);
      this.athletRegistrations.set(athletes);
      this.programs.set(programs);
      this.syncActions.set(syncActions);
      this.judgeRegistrations.set(judges);
      this.teams.set(teams);
    } catch {
      this.registration.set(null);
      this.athletRegistrations.set([]);
      this.programs.set([]);
      this.syncActions.set([]);
      this.judgeRegistrations.set([]);
      this.teams.set([]);
    } finally {
      this.loading.set(false);
    }
  }

  getAthleteSyncStatusSimple(ath: AthletRegistration): string {
    if (this.syncActions().length > 0) {
      const action = this.syncActions().find(a => a.data.registrationId === ath.vereinregistrationId && a.caption.indexOf(ath.name) > -1 && a.caption.indexOf(ath.vorname) > -1);
      if (action) {
        return 'pending';
      } else {
        return 'in sync';
      }
    } else {
      return '';
    }
  }

  getAthleteSyncStatusDetail(ath: AthletRegistration): string {
    if (this.syncActions().length > 0) {
      const action = this.syncActions().find(a => a.data.registrationId === ath.vereinregistrationId && a.caption.indexOf(ath.name) > -1 && a.caption.indexOf(ath.vorname) > -1);
      if (action) {
        return action.caption.substring(0, (action.caption + ':').indexOf(':'));
      } else {
        return '';
      }
    } else {
      return '';
    }
  }

  get groupedPrograms() {
    const byProgram = new Map<number, { id: number; name: string; athletes: AthletRegistration[] }>();
    for (const ath of this.athletRegistrations()) {
      if (!byProgram.has(ath.programId)) {
        const pgm = this.programs().find(p => p.id === ath.programId);
        byProgram.set(ath.programId, {
          id: ath.programId,
          name: pgm?.name || 'Programm ' + ath.programId,
          athletes: []
        });
      }
      byProgram.get(ath.programId)!.athletes.push(ath);
    }
    return Array.from(byProgram.values());
  }

  getTeamName(index: number): string {
    const team = this.teams().find(t => t.index === index);
    return team ? team.name : 'Team ' + index;
  }

  get isApproved(): boolean {
    const reg = this.registration();
    return reg?.vereinId != null && reg.vereinId > 0;
  }
}
