import { Component, inject, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { AdminWebsocketService } from '../services/admin-websocket.service';
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

  registration: ClubRegistration | null = null;
  athletRegistrations: AthletRegistration[] = [];
  programs: ProgrammRaw[] = [];
  syncActions: SyncAction[] = [];
  judgeRegistrations: JudgeRegistration[] = [];
  teams: TeamItem[] = [];
  loading = false;

  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private secretService = inject(SecretService);
  private backend = inject(AdminBackendService);
  private ws: AdminWebsocketService | null = null;
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
    this.ws?.disconnectWS?.();
    this.ws = null;
  }

  private initWebSocket() {
    if (this.ws) return;
    this.ws = new AdminWebsocketService(this.uuid, this.secret);
    this.wsSubscriptions.push(
      this.ws.registrationSyncUpdated.subscribe(() => {
        this.loadData();
      })
    );
    this.ws.initWebsocket();
  }

  async loadData() {
    this.loading = true;
    try {
      const [registration, athletes, programs, syncActions, judges, teams] = await Promise.all([
        firstValueFrom(this.backend.getRegistration(this.uuid, this.regId, this.secret)),
        firstValueFrom(this.backend.getAthletRegistrations(this.uuid, this.regId, this.secret)),
        firstValueFrom(this.backend.getProgramList(this.uuid, this.secret)),
        firstValueFrom(this.backend.getSyncActions(this.uuid, this.secret)).catch(() => [] as SyncAction[]),
        firstValueFrom(this.backend.getJudgeRegistrations(this.uuid, this.regId, this.secret)).catch(() => [] as JudgeRegistration[]),
        firstValueFrom(this.backend.getTeams(this.uuid, this.regId, this.secret)).catch(() => [] as TeamItem[])
      ]);
      this.registration = registration;
      this.athletRegistrations = athletes;
      this.programs = programs;
      this.syncActions = syncActions;
      this.judgeRegistrations = judges;
      this.teams = teams;
    } catch {
      this.registration = null;
      this.athletRegistrations = [];
      this.programs = [];
      this.syncActions = [];
      this.judgeRegistrations = [];
      this.teams = [];
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  getAthleteSyncStatusSimple(ath: AthletRegistration): string {
    if (this.syncActions.length > 0) {
      const action = this.syncActions.find(a => a.data.registrationId === ath.vereinregistrationId && a.caption.indexOf(ath.name) > -1 && a.caption.indexOf(ath.vorname) > -1);
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
    if (this.syncActions.length > 0) {
      const action = this.syncActions.find(a => a.data.registrationId === ath.vereinregistrationId && a.caption.indexOf(ath.name) > -1 && a.caption.indexOf(ath.vorname) > -1);
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
    for (const ath of this.athletRegistrations) {
      if (!byProgram.has(ath.programId)) {
        const pgm = this.programs.find(p => p.id === ath.programId);
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
    const team = this.teams.find(t => t.index === index);
    return team ? team.name : 'Team ' + index;
  }

  get isApproved(): boolean {
    return this.registration?.vereinId != null && this.registration.vereinId > 0;
  }
}
