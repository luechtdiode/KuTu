import { Component, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { NavController } from '@ionic/angular';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { formatDisplayDate } from '../utils';
import { firstValueFrom, forkJoin } from 'rxjs';

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
  competitions: CompetitionListItem[] = [];
  private cdr = inject(ChangeDetectorRef);
  private secretService = inject(SecretService);
  private backend = inject(AdminBackendService);
  private nav = inject(NavController);

  ngOnDestroy() {
    for (const c of this.competitions) {
      if (c.logoUrl) URL.revokeObjectURL(c.logoUrl);
    }
  }

  ionViewWillEnter() {
    const secrets = this.secretService.getSecrets();
    this.competitions = secrets.map(s => ({
      uuid: s.uuid,
      titel: s.titel,
      datum: s.datum,
      secret: s.secret,
      loading: true,
      error: false
    }));
    this.refreshFromServer();
  }

  formatDate(d: string): string {
    return formatDisplayDate(d);
  }

  openOverview(c: CompetitionListItem) {
    this.nav.navigateRoot('/admin/competitions/' + c.uuid);
  }

  private refreshFromServer() {
    const requests = this.competitions.map(c =>
      this.backend.getCompetitionDetails(c.uuid, c.secret)
    );
    if (requests.length === 0) {
      this.cdr.detectChanges();
      return;
    }
    forkJoin(requests.map(r => firstValueFrom(r).catch(() => null))).subscribe(results => {
      for (let i = 0; i < results.length; i++) {
        const data = results[i];
        if (data) {
          this.competitions[i].titel = data.titel;
          this.competitions[i].datum = data.datum;
          this.competitions[i].error = false;
        } else {
          this.competitions[i].error = true;
        }
        this.competitions[i].loading = false;
        this.loadLogo(i);
      }
      this.cdr.detectChanges();
    });
  }

  private loadLogo(index: number) {
    const c = this.competitions[index];
    this.backend.getCompetitionLogo(c.uuid, c.secret).subscribe({
      next: blob => {
        c.logoUrl = URL.createObjectURL(blob);
        this.cdr.detectChanges();
      },
      error: () => { /* no logo */ }
    });
  }
}
