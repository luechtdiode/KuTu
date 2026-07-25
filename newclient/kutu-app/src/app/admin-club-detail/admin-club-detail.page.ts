import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { ClubRegistration, AthletRegistration, ProgrammRaw } from '../backend-types';
import { firstValueFrom } from 'rxjs';

@Component({
  templateUrl: 'admin-club-detail.page.html',
  styleUrls: ['admin-club-detail.page.scss'],
  standalone: false
})
export class AdminClubDetailPage {
  uuid = '';
  secret = '';
  regId = 0;

  registration: ClubRegistration | null = null;
  athletRegistrations: AthletRegistration[] = [];
  programs: ProgrammRaw[] = [];
  loading = false;

  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private secretService = inject(SecretService);
  private backend = inject(AdminBackendService);

  async ionViewWillEnter() {
    this.uuid = this.route.snapshot.paramMap.get('uuid') || '';
    this.regId = Number(this.route.snapshot.paramMap.get('regId') || '0');
    const stored = this.secretService.getSecret(this.uuid);
    if (stored) {
      this.secret = stored.secret;
    }
    await this.loadData();
  }

  async loadData() {
    this.loading = true;
    try {
      const [registration, athletes, programs] = await Promise.all([
        firstValueFrom(this.backend.getRegistration(this.uuid, this.regId, this.secret)),
        firstValueFrom(this.backend.getAthletRegistrations(this.uuid, this.regId, this.secret)),
        firstValueFrom(this.backend.getProgramList(this.uuid, this.secret))
      ]);
      this.registration = registration;
      this.athletRegistrations = athletes;
      this.programs = programs;
    } catch {
      this.registration = null;
      this.athletRegistrations = [];
      this.programs = [];
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
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

  get isApproved(): boolean {
    return this.registration?.vereinId != null && this.registration.vereinId > 0;
  }
}
