import { Component, inject, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { AlertController, ToastController } from '@ionic/angular';
import { ActivatedRoute } from '@angular/router';
import { SecretService } from '../services/secret.service';
import { AdminBackendService } from '../services/admin-backend.service';
import { BackendService } from '../services/backend.service';
import { PublishedScoreView, AdminScoreRequest, ScoreBlock } from '../backend-types';
import { firstValueFrom, Subscription } from 'rxjs';
import { backendUrl } from '../utils';

@Component({
  templateUrl: 'admin-rankings.page.html',
  styleUrls: ['admin-rankings.page.scss'],
  standalone: false
})
export class AdminRankingsPage implements OnDestroy {
  uuid = '';
  secret = '';
  wettkampfTitle = '';

  availableGroupers: string[] = [];
  selectedGroupers: (string | null)[] = [null, null, null, null];
  filterOptions: Map<string, string[]> = new Map();
  selectedFilters: Map<string, Set<string>> = new Map();
  filterOnly: boolean[] = [false, false, false, false];

  kind = 'Einzelrangliste';
  bestN = 'alle';
  isAlphanumeric = false;
  isAvg = true;

  scoreblocks: ScoreBlock[] = [];
  savedScores: PublishedScoreView[] = [];
  editingScore: PublishedScoreView | null = null;
  editTitle = '';

  creatingNew = false;
  loading = false;
  previewLoading = false;
  loadingGroupers = false;

  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private secretService = inject(SecretService);
  private backend = inject(AdminBackendService);
  private backendService = inject(BackendService);
  private alertCtrl = inject(AlertController);
  private toastCtrl = inject(ToastController);

  private subscriptions: Subscription[] = [];

  ngOnDestroy() {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  async ionViewWillEnter() {
    this.uuid = this.route.snapshot.paramMap.get('uuid') || '';
    const stored = this.secretService.getSecret(this.uuid);
    if (stored) {
      this.secret = stored.secret;
      this.wettkampfTitle = stored.titel;
    }
    await this.loadGroupers();
    await this.loadSavedScores();
  }

  async loadGroupers() {
    this.loadingGroupers = true;
    try {
      this.availableGroupers = await firstValueFrom(this.backend.getAvailableGroupers(this.uuid));
    } catch {
      this.availableGroupers = [];
    } finally {
      this.loadingGroupers = false;
      this.cdr.detectChanges();
    }
  }

  async loadFilterOptions(level: number) {
    const grouper = this.selectedGroupers[level];
    if (!grouper) {
      this.filterOptions.delete('' + level);
      this.selectedFilters.delete('' + level);
      return;
    }
    try {
      const groupby = this.selectedGroupers.filter(g => g !== null).join(':');
      const filters = await firstValueFrom(this.backend.getAvailableFilters(this.uuid, groupby));
      const prefix = grouper + ':';
      const matching = filters.find(f => decodeURIComponent(f.split(':')[0]) === grouper);
      const parsedFilters = matching
        ? matching.substring(prefix.length).split('!').map(v => decodeURIComponent(v)).sort()
        : [];
      this.filterOptions.set('' + level, parsedFilters);
      if (!this.selectedFilters.has('' + level)) {
        this.selectedFilters.set('' + level, new Set(parsedFilters));
      } else {
        const existing = this.selectedFilters.get('' + level);
        const newSet = new Set<string>();
        parsedFilters.forEach(f => {
          if (existing.has(f)) {
            newSet.add(f);
          }
        });
        this.selectedFilters.set('' + level, newSet);
      }
    } catch {
      this.filterOptions.delete('' + level);
      this.selectedFilters.delete('' + level);
    }
    this.cdr.detectChanges();
  }

  onGrouperChange(level: number) {
    this.filterOnly[level] = false;
    for (let i = level + 1; i < 4; i++) {
      this.selectedGroupers[i] = null;
      this.filterOptions.delete('' + i);
      this.selectedFilters.delete('' + i);
      this.filterOnly[i] = false;
    }
    this.loadFilterOptions(level);
  }

  onModeChange(level: number, event: any) {
    this.filterOnly[level] = event.detail.value === 'filter';
  }

  getFilterOptions(level: number): string[] {
    return this.filterOptions.get('' + level) || [];
  }

  getSelectedFilters(level: number): Set<string> {
    return this.selectedFilters.get('' + level) || new Set();
  }

  toggleFilter(level: number, value: string) {
    const filters = this.getSelectedFilters(level);
    if (filters.has(value)) {
      filters.delete(value);
    } else {
      filters.add(value);
    }
  }

  isFilterSelected(level: number, value: string): boolean {
    return this.getSelectedFilters(level).has(value);
  }

  selectAllFilters(level: number) {
    const options = this.getFilterOptions(level);
    this.selectedFilters.set('' + level, new Set(options));
  }

  deselectAllFilters(level: number) {
    this.selectedFilters.set('' + level, new Set());
  }

  buildQuery(): string {
    const groupby = this.selectedGroupers.filter(g => g !== null).join(':');
    if (!groupby) return '';

    let query = 'groupby=' + encodeURIComponent(groupby);

    for (let i = 0; i < 4; i++) {
      const grouper = this.selectedGroupers[i];
      if (!grouper) continue;
      const options = this.getFilterOptions(i);
      const selected = this.getSelectedFilters(i);
      if (this.filterOnly[i] && options.length > 1) {
        const filterValues = Array.from(selected).map(v => encodeURIComponent(v)).concat(['all']).join('!');
        query += '&filter=' + encodeURIComponent(grouper) + ':' + filterValues;
      } else if (selected.size > 0 && selected.size < options.length) {
        const filterValues = Array.from(selected).map(v => encodeURIComponent(v)).join('!');
        query += '&filter=' + encodeURIComponent(grouper) + ':' + filterValues;
      }
    }

    if (this.isAlphanumeric) query += '&alphanumeric';
    query += '&avg=' + this.isAvg;
    query += '&kind=' + encodeURIComponent(this.kind);
    query += '&counting=' + this.bestN;

    return query;
  }

  async loadPreview() {
    const query = this.buildQuery();
    if (!query) {
      this.scoreblocks = [];
      return;
    }
    this.previewLoading = true;
    try {
      const link = `${backendUrl}api/scores/${this.uuid}/query?${query}`;
      const score = await firstValueFrom(this.backendService.getScoreList('api/scores/' + this.uuid + '/query?' + query));
      this.scoreblocks = score.scoreblocks || [];
    } catch {
      this.scoreblocks = [];
    } finally {
      this.previewLoading = false;
      this.cdr.detectChanges();
    }
  }

  async loadSavedScores() {
    this.loading = true;
    try {
      this.savedScores = await firstValueFrom(this.backend.getAdminScores(this.uuid, this.secret));
    } catch {
      this.savedScores = [];
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  startNewScore() {
    this.creatingNew = true;
    this.editingScore = null;
    this.editTitle = '';
    const default0 = this.availableGroupers[0] || null;
    const default1 = this.availableGroupers.includes('Geschlecht') ? 'Geschlecht' : null;
    this.selectedGroupers = [default0, default1, null, null];
    this.filterOptions.clear();
    this.selectedFilters.clear();
    this.filterOnly = [false, false, false, false];
    this.kind = 'Einzelrangliste';
    this.bestN = 'alle';
    this.isAlphanumeric = false;
    this.isAvg = true;
    this.scoreblocks = [];
    if (default0 || default1) {
      this.loadAllFilterOptions();
    }
  }

  async editScore(score: PublishedScoreView) {
    this.creatingNew = false;
    this.editingScore = score;
    this.editTitle = score.title;
    this.parseQuery(score.query);
    await this.loadAllFilterOptions();
    await this.loadPreview();
  }

  private parseQuery(query: string) {
    this.selectedGroupers = [null, null, null, null];
    this.filterOptions.clear();
    this.selectedFilters.clear();
    this.filterOnly = [false, false, false, false];
    this.kind = 'Einzelrangliste';
    this.bestN = 'alle';
    this.isAlphanumeric = false;
    this.isAvg = true;

    const params = new URLSearchParams(query);
    const groupby = params.get('groupby');
    if (groupby) {
      const parts = groupby.split(':');
      parts.forEach((p, i) => {
        if (i < 4) this.selectedGroupers[i] = decodeURIComponent(p);
      });
    }

    const filters = params.getAll('filter');
    filters.forEach(f => {
      const [grouperName, values] = f.split(':');
      const decodedGrouper = decodeURIComponent(grouperName);
      const decodedValues = values.split('!').map(v => decodeURIComponent(v));
      const hasAll = decodedValues.includes('all') || decodedValues.includes('alle');
      const realValues = decodedValues.filter(v => v !== 'all' && v !== 'alle');
      const level = this.selectedGroupers.findIndex(g => g === decodedGrouper);
      if (level >= 0) {
        this.selectedFilters.set('' + level, new Set(realValues));
        if (hasAll) {
          this.filterOnly[level] = true;
        }
      }
    });

    if (params.has('alphanumeric')) this.isAlphanumeric = true;
    if (params.get('avg') === 'false') this.isAvg = false;
    if (params.has('kind')) this.kind = params.get('kind');
    if (params.has('counting')) this.bestN = params.get('counting');
  }

  private async loadAllFilterOptions() {
    for (let i = 0; i < 4; i++) {
      if (this.selectedGroupers[i]) {
        await this.loadFilterOptions(i);
      }
    }
  }

  async saveScore(published: boolean) {
    if (!this.editTitle.trim()) {
      const toast = await this.toastCtrl.create({ message: 'Bitte einen Titel eingeben', duration: 2000, color: 'warning' });
      await toast.present();
      return;
    }
    const query = this.buildQuery();
    if (!query) {
      const toast = await this.toastCtrl.create({ message: 'Bitte mindestens einen Grouper auswählen', duration: 2000, color: 'warning' });
      await toast.present();
      return;
    }
    const request: AdminScoreRequest = { title: this.editTitle.trim(), query, published };
    try {
      if (this.editingScore) {
        await firstValueFrom(this.backend.updateAdminScore(this.uuid, this.editingScore.id, request, this.secret));
      } else {
        await firstValueFrom(this.backend.createAdminScore(this.uuid, request, this.secret));
      }
      const toast = await this.toastCtrl.create({
        message: published ? 'Rangliste veröffentlicht' : 'Rangliste gespeichert',
        duration: 2000,
        color: 'success'
      });
      await toast.present();
      this.creatingNew = false;
      this.editingScore = null;
      await this.loadSavedScores();
    } catch (e: any) {
      const toast = await this.toastCtrl.create({ message: 'Fehler: ' + (e.message || e), duration: 3000, color: 'danger' });
      await toast.present();
    }
  }

  async publishScore(score: PublishedScoreView) {
    const request: AdminScoreRequest = { title: score.title, query: score.query, published: true };
    try {
      await firstValueFrom(this.backend.updateAdminScore(this.uuid, score.id, request, this.secret));
      const toast = await this.toastCtrl.create({ message: 'Veröffentlicht', duration: 2000, color: 'success' });
      await toast.present();
      await this.loadSavedScores();
    } catch (e: any) {
      const toast = await this.toastCtrl.create({ message: 'Fehler: ' + (e.message || e), duration: 3000, color: 'danger' });
      await toast.present();
    }
  }

  async unpublishScore(score: PublishedScoreView) {
    const request: AdminScoreRequest = { title: score.title, query: score.query, published: false };
    try {
      await firstValueFrom(this.backend.updateAdminScore(this.uuid, score.id, request, this.secret));
      const toast = await this.toastCtrl.create({ message: 'Veröffentlichung zurückgenommen', duration: 2000, color: 'success' });
      await toast.present();
      await this.loadSavedScores();
    } catch (e: any) {
      const toast = await this.toastCtrl.create({ message: 'Fehler: ' + (e.message || e), duration: 3000, color: 'danger' });
      await toast.present();
    }
  }

  async deleteScore(score: PublishedScoreView) {
    const alert = await this.alertCtrl.create({
      header: 'Rangliste löschen',
      message: `Soll "${score.title}" gelöscht werden?`,
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Löschen', role: 'destructive',
          handler: async () => {
            try {
              await firstValueFrom(this.backend.deleteAdminScore(this.uuid, score.id, this.secret));
              const toast = await this.toastCtrl.create({ message: 'Gelöscht', duration: 2000, color: 'success' });
              await toast.present();
              if (this.editingScore?.id === score.id) {
                this.editingScore = null;
              }
              await this.loadSavedScores();
            } catch {
              const toast = await this.toastCtrl.create({ message: 'Fehler beim Löschen', duration: 3000, color: 'danger' });
              await toast.present();
            }
          }
        }
      ]
    });
    await alert.present();
  }

  get hasGroupers(): boolean {
    return this.selectedGroupers.some(g => g !== null);
  }

  async openScore(scoreId: string) {
    const token = await firstValueFrom(this.backend.getScoreToken(this.uuid, scoreId, this.secret));
    const url = `${backendUrl}api/scores/${this.uuid}/${scoreId}?html&token=${encodeURIComponent(token)}`;
    window.open(url, '_blank');
  }
}
